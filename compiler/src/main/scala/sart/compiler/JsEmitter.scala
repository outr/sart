package sart.compiler

import scala.quoted.*
import scala.tasty.inspector.*
import java.nio.file.{Files, Path}
import java.nio.charset.StandardCharsets
import scala.collection.mutable

/** Scala 3 → ES5 JavaScript emitter — the web-lite backend, a sibling of
 *  [[DartEmitter]] that shares the same TASTy frontend (tasty-inspector).
 *
 *  It emits self-contained, dependency-free ES5 (var, `function(){}`, string
 *  concatenation, object/array literals) — NO runtime or framework is bundled,
 *  so output stays KB-scale for low-power targets (old smart TVs) and as a
 *  lightweight alternative to Flutter Web. Host globals (`document`,
 *  `localStorage`) and small helpers (`Xhr`) live in the page, never in the
 *  emitted app.
 *
 *  Facades are marked `@native` (as with the Dart backend): their definitions
 *  emit nothing and their call sites emit verbatim JS. Every other user
 *  `object` becomes a JS namespace (`var Foo = {}` + members). A `@main` def is
 *  emitted as the trailing entry call.
 */
class JsEmitter(
  outDir: Path,
  sourceRoot: Option[Path] = None,
  projectName: String = "sart_app"
) extends Inspector:

  private val out   = new StringBuilder
  private val todos = mutable.ListBuffer[String]()

  /** Unhandled trees, mirroring DartEmitter's strict-mode contract. */
  def unsupported: List[String] = todos.toList.distinct

  def inspect(using q: Quotes)(tastys: List[Tasty[q.type]]): Unit =
    import q.reflect.*

    // ── helpers ──────────────────────────────────────────────────────────
    def hasNative(s: Symbol): Boolean =
      s.exists && s.annotations.exists(a => a.tpe.typeSymbol.fullName == "sart.dart.native")

    val mainFqns = Set("scala.main", "scala.annotation.main", "scala.annotation.newMain")
    def isMain(s: Symbol): Boolean =
      s.exists && s.annotations.exists(a => mainFqns.contains(a.tpe.typeSymbol.fullName))

    def unwrap(t: Term): Term = t match
      case Inlined(_, _, e) => unwrap(e)
      case Typed(e, _)      => unwrap(e)
      case _                => t

    def isUnit(t: Term): Boolean = unwrap(t) match
      case Literal(UnitConstant()) => true
      case Block(Nil, e)           => isUnit(e)
      case _                       => false

    def jsStr(s: String): String =
      "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""

    def emitConst(c: Constant): String = c match
      case IntConstant(i)     => i.toString
      case LongConstant(l)    => l.toString
      case DoubleConstant(d)  => d.toString
      case FloatConstant(f)   => f.toString
      case StringConstant(s)  => jsStr(s)
      case BooleanConstant(b) => b.toString
      case CharConstant(ch)   => jsStr(ch.toString)
      case NullConstant()     => "null"
      case UnitConstant()     => "undefined"
      case other              => other.value.toString

    def todo(t: Tree): String =
      val label = t.getClass.getSimpleName
      todos += s"unsupported: $label"
      s"/*?$label*/"

    // User-authored `object`s become JS namespaces; this maps each such
    // module symbol to its namespace name so cross-module refs resolve.
    val moduleNames = mutable.LinkedHashMap[Symbol, String]()
    var currentModule: Symbol = Symbol.noSymbol

    def isBuiltin(s: Symbol): Boolean =
      s.exists && (s.fullName.startsWith("scala.") || s.fullName.startsWith("java."))

    def isUserModule(cd: ClassDef): Boolean =
      val s = cd.symbol
      s.exists && s.flags.is(Flags.Module)
        && !s.flags.is(Flags.Case) && !hasNative(s)
        // the synthetic top-level holder is `<file>$package` (module class
        // name carries a trailing `$`) — never a user namespace.
        && !s.name.stripSuffix("$").endsWith("$package") && !isBuiltin(s)
        // exclude the synthesised companion of a case class
        && !(s.companionClass.exists && s.companionClass.flags.is(Flags.Case))

    def userMember(s: Symbol): Boolean =
      s.exists && !s.flags.is(Flags.Synthetic) && !s.flags.is(Flags.Artifact)

    def isFacade(s: Symbol): Boolean =
      s.exists && (hasNative(s) || hasNative(s.owner))

    def repeatedElems(t: Term): List[Term] = unwrap(t) match
      case Repeated(es, _) => es
      case _               => Nil

    def stripTypeApply(f: Term): Term = f match
      case TypeApply(inner, _) => stripTypeApply(inner)
      case _                   => f

    def flattenVarargs(args: List[Term]): List[Term] =
      args.flatMap(a => repeatedElems(a) match { case Nil => List(a); case es => es })

    // Fields (ctor order) if `resultTpe` is a user case class.
    def caseApply(resultTpe: TypeRepr): Option[List[String]] =
      val ts = resultTpe.typeSymbol
      if ts.exists && ts.flags.is(Flags.Case) && !isBuiltin(ts)
      then Some(ts.caseFields.map(_.name))
      else None

    // List.apply is inherited from SeqFactory, so its owner isn't "List" —
    // detect via the receiver instead.
    def isListApply(fn: Term): Boolean = fn match
      case Select(q, "apply") =>
        q.symbol.name == "List" || (q match { case Ident("List") => true; case _ => false })
      case _ => false

    def termParamNames(pcs: List[ParamClause]): List[String] =
      pcs.collect { case c: TermParamClause => c }.flatMap(_.params).map(_.name)

    def hasTermClause(pcs: List[ParamClause]): Boolean =
      pcs.exists { case _: TermParamClause => true; case _ => false }

    val binOps = Map(
      "==" -> "===", "!=" -> "!==", "+" -> "+", "-" -> "-", "*" -> "*",
      "/" -> "/", "%" -> "%", "<" -> "<", ">" -> ">", "<=" -> "<=", ">=" -> ">=",
      "&&" -> "&&", "||" -> "||"
    )

    // ── expression emission ────────────────────────────────────────────────
    def emitRef(sym: Symbol, name: String): String =
      if name == "Nil" then "[]"
      else if isFacade(sym) then name
      else moduleNames.get(sym.owner) match
        case Some(m) => s"$m.$name"
        case None    => name

    def emitStringInterp(outer: Term): Option[String] = unwrap(outer) match
      case Apply(Select(inner, "s"), List(argsArg)) =>
        unwrap(inner) match
          case Apply(Select(sc, "apply"), List(partsArg))
              if sc.symbol.fullName.contains("StringContext") =>
            val parts  = repeatedElems(partsArg).collect { case Literal(StringConstant(s)) => s }
            val args   = repeatedElems(argsArg)
            val pieces = mutable.ListBuffer[String]()
            for i <- args.indices do
              if i < parts.length && parts(i).nonEmpty then pieces += jsStr(parts(i))
              pieces += emitTerm(args(i))
            if parts.length > args.length && parts.last.nonEmpty then pieces += jsStr(parts.last)
            Some(if pieces.isEmpty then "\"\"" else pieces.mkString(" + "))
          case _ => None
      case _ => None

    def asClosure(t: Term): Option[DefDef] =
      def peel(x: Term): Term = x match
        case Inlined(_, _, e) => peel(e)
        case Typed(e, _)      => peel(e)
        case Block(Nil, e)    => peel(e) // scalac wraps some lambdas in Block(Nil, ...)
        case _                => x
      peel(t) match
        case Block(List(dd @ DefDef(_, _, _, _)), Closure(_, _)) => Some(dd)
        case _                                                   => None

    def emitClosure(dd: DefDef, forceUnit: Boolean = false): String =
      val params  = termParamNames(dd.paramss)
      val wantRet = !forceUnit && !(dd.returnTpt.tpe =:= TypeRepr.of[Unit])
      val body    = dd.rhs.map(b => emitBody(b, wantRet)).getOrElse("")
      s"function(${params.mkString(", ")}) { $body }"

    def emitArgs(args: List[Term]): String =
      flattenVarargs(args).map(a => asClosure(a) match
        case Some(dd) => emitClosure(dd)
        case None     => emitTerm(a)
      ).mkString(", ")

    def emitApply(t: Term): String =
      emitStringInterp(t) match
        case Some(s) => return s
        case None    =>
      t match
        case Apply(fn0, args) =>
          val fn = stripTypeApply(fn0)
          if isListApply(fn) then
            return "[" + flattenVarargs(args).map(emitTerm).mkString(", ") + "]"
          caseApply(t.tpe) match
            case Some(fields) if fn.symbol.name == "apply" || fn.symbol.name == "<init>" =>
              val flat = flattenVarargs(args)
              return "{" + fields.zip(flat).map((f, a) => s"$f: ${emitTerm(a)}").mkString(", ") + "}"
            case _ =>
          fn match
            case Select(recv, op) if binOps.contains(op) && args.length == 1 =>
              s"(${emitTerm(recv)} ${binOps(op)} ${emitTerm(args.head)})"
            case Select(recv, "foreach") =>
              // a foreach body is Unit-context — don't emit a `return`
              val cb = args.headOption.flatMap(asClosure).map(emitClosure(_, forceUnit = true)).getOrElse(emitArgs(args))
              s"${emitTerm(recv)}.forEach($cb)"
            case Select(recv, "map")     => s"${emitTerm(recv)}.map(${emitArgs(args)})"
            case _                       => s"${emitTerm(fn)}(${emitArgs(args)})"
        case _ => emitTerm(t)

    def emitTerm(t0: Term): String =
      unwrap(t0) match
        case Literal(c)            => emitConst(c)
        case This(_)               => moduleNames.getOrElse(currentModule, "this")
        case id @ Ident(n)         => emitRef(id.symbol, n)
        case Select(q, "toString")           => s"String(${emitTerm(q)})"
        case Apply(Select(q, "toString"), Nil) => s"String(${emitTerm(q)})"
        case Apply(sel @ Select(_, _), Nil)  => s"${emitTerm(sel)}()"
        case Apply(id @ Ident(_), Nil)       => s"${emitTerm(id)}()"
        case a: Apply                        => emitApply(a)
        case TypeApply(fn, _)                => emitTerm(fn)
        case sel @ Select(q, name) =>
          moduleNames.get(sel.symbol.owner) match
            case Some(m) => s"$m.$name"
            case None    => s"${emitTerm(q)}.$name"
        case If(c, a, b)  => s"(${emitTerm(c)} ? ${emitTerm(a)} : ${emitTerm(b)})"
        case Block(_, e)  => emitTerm(e)
        case other        => todo(other)

    // ── statements / bodies ─────────────────────────────────────────────────
    def emitStat(s: Statement): String = s match
      case ValDef(name, _, Some(rhs)) => s"var $name = ${emitTerm(rhs)};"
      case Assign(lhs, rhs)           => s"${emitTerm(lhs)} = ${emitTerm(rhs)};"
      case If(c, a, b)                => emitIfStat(c, a, b)
      case t: Term                    => s"${emitTerm(t)};"
      case _                          => ""

    def emitIfStat(c: Term, a: Term, b: Term): String =
      val head = s"if (${emitTerm(c)}) { ${emitBody(a, false)} }"
      if isUnit(b) then head else s"$head else { ${emitBody(b, false)} }"

    def emitTrailing(expr: Term, wantRet: Boolean): String =
      unwrap(expr) match
        case e @ Block(_, _)  => emitBody(e, wantRet)
        case If(c, a, b)      => emitIfStat(c, a, b)
        case Assign(lhs, rhs) => s"${emitTerm(lhs)} = ${emitTerm(rhs)};"
        case e if isUnit(e)   => ""
        case e if wantRet     => s"return ${emitTerm(e)};"
        case e                => s"${emitTerm(e)};"

    def emitBody(t: Term, wantRet: Boolean): String = unwrap(t) match
      case Block(stats, expr) =>
        (stats.map(emitStat).filter(_.nonEmpty) :+ emitTrailing(expr, wantRet))
          .filter(_.nonEmpty).mkString(" ")
      case other => emitTrailing(other, wantRet)

    // ── module (object → JS namespace) ──────────────────────────────────────
    def emitModule(cd: ClassDef): Unit =
      currentModule = cd.symbol
      val mn = moduleNames(cd.symbol)
      out.append(s"var $mn = {};\n")
      cd.body.foreach {
        case vd @ ValDef(name, _, Some(rhs)) if userMember(vd.symbol) =>
          out.append(s"$mn.$name = ${emitTerm(rhs)};\n")
        case dd @ DefDef(name, pcs, _, Some(rhs))
            if userMember(dd.symbol) && name != "<init>" && !name.endsWith("_=")
               && hasTermClause(pcs) =>
          val params  = termParamNames(pcs)
          val wantRet = !(dd.returnTpt.tpe =:= TypeRepr.of[Unit])
          out.append(s"$mn.$name = function(${params.mkString(", ")}) { ${emitBody(rhs, wantRet)} };\n")
        case _ => ()
      }
      currentModule = Symbol.noSymbol

    // ── walk ─────────────────────────────────────────────────────────────
    def eachClassDef(tree: Tree)(f: ClassDef => Unit): Unit = tree match
      case pc: PackageClause => pc.stats.foreach(s => eachClassDef(s)(f))
      case cd: ClassDef      => f(cd); cd.body.foreach(s => eachClassDef(s)(f))
      case _                 => ()

    val mains = mutable.ListBuffer[DefDef]()
    def collectMains(tree: Tree): Unit = tree match
      case pc: PackageClause => pc.stats.foreach(collectMains)
      case cd: ClassDef      => cd.body.foreach(collectMains)
      case dd: DefDef if isMain(dd.symbol) => mains += dd
      case _                 => ()

    // 1. register module namespaces (so cross-module refs resolve)
    for tasty <- tastys do
      eachClassDef(tasty.ast) { cd =>
        if isUserModule(cd) then moduleNames.getOrElseUpdate(cd.symbol, cd.symbol.name.stripSuffix("$"))
      }
    // 2. emit each module
    for tasty <- tastys do
      eachClassDef(tasty.ast) { cd => if isUserModule(cd) then emitModule(cd) }
    // 3. emit the @main entry call(s) last
    for tasty <- tastys do collectMains(tasty.ast)
    for dd <- mains; body <- dd.rhs do
      val js = emitBody(body, false)
      if js.nonEmpty then out.append(js).append('\n')

  // ── output ─────────────────────────────────────────────────────────────
  def writeOutput(): Unit =
    Files.createDirectories(outDir)
    Files.writeString(outDir.resolve("app.js"), out.toString, StandardCharsets.UTF_8)
    Files.writeString(outDir.resolve("index.html"), indexHtml, StandardCharsets.UTF_8)

  /** A minimal host page: a mount point, the tiny callback-XHR host helper
   *  (NOT part of the app bundle), then the emitted app. Everything here is
   *  plain ES5. */
  private def indexHtml: String =
    s"""<!doctype html>
       |<html>
       |<head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>$projectName</title></head>
       |<body>
       |<div id="app"></div>
       |<script>
       |// Host helper (not bundled): callback-shaped XHR for old engines
       |// (webOS 3 / Tizen 2.3: no fetch, unreliable Promises).
       |var Xhr = { get: function(url, onOk, onErr) {
       |  try {
       |    var x = new XMLHttpRequest();
       |    x.open("GET", url, true);
       |    x.onreadystatechange = function() {
       |      if (x.readyState === 4) {
       |        if (x.status >= 200 && x.status < 300) { onOk(x.responseText); } else { onErr(); }
       |      }
       |    };
       |    x.send();
       |  } catch (e) { onErr(); }
       |} };
       |</script>
       |<script src="app.js"></script>
       |</body>
       |</html>
       |""".stripMargin
