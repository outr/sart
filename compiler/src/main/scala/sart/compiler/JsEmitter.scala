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

    // Class-emission context: the class whose instance method we're emitting,
    // and the name used for member access. We capture `this` into a local
    // `self` at each function's top and refer to members through it — ES5
    // `function(){}` does NOT lexically capture `this`, so an event-handler
    // closure built inside render() would otherwise lose the instance.
    var currentClass: Symbol = Symbol.noSymbol
    var selfRef: String = "this"
    def inCurrentClass(owner: Symbol): Boolean =
      currentClass.exists && owner.exists && currentClass.typeRef.baseClasses.contains(owner)

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

    // A user-authored class (not an object/module, case class, trait, facade,
    // or stdlib type) — emitted as an ES5 constructor + prototype.
    def isUserClassSym(s: Symbol): Boolean =
      s.exists && s.isClassDef && !s.flags.is(Flags.Module) && !s.flags.is(Flags.Case)
        && !s.flags.is(Flags.Trait) && !hasNative(s) && !isBuiltin(s)
    def isUserClass(cd: ClassDef): Boolean = isUserClassSym(cd.symbol)

    // The user superclass (if any) plus its constructor args, for the
    // `Base.call(this, …)` + `Object.create(Base.prototype)` wiring.
    def parentInfo(cd: ClassDef): Option[(Symbol, List[Term])] =
      cd.parents.collectFirst {
        case Apply(Select(New(tpt), _), args) if isUserClassSym(tpt.tpe.typeSymbol) => (tpt.tpe.typeSymbol, args)
        case tt: TypeTree if isUserClassSym(tt.tpe.typeSymbol)                      => (tt.tpe.typeSymbol, Nil)
      }

    def isFacade(s: Symbol): Boolean =
      s.exists && (hasNative(s) || hasNative(s.owner))

    def repeatedElems(t: Term): List[Term] = unwrap(t) match
      case Repeated(es, _) => es
      case _               => Nil

    // Option-typed so an EMPTY varargs (`List()`) is distinguishable from a
    // non-varargs argument — otherwise the empty SeqLiteral leaks through.
    def repeatedElemsOpt(t: Term): Option[List[Term]] = unwrap(t) match
      case Repeated(es, _) => Some(es)
      case _               => None

    def stripTypeApply(f: Term): Term = f match
      case TypeApply(inner, _) => stripTypeApply(inner)
      case _                   => f

    def flattenVarargs(args: List[Term]): List[Term] =
      args.flatMap(a => repeatedElemsOpt(a).getOrElse(List(a)))

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

    // ── async markers (shared with the Dart backend) ────────────────────────
    // `sart.dart.await.apply(f)` and `sart.dart.async.apply(body)`, with or
    // without the elided type argument. Detected by the object's FQN so the
    // SAME Scala async code compiles to both backends.
    def sartObj(q: Term, fqn: String): Boolean =
      q.symbol.exists && q.symbol.fullName == fqn

    def isAwait(t: Term): Option[Term] = unwrap(t) match
      case Apply(TypeApply(Select(q, "apply"), _), List(f)) if sartObj(q, "sart.dart.await") => Some(f)
      case Apply(Select(q, "apply"), List(f))               if sartObj(q, "sart.dart.await") => Some(f)
      case _ => None

    def unwrapAsync(t: Term): Option[Term] = unwrap(t) match
      case Apply(TypeApply(Select(q, "apply"), _), List(b)) if sartObj(q, "sart.dart.async") => Some(b)
      case Apply(Select(q, "apply"), List(b))               if sartObj(q, "sart.dart.async") => Some(b)
      case _ => None

    // True if `tree` awaits in its OWN body — stops at def/closure boundaries
    // and at nested `async { … }` blocks (whose awaits belong to that block),
    // mirroring DartEmitter's containsAwait.
    def containsAwait(tree: Tree): Boolean =
      val acc = new TreeAccumulator[Boolean]:
        def foldTree(found: Boolean, t: Tree)(owner: Symbol): Boolean =
          if found then true else t match
            case _: DefDef                              => found
            case tm: Term if unwrapAsync(tm).isDefined  => found
            case tm: Term if isAwait(tm).isDefined      => true
            case _                                      => foldOverTree(found, t)(owner)
      acc.foldTree(false, tree)(Symbol.spliceOwner)

    var deferredCounter = 0
    def freshDeferred(): String =
      val n = s"__d$deferredCounter"; deferredCounter += 1; n

    def todoAwait(s: Tree): String =
      val label = s.getClass.getSimpleName
      todos += s"unsupported: await in $label (only straight-line/tail await supported)"
      s"/*?await-in-$label*/"

    // ── expression emission ────────────────────────────────────────────────
    def emitRef(sym: Symbol, name: String): String =
      if name == "Nil" then "[]"
      else if isFacade(sym) then name
      else if inCurrentClass(sym.owner) then s"$selfRef.$name"
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
      // await/async only lower at statement/tail level (see emitCps); anywhere
      // else (e.g. `f(await(g))`) is unsupported — flag it rather than emit
      // a broken `await.apply(...)` call.
      if isAwait(t).isDefined || unwrapAsync(t).isDefined then return todoAwait(t)
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
            case Select(New(tpt), _) if isUserClassSym(tpt.tpe.typeSymbol) =>
              s"new ${tpt.tpe.typeSymbol.name}(${emitArgs(args)})"
            // A `var` setter (`x.prop_=(v)`) → a plain JS assignment.
            case Select(recv, setter) if setter.endsWith("_=") && args.length == 1 =>
              s"${emitTerm(recv)}.${setter.dropRight(2)} = ${emitTerm(args.head)}"
            // List append (`xs :+ x`) → `xs.concat([x])` (immutable, like Scala).
            case Select(recv, ":+" | "$colon$plus") =>
              s"${emitTerm(recv)}.concat([${emitTerm(args.head)}])"
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
        // `new UserClass(args)` (incl. creator-application `Foo()` and the
        // zero-arg form) — matched before the generic Apply cases below.
        case Apply(Select(New(tpt), _), args) if isUserClassSym(tpt.tpe.typeSymbol) =>
          s"new ${tpt.tpe.typeSymbol.name}(${emitArgs(args)})"
        case Apply(TypeApply(Select(New(tpt), _), _), args) if isUserClassSym(tpt.tpe.typeSymbol) =>
          s"new ${tpt.tpe.typeSymbol.name}(${emitArgs(args)})"
        case Literal(c)            => emitConst(c)
        case This(_)               =>
          if currentClass.exists then selfRef else moduleNames.getOrElse(currentModule, "this")
        case id @ Ident(n)         => emitRef(id.symbol, n)
        case Select(q, "toString")           => s"String(${emitTerm(q)})"
        case Apply(Select(q, "toString"), Nil) => s"String(${emitTerm(q)})"
        // `xs.size` → JS `.length` (Scala collections; a property, not a call).
        case Select(q, "size")               => s"${emitTerm(q)}.length"
        case Apply(sel @ Select(_, _), Nil)  => s"${emitTerm(sel)}()"
        case Apply(id @ Ident(_), Nil)       => s"${emitTerm(id)}()"
        case a: Apply                        => emitApply(a)
        case TypeApply(fn, _)                => emitTerm(fn)
        case sel @ Select(q, name) =>
          if inCurrentClass(sel.symbol.owner) then s"$selfRef.$name"
          else moduleNames.get(sel.symbol.owner) match
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

    // ── async CPS lowering ──────────────────────────────────────────────────
    // Lower an `async`/await body to nested callbacks (no Promise). Each
    // `await(f)` splits the continuation: the rest of the body becomes
    // `f.onComplete(function(bind){ … })`. `resolveVar` is the enclosing
    // method's `Deferred`, resolved at the tail; None = fire-and-forget.
    // Straight-line and tail awaits only — an await inside a loop or a
    // non-tail branch is flagged unsupported (via emitStat's containsAwait
    // guard), never miscompiled.
    def emitCps(body: Term, resolveVar: Option[String]): String =
      unwrap(body) match
        case Block(stats, expr) => emitCpsSeq(stats, expr, resolveVar)
        case other              => emitCpsTail(other, resolveVar)

    def emitCpsSeq(stats: List[Statement], tail: Term, resolveVar: Option[String]): String =
      stats match
        case Nil => emitCpsTail(tail, resolveVar)
        case ValDef(name, _, Some(rhs)) :: rest if isAwait(rhs).isDefined =>
          s"${emitTerm(isAwait(rhs).get)}.onComplete(function($name) { ${emitCpsSeq(rest, tail, resolveVar)} });"
        case (t: Term) :: rest if isAwait(t).isDefined =>
          s"${emitTerm(isAwait(t).get)}.onComplete(function() { ${emitCpsSeq(rest, tail, resolveVar)} });"
        case (s: Statement) :: rest =>
          val stmt = if containsAwait(s) then todoAwait(s) else emitStat(s)
          val more = emitCpsSeq(rest, tail, resolveVar)
          if stmt.isEmpty then more else if more.isEmpty then stmt else s"$stmt $more"

    def emitCpsTail(tail0: Term, resolveVar: Option[String]): String =
      val tail = unwrap(tail0)
      tail match
        case Block(stats, expr) => emitCpsSeq(stats, expr, resolveVar)
        case _ => isAwait(tail) match
          case Some(f) => resolveVar match
            case Some(d) => s"${emitTerm(f)}.onComplete(function(__v) { $d.resolve(__v); });"
            case None    => s"${emitTerm(f)}.onComplete(function() {});"
          case None => resolveVar match
            case Some(d) =>
              val unitTail = isUnit(tail) || tail.tpe =:= TypeRepr.of[Unit]
              if unitTail then
                val st = emitStat(tail)
                if st.isEmpty then s"$d.resolve(undefined);" else s"$st $d.resolve(undefined);"
              else if containsAwait(tail) then todoAwait(tail)
              else s"$d.resolve(${emitTerm(tail)});"
            case None =>
              if containsAwait(tail) then todoAwait(tail) else emitStat(tail)

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
          val asyncBody = unwrapAsync(rhs)
          if asyncBody.isDefined || containsAwait(rhs) then
            // Async method: return a Deferred the CPS chain resolves.
            val d   = freshDeferred()
            val cps = emitCps(asyncBody.getOrElse(rhs), Some(d))
            out.append(s"$mn.$name = function(${params.mkString(", ")}) { var $d = new Deferred(); $cps return $d; };\n")
          else
            val wantRet = !(dd.returnTpt.tpe =:= TypeRepr.of[Unit])
            out.append(s"$mn.$name = function(${params.mkString(", ")}) { ${emitBody(rhs, wantRet)} };\n")
        case _ => ()
      }
      currentModule = Symbol.noSymbol

    // ── class (→ ES5 constructor + prototype) ────────────────────────────────
    def emitClassMethod(cn: String, dd: DefDef): Unit =
      val name    = dd.name
      val params  = termParamNames(dd.paramss)
      val rhs     = dd.rhs.get
      val asyncBody = unwrapAsync(rhs)
      if asyncBody.isDefined || containsAwait(rhs) then
        val d   = freshDeferred()
        val cps = emitCps(asyncBody.getOrElse(rhs), Some(d))
        out.append(s"$cn.prototype.$name = function(${params.mkString(", ")}) { var $selfRef = this; var $d = new Deferred(); $cps return $d; };\n")
      else
        val wantRet = !(dd.returnTpt.tpe =:= TypeRepr.of[Unit])
        out.append(s"$cn.prototype.$name = function(${params.mkString(", ")}) { var $selfRef = this; ${emitBody(rhs, wantRet)} };\n")

    def emitClass(cd: ClassDef): Unit =
      val sym    = cd.symbol
      val cn     = sym.name
      val parent = parentInfo(cd)
      currentClass = sym
      selfRef = "self"

      // constructor: super call, capture `self`, then fields + init in body order
      val ctorParams = termParamNames(cd.constructor.paramss)
      val cb = new StringBuilder
      parent.foreach { case (p, args) =>
        val extra = if args.isEmpty then "" else ", " + emitArgs(args)
        cb.append(s"${p.name}.call(this$extra); ")
      }
      cb.append(s"var $selfRef = this; ")
      cd.body.foreach {
        case vd @ ValDef(name, _, Some(rhs)) if userMember(vd.symbol) =>
          if vd.symbol.flags.is(Flags.ParamAccessor) then cb.append(s"$selfRef.$name = $name; ")
          else cb.append(s"$selfRef.$name = ${emitTerm(rhs)}; ")
        case t: Term =>
          val st = emitStat(t); if st.nonEmpty then cb.append(s"$st ")
        case _ => ()
      }
      out.append(s"function $cn(${ctorParams.mkString(", ")}) { ${cb.toString.trim} }\n")
      parent.foreach { case (p, _) =>
        out.append(s"$cn.prototype = Object.create(${p.name}.prototype);\n")
        out.append(s"$cn.prototype.constructor = $cn;\n")
      }

      // methods (abstract defs — no rhs — are provided by subclasses; skip)
      cd.body.foreach {
        case dd @ DefDef(name, pcs, _, Some(_))
            if userMember(dd.symbol) && name != "<init>" && !name.endsWith("_=") && hasTermClause(pcs) =>
          emitClassMethod(cn, dd)
        case _ => ()
      }
      currentClass = Symbol.noSymbol
      selfRef = "this"

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

    // 1. collect @main defs first — Scala 3 also generates a runnable wrapper
    //    *class* of the same name, which must NOT be emitted as a user class.
    for tasty <- tastys do collectMains(tasty.ast)
    val mainNames = mains.map(_.name).toSet
    // 2. register module namespaces (so cross-module refs resolve)
    for tasty <- tastys do
      eachClassDef(tasty.ast) { cd =>
        if isUserModule(cd) then moduleNames.getOrElseUpdate(cd.symbol, cd.symbol.name.stripSuffix("$"))
      }
    // 3. emit each user class (constructor+prototype) and module (namespace).
    //    Constructor `function` declarations hoist and `Object.create` reads a
    //    live prototype, so inheritance is order-independent; @main runs last.
    for tasty <- tastys do
      eachClassDef(tasty.ast) { cd =>
        if isUserClass(cd) && !mainNames.contains(cd.symbol.name) then emitClass(cd)
        else if isUserModule(cd) then emitModule(cd)
      }
    // 4. emit the @main entry call(s) last
    for dd <- mains; body <- dd.rhs do
      val asyncBody = unwrapAsync(body)
      // A `@main` needs no return value, so an async entry is fire-and-forget
      // (no Deferred): just run the CPS chain.
      val js =
        if asyncBody.isDefined || containsAwait(body) then emitCps(asyncBody.getOrElse(body), None)
        else emitBody(body, false)
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
       |// Host helpers (NOT bundled in app.js). Deferred is the tiny async
       |// primitive the CPS lowering targets — no Promise, works on old engines
       |// (webOS 3 / Tizen 2.3). Xhr returns a Deferred so `await(Xhr.get(u))`
       |// composes.
       |function Deferred() { this.cbs = []; this.done = false; this.val = undefined; }
       |Deferred.prototype.onComplete = function(f) { if (this.done) { f(this.val); } else { this.cbs.push(f); } };
       |Deferred.prototype.resolve = function(v) { this.done = true; this.val = v; for (var i = 0; i < this.cbs.length; i++) { this.cbs[i](this.val); } this.cbs = []; };
       |var Xhr = { get: function(url) {
       |  var d = new Deferred();
       |  try {
       |    var x = new XMLHttpRequest();
       |    x.open("GET", url, true);
       |    x.onreadystatechange = function() { if (x.readyState === 4) { d.resolve(x.responseText); } };
       |    x.send();
       |  } catch (e) { d.resolve(""); }
       |  return d;
       |} };
       |var Random = { nextInt: function(bound) { return Math.floor(Math.random() * bound); } };
       |var Timer = { periodic: function(ms, cb) { var t = { id: setInterval(cb, ms) }; t.cancel = function() { clearInterval(t.id); }; return t; } };
       |</script>
       |<script src="app.js"></script>
       |</body>
       |</html>
       |""".stripMargin
