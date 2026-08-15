package sart.facadegen

import scala.collection.mutable

/** A tiny JSON parser sufficient for the Dart helper's output shape.
 *
 *  Deliberately hand-rolled so `sart-facadegen` keeps zero third-party
 *  dependencies. Not spec-complete — handles the subset the helper emits:
 *  objects, arrays, strings, numbers, booleans, and null.
 */
object JsonParser:

  sealed trait JValue
  case class JObject(members: Map[String, JValue]) extends JValue
  case class JArray(items: List[JValue]) extends JValue
  case class JString(value: String) extends JValue
  case class JNumber(value: Double) extends JValue
  case class JBool(value: Boolean)  extends JValue
  case object JNull extends JValue

  def parse(input: String): ApiDump =
    val p = new Parser(input)
    p.skipWs()
    toDump(p.readValue())

  // ── Tree → ApiDump ───────────────────────────────────────────────────

  private def toDump(v: JValue): ApiDump = v match
    case o: JObject =>
      val ancestors = o.members.get("typeAncestors") match
        case Some(JObject(m)) =>
          m.map { case (k, v) => k -> strList(v) }
        case _ => Map.empty[String, List[String]]
      ApiDump(
        files         = arr(o, "files").map(toFile),
        typeAncestors = ancestors
      )
    case _ => sys.error(s"expected object at root, got $v")

  private def toFile(v: JValue): FileInfo = v match
    case o: JObject =>
      FileInfo(
        path    = str(o, "path"),
        classes = arr(o, "classes").map(toClass),
        enums   = arr(o, "enums").map(toEnum)
      )
    case _ => sys.error(s"expected file object, got $v")

  private def toEnum(v: JValue): EnumInfo = v match
    case o: JObject => EnumInfo(str(o, "name"), strList(o.members.getOrElse("constants", JArray(Nil))))
    case _ => sys.error(s"expected enum object, got $v")

  private def toClass(v: JValue): ClassInfo = v match
    case o: JObject =>
      ClassInfo(
        name            = str(o, "name"),
        isAbstract      = bool(o, "abstract"),
        typeParams      = strList(o.members.getOrElse("typeParams", JArray(Nil))),
        ancestors       = strList(o.members.getOrElse("ancestors", JArray(Nil))),
        ctors           = arr(o, "constructors").map(toCtor),
        staticFields    = arr(o, "staticFields").map(toStaticField),
        staticMethods   = arr(o, "staticMethods").map(toMethod),
        instanceGetters = arr(o, "instanceGetters").map(toGetter),
        instanceMethods = arr(o, "instanceMethods").map(toMethod),
        inheritedGetters = arr(o, "inheritedGetters").map(toGetter),
        inheritedMethods = arr(o, "inheritedMethods").map(toMethod)
      )
    case _ => sys.error(s"expected class object, got $v")

  private def toCtor(v: JValue): Ctor = v match
    case o: JObject =>
      Ctor(
        name      = str(o, "name"),
        isConst   = bool(o, "const"),
        isFactory = bool(o, "factory"),
        params    = arr(o, "params").map(toParam)
      )
    case _ => sys.error(s"expected ctor object, got $v")

  private def toStaticField(v: JValue): StaticField = v match
    case o: JObject => StaticField(str(o, "name"), str(o, "type"))
    case _ => sys.error(s"expected static-field object, got $v")

  private def toGetter(v: JValue): Getter = v match
    case o: JObject => Getter(str(o, "name"), str(o, "type"), bool(o, "overrides"), str(o, "from"))
    case _ => sys.error(s"expected getter object, got $v")

  private def toMethod(v: JValue): Method = v match
    case o: JObject =>
      Method(
        name       = str(o, "name"),
        returnType = str(o, "returnType"),
        typeParams = strList(o.members.getOrElse("typeParams", JArray(Nil))),
        overrides  = bool(o, "overrides"),
        params     = arr(o, "params").map(toParam),
        from       = str(o, "from")
      )
    case _ => sys.error(s"expected method object, got $v")

  private def toParam(v: JValue): Param = v match
    case o: JObject =>
      Param(
        name       = str(o, "name"),
        tpe        = str(o, "type"),
        named      = bool(o, "named"),
        required   = bool(o, "required"),
        hasDefault = bool(o, "hasDefault")
      )
    case _ => sys.error(s"expected param object, got $v")

  private def strList(v: JValue): List[String] = v match
    case JArray(items) => items.collect { case JString(s) => s }
    case _             => Nil

  private def str(o: JObject, key: String): String = o.members.get(key) match
    case Some(JString(s)) => s
    case _ => ""

  private def bool(o: JObject, key: String): Boolean = o.members.get(key) match
    case Some(JBool(b)) => b
    case _ => false

  private def arr(o: JObject, key: String): List[JValue] = o.members.get(key) match
    case Some(JArray(items)) => items
    case _ => Nil

  // ── Parser ───────────────────────────────────────────────────────────

  private class Parser(input: String):
    private var pos: Int = 0

    def skipWs(): Unit =
      while pos < input.length && input.charAt(pos).isWhitespace do pos += 1

    def readValue(): JValue =
      skipWs()
      if pos >= input.length then sys.error("unexpected EOF")
      input.charAt(pos) match
        case '{' => readObject()
        case '[' => readArray()
        case '"' => readString()
        case 't' | 'f' => readBool()
        case 'n' => readNull()
        case c if c == '-' || c.isDigit => readNumber()
        case c => sys.error(s"unexpected '$c' at $pos")

    def readObject(): JObject =
      expect('{')
      skipWs()
      val m = mutable.LinkedHashMap[String, JValue]()
      if peek != '}' then
        while true do
          skipWs()
          val key = readString().value
          skipWs()
          expect(':')
          val value = readValue()
          m(key) = value
          skipWs()
          if peek == ',' then pos += 1
          else { expect('}'); return JObject(m.toMap) }
      expect('}')
      JObject(m.toMap)

    def readArray(): JArray =
      expect('[')
      skipWs()
      val items = mutable.ListBuffer[JValue]()
      if peek != ']' then
        while true do
          items += readValue()
          skipWs()
          if peek == ',' then pos += 1
          else { expect(']'); return JArray(items.toList) }
      expect(']')
      JArray(items.toList)

    def readString(): JString =
      expect('"')
      val sb = new StringBuilder
      while pos < input.length && input.charAt(pos) != '"' do
        val c = input.charAt(pos)
        if c == '\\' then
          pos += 1
          val e = input.charAt(pos)
          e match
            case '"'  => sb.append('"')
            case '\\' => sb.append('\\')
            case '/'  => sb.append('/')
            case 'n'  => sb.append('\n')
            case 'r'  => sb.append('\r')
            case 't'  => sb.append('\t')
            case 'b'  => sb.append('\b')
            case 'f'  => sb.append('\f')
            case 'u'  =>
              val hex = input.substring(pos + 1, pos + 5)
              sb.append(Integer.parseInt(hex, 16).toChar)
              pos += 4
            case other => sys.error(s"bad escape \\$other at $pos")
          pos += 1
        else
          sb.append(c)
          pos += 1
      expect('"')
      JString(sb.toString)

    def readNumber(): JNumber =
      val start = pos
      if peek == '-' then pos += 1
      while pos < input.length && (input.charAt(pos).isDigit || "eE+-.".contains(input.charAt(pos))) do
        pos += 1
      JNumber(input.substring(start, pos).toDouble)

    def readBool(): JBool =
      if input.startsWith("true", pos)  then { pos += 4; JBool(true) }
      else if input.startsWith("false", pos) then { pos += 5; JBool(false) }
      else sys.error(s"expected bool at $pos")

    def readNull(): JValue =
      if input.startsWith("null", pos) then { pos += 4; JNull }
      else sys.error(s"expected null at $pos")

    private def peek: Char =
      skipWs()
      if pos >= input.length then ' ' else input.charAt(pos)

    private def expect(c: Char): Unit =
      skipWs()
      if pos >= input.length || input.charAt(pos) != c then
        sys.error(s"expected '$c' at $pos, got '${if pos < input.length then input.charAt(pos) else "<EOF>"}'")
      pos += 1
