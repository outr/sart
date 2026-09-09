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

    def isSuper(t: Term): Boolean = t match
      case Super(_, _) => true
      case _           => false

    def jsStr(s: String): String =
      "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""

    // JS reserved words a Scala identifier might collide with when used as a
    // binding (param/local/ref). Member access (`x.default`) is fine in ES5.1,
    // so only binding names need this; a reserved name gets a trailing `_`.
    val jsReserved = Set(
      "default", "new", "var", "function", "this", "class", "return", "typeof",
      "in", "instanceof", "delete", "void", "with", "switch", "case", "for",
      "while", "do", "if", "else", "try", "catch", "finally", "throw", "break",
      "continue", "const", "let", "enum", "export", "import", "super", "extends",
      "null", "true", "false", "arguments", "eval", "yield", "await", "debugger"
    )
    def jsSafe(name: String): String = if jsReserved(name) then name + "_" else name

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

    // Unhandled tree → a VALID JS placeholder (`null`), so a dormant/
    // unsupported construct (in a native-package demo that never mounts)
    // can't break `node --check` of the whole bundle. Tracked for the report.
    def todo(t: Tree): String =
      val label = t.getClass.getSimpleName
      todos += s"unsupported: $label"
      "null"

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

    // Scala 3 enums emit as class+companion with `$new`/`values` — unsupported
    // (and they collide on the JS name); skip them (dormant in the demos).
    def isEnumLike(s: Symbol): Boolean =
      s.exists && (s.flags.is(Flags.Enum)
        || (s.companionClass.exists && s.companionClass.flags.is(Flags.Enum))
        || (s.companionModule.exists && s.companionModule.flags.is(Flags.Enum)))

    def isUserModule(cd: ClassDef): Boolean =
      val s = cd.symbol
      s.exists && s.flags.is(Flags.Module)
        && !s.flags.is(Flags.Case) && !hasNative(s) && !isEnumLike(s)
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
        && !s.flags.is(Flags.Trait) && !s.flags.is(Flags.Enum) && !hasNative(s) && !isBuiltin(s)
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

    // True only for actual case-class CONSTRUCTION (`Foo(...)` / `new Foo`),
    // not any `.apply` that happens to return a case type — e.g. `xs(i)` on a
    // `List[Demo]` returns a Demo but is index access, not construction.
    def isCaseCtor(fn: Term, resultTpe: TypeRepr): Boolean = fn match
      case Select(New(_), _) => true
      case Select(q, "apply") =>
        val comp = resultTpe.typeSymbol.companionModule
        comp.exists && q.symbol.exists && q.symbol == comp
      case _ => false

    // List.apply is inherited from SeqFactory, so its owner isn't "List" —
    // detect via the receiver instead.
    def isListApply(fn: Term): Boolean = fn match
      case Select(q, "apply") =>
        q.symbol.name == "List" || (q match { case Ident("List") => true; case _ => false })
      case _ => false

    def termParamNames(pcs: List[ParamClause]): List[String] =
      pcs.collect { case c: TermParamClause => c }.flatMap(_.params).map(p => jsSafe(p.name))

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
      else if name == "None" && sym.exists && sym.fullName.endsWith(".None") then "null"
      else if isFacade(sym) then name
      else if inCurrentClass(sym.owner) then s"$selfRef.$name"
      else moduleNames.get(sym.owner) match
        case Some(m) => s"$m.$name"
        case None    => jsSafe(name)

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

    // Positional arg emission (user-class ctors, plain calls). Named args are
    // unwrapped to their value in Scala's declaration order (a user JS ctor is
    // positional); default-arg refs are dropped.
    def emitArgs(args: List[Term]): String =
      flattenVarargs(args).filterNot(isDefaultArg).map {
        case NamedArg(_, v) => emitArgVal(v)
        case a              => emitArgVal(a)
      }.mkString(", ")

    // A reference to a synthesised default-argument getter (`Foo$default$3`)
    // — Scala fills omitted defaulted params with these at the call site; a
    // facade's real (JS-runtime) default takes over, so we drop them.
    def isDefaultArg(t: Term): Boolean = unwrap(t) match
      case Select(_, n) if n.contains("$default$") => true
      case Ident(n) if n.contains("$default$")     => true
      case Apply(f, _)                             => isDefaultArg(f)
      case TypeApply(f, _)                         => isDefaultArg(f)
      case NamedArg(_, v)                          => isDefaultArg(v)
      case _                                       => false

    def emitArgVal(t: Term): String = asClosure(t) match
      case Some(dd) => emitClosure(dd)
      case None     => emitTerm(t)

    // Facade / widget calling convention: positional args stay positional; the
    // named args collapse into ONE trailing options object — `Scaffold(appBar =
    // a, body = b)` → `Scaffold({appBar: a, body: b})`, matching the widget
    // runtime. Omitted defaults are dropped.
    def emitFacadeArgs(args: List[Term]): String =
      val kept = args.filterNot(isDefaultArg)
      val pos   = mutable.ListBuffer[String]()
      val named = mutable.ListBuffer[String]()
      kept.foreach {
        case NamedArg(n, v) => named += s"$n: ${emitArgVal(v)}"
        case other          => pos += emitArgVal(other)
      }
      if named.isEmpty then pos.mkString(", ")
      else if pos.isEmpty then "{" + named.mkString(", ") + "}"
      else pos.mkString(", ") + ", {" + named.mkString(", ") + "}"

    // Facade WIDGET construction → an options object keyed by PARAMETER NAME.
    // Scala passes in-declaration-order named args positionally (no NamedArg),
    // so relying on NamedArg alone would emit `Foo(a, b)` and the runtime
    // widget (which reads `o.onKey`/`o.child`) would get the wrong shape. Map
    // each arg to its ctor param name so `RemoteControl(onKey=f, child=c)`
    // always becomes `RemoteControl({onKey: f, child: c})`.
    def emitFacadeCtorArgs(ctor: Symbol, args: List[Term]): String =
      val params = if ctor.exists then ctor.paramSymss.flatten.filter(_.isTerm) else Nil
      val parts = mutable.ListBuffer[String]()
      var i = 0
      args.foreach { a =>
        if !isDefaultArg(a) then
          a match
            case NamedArg(n, v) => parts += s"$n: ${emitArgVal(v)}"
            case other =>
              params.lift(i) match
                case Some(p) => parts += s"${p.name}: ${emitArgVal(other)}"
                case None    => parts += emitArgVal(other)
        i += 1
      }
      "{" + parts.mkString(", ") + "}"

    def isFacadeCall(fn: Term): Boolean =
      val s = fn.symbol
      s.exists && (hasNative(s) || hasNative(s.owner))

    // A Scala collection value (List/Seq/…) — its `apply(i)` is index access.
    def isCollType(t: Term): Boolean =
      val s = t.tpe.dealias.typeSymbol
      s.exists && s.fullName.startsWith("scala.collection.")

    // A Scala/sart Option value → lowered to a plain nullable, so its ops
    // (fold/map/foreach/getOrElse/isDefined/…) become null-checks in JS.
    def isOptionType(t: Term): Boolean =
      val s = t.tpe.widen.dealias.typeSymbol
      s.exists && (s.fullName == "scala.Option" || s.fullName == "sart.stdlib.Option")

    // Apply an argument (usually a closure `x => …`) to an already-emitted JS
    // receiver expression: `(function(x){…})(recvJs)`. `unitCtx` suppresses a
    // trailing `return` for Unit-bodied callbacks (e.g. Option.foreach).
    def applyClosureTo(arg: Term, recvJs: String, unitCtx: Boolean): String =
      asClosure(arg) match
        case Some(dd) => s"(${emitClosure(dd, forceUnit = unitCtx)})($recvJs)"
        case None     => s"(${emitTerm(arg)})($recvJs)"

    // `opt.fold(ifEmpty)(f)` (two arg clauses, a leading type-arg on `fold`):
    // returns (receiver, ifEmpty, f) when `receiver` is an Option.
    def optionFold(x: Term): Option[(Term, Term, Term)] =
      def peel(y: Term): Term = y match { case TypeApply(z, _) => peel(z); case _ => y }
      x match
        case Apply(inner, List(f)) =>
          peel(inner) match
            case Apply(sel, List(ifEmpty)) =>
              peel(sel) match
                case Select(recv, "fold") if isOptionType(recv) => Some((recv, ifEmpty, f))
                case _ => None
            case _ => None
        case _ => None

    // `Some(x)` → `x` (nullable), `None` → handled in emitRef. Mirrors the
    // Dart backend: Option is a plain nullable at the value level.
    def someArg(t: Term): Option[Term] = t match
      case Apply(fn, List(x)) =>
        stripTypeApply(fn) match
          case Select(q, "apply") if q.symbol.exists && q.symbol.fullName.endsWith(".Some") => Some(x)
          case _ => None
      case _ => None

    def emitApply(t: Term): String =
      // await/async only lower at statement/tail level (see emitCps); anywhere
      // else (e.g. `f(await(g))`) is unsupported — flag it rather than emit
      // a broken `await.apply(...)` call.
      if isAwait(t).isDefined || unwrapAsync(t).isDefined then return todoAwait(t)
      // `Some(x)` is a transparent nullable — emit just the value.
      someArg(t) match
        case Some(x) => return emitArgVal(x)
        case None    =>
      emitStringInterp(t) match
        case Some(s) => return s
        case None    =>
      // `opt.fold(ifEmpty)(f)` → single-eval IIFE null-check.
      optionFold(t) match
        case Some((recv, ifEmpty, f)) =>
          return s"(function(_o) { return _o == null ? ${emitArgVal(ifEmpty)} : ${applyClosureTo(f, "_o", false)}; })(${emitTerm(recv)})"
        case None =>
      t match
        case Apply(fn0, args) =>
          val fn = stripTypeApply(fn0)
          if isListApply(fn) then
            return "[" + flattenVarargs(args).map(emitTerm).mkString(", ") + "]"
          caseApply(t.tpe) match
            case Some(fields) if isCaseCtor(fn, t.tpe) =>
              val flat = flattenVarargs(args).filterNot(isDefaultArg)
              // A `_tag` (the class name) lets `match` dispatch on the case
              // type at runtime (object literals otherwise carry no type).
              val tag = "_tag: " + jsStr(t.tpe.typeSymbol.name)
              val fs = fields.zip(flat).map((f, a) => s"$f: ${emitArgVal(a)}")
              return "{" + (tag :: fs).mkString(", ") + "}"
            case _ =>
          fn match
            // Widget / facade construction → runtime call with an options object.
            case Select(New(tpt), _) if hasNative(tpt.tpe.typeSymbol) =>
              s"${tpt.tpe.typeSymbol.name}(${emitFacadeCtorArgs(tpt.tpe.typeSymbol.primaryConstructor, args)})"
            case Select(New(tpt), _) if isUserClassSym(tpt.tpe.typeSymbol) =>
              s"new ${tpt.tpe.typeSymbol.name}(${emitArgs(args)})"
            // Any other construction (a builtin/unknown type in a dormant
            // native-package demo) → valid `new Name(...)` JS.
            case Select(New(tpt), _) =>
              s"new ${tpt.tpe.typeSymbol.name}(${emitArgs(args)})"
            // A `var` setter (`x.prop_=(v)`) → a plain JS assignment.
            case Select(recv, setter) if setter.endsWith("_=") && args.length == 1 =>
              s"${emitTerm(recv)}.${setter.dropRight(2)} = ${emitTerm(args.head)}"
            // Collection index access `xs(i)` → `xs[i]`.
            case Select(recv, "apply") if args.length == 1 && isCollType(recv) =>
              s"${emitTerm(recv)}[${emitTerm(args.head)}]"
            // List append (`xs :+ x`) → `xs.concat([x])` (immutable, like Scala).
            case Select(recv, ":+" | "$colon$plus") =>
              s"${emitTerm(recv)}.concat([${emitTerm(args.head)}])"
            case Select(recv, op) if binOps.contains(op) && args.length == 1 =>
              s"(${emitTerm(recv)} ${binOps(op)} ${emitTerm(args.head)})"
            // Option ops → null-checks (single-eval IIFE). Must precede the
            // collection foreach/map below, since Option lowers to a nullable.
            case Select(recv, "foreach") if isOptionType(recv) =>
              s"(function(_o) { if (_o != null) { ${applyClosureTo(args.head, "_o", true)}; } })(${emitTerm(recv)})"
            case Select(recv, "map") if isOptionType(recv) =>
              s"(function(_o) { return _o == null ? null : ${applyClosureTo(args.head, "_o", false)}; })(${emitTerm(recv)})"
            case Select(recv, "getOrElse") if isOptionType(recv) =>
              s"(function(_o) { return _o == null ? ${emitArgVal(args.head)} : _o; })(${emitTerm(recv)})"
            case Select(recv, "foreach") =>
              // a foreach body is Unit-context — don't emit a `return`
              val cb = args.headOption.flatMap(asClosure).map(emitClosure(_, forceUnit = true)).getOrElse(emitArgs(args))
              s"${emitTerm(recv)}.forEach($cb)"
            case Select(recv, "map")     => s"${emitTerm(recv)}.map(${emitArgs(args)})"
            // A callable facade OBJECT (`object runApp`) is invoked as
            // `runApp.apply(x)` in Scala — drop the `.apply`, call it directly.
            case Select(recv, "apply") if isFacadeCall(fn) =>
              s"${emitTerm(recv)}(${emitFacadeArgs(args)})"
            // A facade member call (widget factory, Theme.of, Navigator.of, …)
            // uses the options-object convention; user calls stay positional.
            case _ if isFacadeCall(fn) => s"${emitTerm(fn)}(${emitFacadeArgs(args)})"
            case _                     => s"${emitTerm(fn)}(${emitArgs(args)})"
        case _ => emitTerm(t)

    // The case class a pattern matches, for its `_tag` + field names.
    def patternClass(p: Tree): Symbol = p match
      case Unapply(fun, _, _) =>
        val s = fun.symbol
        if s.exists && s.owner.exists && s.owner.companionClass.exists then s.owner.companionClass
        else p.asInstanceOf[Term].tpe.typeSymbol
      case TypedOrTest(_, tpt) => tpt.tpe.typeSymbol
      case Typed(_, tpt)       => tpt.tpe.typeSymbol
      case _                   => Symbol.noSymbol

    // (testExpr, bindStatements) for matching `subj` against pattern `p`.
    def patternTest(p: Tree, subj: String): (String, String) = p match
      case Wildcard()      => ("true", "")
      case Ident("_")      => ("true", "")
      case Bind(name, inner) =>
        val (t, b) = patternTest(inner, subj)
        (t, s"var ${jsSafe(name)} = $subj; " + b)
      case Literal(c)      => (s"($subj === ${emitConst(c)})", "")
      case Typed(inner, _) => patternTest(inner, subj) // typed pattern (e: Exception) — bind, accept
      case TypedOrTest(inner, tpt) =>
        val ts = tpt.tpe.typeSymbol
        val tag = if ts.exists && ts.flags.is(Flags.Case) then s"($subj != null && $subj._tag === ${jsStr(ts.name)})" else "true"
        val (it, ib) = patternTest(inner, subj)
        ((if it == "true" then tag else if tag == "true" then it else s"$tag && $it"), ib)
      case u @ Unapply(_, _, subs) =>
        val cls = patternClass(u)
        val fields = if cls.exists then cls.caseFields.map(_.name) else Nil
        val tag = if cls.exists then s"($subj != null && $subj._tag === ${jsStr(cls.name)})" else "true"
        val subT = subs.zipWithIndex.map { case (sp, i) => patternTest(sp, s"$subj.${fields.lift(i).getOrElse("_" + i)}") }
        val test = (tag :: subT.map(_._1)).filter(_ != "true").mkString(" && ")
        ((if test.isEmpty then "true" else test), subT.map(_._2).mkString(""))
      case _ => ("true", "")

    // `expr match { case … }` → an IIFE of an if/else chain (value position).
    def emitMatch(scrut: Term, cases: List[CaseDef]): String =
      val m = "_m"
      val branches = cases.map { cd =>
        val (test, binds) = patternTest(cd.pattern, m)
        val guard = cd.guard.map(g => " && (" + emitTerm(g) + ")").getOrElse("")
        s"if ($test$guard) { $binds${emitBody(cd.rhs, true)} }"
      }
      s"(function($m) { ${branches.mkString(" else ")} })(${emitTerm(scrut)})"

    // `try body catch { cases } finally fin` → an IIFE with JS try/catch.
    def emitTryTerm(body: Term, cases: List[CaseDef], fin: Option[Term]): String =
      val bodyJs = emitBody(body, true)
      val catchJs =
        if cases.isEmpty then "throw _e;"
        else cases.map { cd =>
          val (t, b) = patternTest(cd.pattern, "_e")
          s"if ($t) { $b${emitBody(cd.rhs, true)} }"
        }.mkString(" else ") + " else { throw _e; }"
      val finJs = fin.map(f => s" finally { ${emitBody(f, false)} }").getOrElse("")
      s"(function() { try { $bodyJs } catch (_e) { $catchJs }$finJs })()"

    def emitTerm(t0: Term): String =
      unwrap(t0) match
        // Widget / facade construction → runtime call with an options object.
        case Apply(Select(New(tpt), _), args) if hasNative(tpt.tpe.typeSymbol) =>
          s"${tpt.tpe.typeSymbol.name}(${emitFacadeCtorArgs(tpt.tpe.typeSymbol.primaryConstructor, args)})"
        case Apply(TypeApply(Select(New(tpt), _), _), args) if hasNative(tpt.tpe.typeSymbol) =>
          s"${tpt.tpe.typeSymbol.name}(${emitFacadeCtorArgs(tpt.tpe.typeSymbol.primaryConstructor, args)})"
        // `new UserClass(args)` (incl. creator-application `Foo()` and the
        // zero-arg form) — matched before the generic Apply cases below.
        case Apply(Select(New(tpt), _), args) if isUserClassSym(tpt.tpe.typeSymbol) =>
          s"new ${tpt.tpe.typeSymbol.name}(${emitArgs(args)})"
        case Apply(TypeApply(Select(New(tpt), _), _), args) if isUserClassSym(tpt.tpe.typeSymbol) =>
          s"new ${tpt.tpe.typeSymbol.name}(${emitArgs(args)})"
        // `super.m(args)` — call the parent prototype with `this`; a super call
        // into a `@native` base (e.g. State.initState) is a no-op. Matched
        // BEFORE the generic `Apply(Select,Nil)` case, which would otherwise
        // emit `undefined()` (a crash).
        case Apply(sel @ Select(spr, name), args) if isSuper(spr) =>
          if isUserClassSym(sel.symbol.owner) then
            val a = flattenVarargs(args).filterNot(isDefaultArg).map(emitArgVal)
            s"${sel.symbol.owner.name}.prototype.$name.call(${(selfRef :: a).mkString(", ")})"
          else "undefined"
        case Apply(TypeApply(sel @ Select(spr, name), _), args) if isSuper(spr) =>
          if isUserClassSym(sel.symbol.owner) then
            val a = flattenVarargs(args).filterNot(isDefaultArg).map(emitArgVal)
            s"${sel.symbol.owner.name}.prototype.$name.call(${(selfRef :: a).mkString(", ")})"
          else "undefined"
        case sel @ Select(spr, name) if isSuper(spr) =>
          if isUserClassSym(sel.symbol.owner) then s"${sel.symbol.owner.name}.prototype.$name"
          else "undefined"
        case Literal(c)            => emitConst(c)
        case This(_)               =>
          if currentClass.exists then selfRef else moduleNames.getOrElse(currentModule, "this")
        case id @ Ident(n)         => emitRef(id.symbol, n)
        case Select(q, "toString")           => s"String(${emitTerm(q)})"
        case Apply(Select(q, "toString"), Nil) => s"String(${emitTerm(q)})"
        // `xs.size` → JS `.length` (Scala collections; a property, not a call).
        case Select(q, "size")               => s"${emitTerm(q)}.length"
        // Collection last/head as Option → last-or-null / head-or-null.
        case Select(q, "lastOption") if isCollType(q) =>
          s"(function(_a) { return _a.length > 0 ? _a[_a.length - 1] : null; })(${emitTerm(q)})"
        case Select(q, "headOption") if isCollType(q) =>
          s"(function(_a) { return _a.length > 0 ? _a[0] : null; })(${emitTerm(q)})"
        // Option getters → null-checks (Option is a plain nullable in JS).
        case Select(q, "isDefined" | "nonEmpty") if isOptionType(q) => s"(${emitTerm(q)} != null)"
        case Select(q, "isEmpty") if isOptionType(q)                => s"(${emitTerm(q)} == null)"
        case Select(q, "get") if isOptionType(q)                    => emitTerm(q)
        // Unary operators (`!x`, `-x`, `~x`) — Scala spells them `x.unary_!`.
        case Select(q, "unary_$bang" | "unary_!") => s"(!${emitTerm(q)})"
        case Select(q, "unary_$minus" | "unary_-") => s"(-${emitTerm(q)})"
        case Select(q, "unary_$tilde" | "unary_~") => s"(~${emitTerm(q)})"
        // Numeric widenings are identity in JS (all numbers are doubles).
        case Select(q, "toDouble" | "toInt" | "toLong" | "toFloat") => emitTerm(q)
        case Apply(sel @ Select(_, _), Nil)  => s"${emitTerm(sel)}()"
        case Apply(id @ Ident(_), Nil)       => s"${emitTerm(id)}()"
        case a: Apply                        => emitApply(a)
        case TypeApply(fn, _)                => emitTerm(fn)
        case sel @ Select(q, name) =>
          // A paren-less USER method (getter like `latestRoll`) is emitted as a
          // zero-arg prototype function, so a paren-less reference must invoke
          // it (`self.latestRoll()`); a plain field/val stays bare. A method
          // with an (even empty) TERM clause like `load()` is NOT paren-less —
          // its call site already applies it, so adding `()` here double-calls.
          val s = sel.symbol
          val parenlessCall =
            s.exists && s.isDefDef && s.flags.is(Flags.Method) && !s.flags.is(Flags.FieldAccessor)
              && s.paramSymss.forall(c => c.nonEmpty && c.forall(_.isType)) && isUserClassSym(s.owner)
          val call = if parenlessCall then "()" else ""
          if inCurrentClass(sel.symbol.owner) then s"$selfRef.$name$call"
          else moduleNames.get(sel.symbol.owner) match
            case Some(m) => s"$m.$name$call"
            case None    => s"${emitTerm(q)}.$name$call"
        case If(c, a, b)  => s"(${emitTerm(c)} ? ${emitTerm(a)} : ${emitTerm(b)})"
        case Match(scrut, cases) => emitMatch(scrut, cases)
        case Try(body, cases, fin) => emitTryTerm(body, cases, fin)
        // A closure/lambda in value position → a JS function expression.
        case Block(List(dd: DefDef), _: Closure) => emitClosure(dd)
        // Expression-position arg-hoist block (a nested widget's args) — inline.
        case Block(stats, e) if allValDefs(stats) =>
          emitTempValBlock(stats.collect { case v: ValDef => v }, e)
        case Block(_, e)  => emitTerm(e)
        case other        => todo(other)

    // ── statements / bodies ─────────────────────────────────────────────────
    def emitStat(s: Statement): String = s match
      case ValDef(name, _, Some(rhs)) => s"var ${jsSafe(name)} = ${emitTerm(rhs)};"
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
        // A value-returning `if` in return position: each branch returns.
        case If(c, a, b) if wantRet && !isUnit(a) =>
          s"if (${emitTerm(c)}) { ${emitBody(a, true)} } else { ${emitBody(b, true)} }"
        case If(c, a, b)      => emitIfStat(c, a, b)
        case Assign(lhs, rhs) => s"${emitTerm(lhs)} = ${emitTerm(rhs)};"
        case e if isUnit(e)   => ""
        case e if wantRet     => s"return ${emitTerm(e)};"
        case e                => s"${emitTerm(e)};"

    def allValDefs(stats: List[Statement]): Boolean =
      stats.nonEmpty && stats.forall { case _: ValDef => true; case _ => false }

    // A block's trailing expr that is really a STATEMENT (not a value to
    // inline): an assignment, or a nested statement block/if. Such a block is
    // a statement sequence, not an arg-hoist.
    def isStmtExpr(t: Term): Boolean = unwrap(t) match
      case _: Assign => true
      case _         => false

    // Scala hoists a call's named/default args into temp vals:
    //   { val theme$1 = …; val key$1 = W.$default$1; …; W(key$1, …, {theme: theme$1}) }
    // Inline the real ones back into the call by string substitution, and strip
    // the omitted-default ones (the widget runtime supplies the defaults). This
    // is what makes flutter.material widget construction render.
    def emitTempValBlock(vals: List[ValDef], expr: Term): String =
      def sub(in: String, name: String, value: String): String =
        val re = "(?<![A-Za-z0-9_$])" + java.util.regex.Pattern.quote(name) + "(?![A-Za-z0-9_$])"
        in.replaceAll(re, java.util.regex.Matcher.quoteReplacement(value))
      val marker = "__SART_DEF__"
      val (defaultVals, realVals) = vals.partition(_.rhs.exists(isDefaultArg))
      var out = emitTerm(expr)
      for vd <- defaultVals do out = sub(out, vd.name, marker)
      // reverse: a later val's inlined RHS may still reference an earlier one.
      for vd <- realVals.reverse do out = sub(out, vd.name, "(" + emitTerm(vd.rhs.get) + ")")
      val id = "[A-Za-z_$][A-Za-z0-9_$]*"
      out
        .replaceAll(id + ": " + marker + ", ", "")
        .replaceAll(", " + id + ": " + marker, "")
        .replaceAll(marker + ", ", "")
        .replaceAll(", " + marker, "")
        .replaceAll("\\(" + marker + "\\)", "()")
        .replaceAll(id + ": " + marker, "")
        .replaceAll(marker, "null")

    def emitBody(t: Term, wantRet: Boolean): String = unwrap(t) match
      // An all-ValDef block is Scala's arg-hoist (or pure val computation) —
      // inline it into one expression, then return/emit that. But NOT when the
      // trailing expr is a statement (an Assign like `history = history :+ x`):
      // that's a real statement sequence (e.g. a setState closure body), and
      // inlining would drop the vars and emit the Assign as an (unsupported)
      // expression — take the normal statement path instead.
      case Block(stats, expr) if allValDefs(stats) && !isStmtExpr(expr) =>
        val inlined = emitTempValBlock(stats.collect { case v: ValDef => v }, expr)
        if wantRet && !isUnit(expr) then s"return $inlined;"
        else if isUnit(expr) then inlined + ";"
        else s"$inlined;"
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
        // `val x` constructor params (ParamAccessor) have NO rhs — assign the
        // ctor arg to the field (`self.x = x`). Must be matched without the
        // `Some(rhs)` guard, which would skip them.
        case vd: ValDef if userMember(vd.symbol) && vd.symbol.flags.is(Flags.ParamAccessor) =>
          cb.append(s"$selfRef.${vd.name} = ${jsSafe(vd.name)}; ")
        case vd @ ValDef(name, _, Some(rhs)) if userMember(vd.symbol) =>
          cb.append(s"$selfRef.$name = ${emitTerm(rhs)}; ")
        case t: Term =>
          val st = emitStat(t); if st.nonEmpty then cb.append(s"$st ")
        case _ => ()
      }
      out.append(s"function $cn(${ctorParams.mkString(", ")}) { ${cb.toString.trim} }\n")
      parent.foreach { case (p, _) =>
        out.append(s"$cn.prototype = Object.create(${p.name}.prototype);\n")
        out.append(s"$cn.prototype.constructor = $cn;\n")
      }

      // methods (abstract defs — no rhs — are provided by subclasses; skip).
      // Includes paren-less user defs (getters like `latestRoll`) — emitted as
      // zero-arg prototype functions — but NOT field accessors (the `val`/`var`
      // getter/setter pairs, which would collide with the ctor-assigned field).
      cd.body.foreach {
        case dd @ DefDef(name, _, _, Some(_))
            if userMember(dd.symbol) && name != "<init>" && !name.endsWith("_=")
               && !dd.symbol.flags.is(Flags.FieldAccessor) =>
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
    Files.writeString(outDir.resolve("sart-runtime.js"), runtimeJs, StandardCharsets.UTF_8)
    Files.writeString(outDir.resolve("index.html"), indexHtml, StandardCharsets.UTF_8)

  /** The tiny host helpers — a separate file so a styled `index.html` overlay
   *  can reuse them without duplicating. NOT part of the app bundle; plain ES5;
   *  no Promise (works on old engines — webOS 3 / Tizen 2.3). Deferred is the
   *  async primitive the CPS lowering targets; Xhr returns one so
   *  `await(Xhr.get(u))` composes. */
  private def runtimeJs: String =
    """// Sart web-lite runtime: a tiny Flutter-widget-on-DOM renderer (ES5, no
      |// framework). Widget factories build styled DOM; StatefulWidget/State get
      |// a build+setState lifecycle. The SAME flutter.material Scala renders here
      |// and on Flutter proper. Styled by styles.css (deepPurple Material).
      |'use strict';
      |// ── async / misc (shared with the Dart backend's markers) ──
      |function Deferred() { this.cbs = []; this.done = false; this.val = undefined; }
      |Deferred.prototype.onComplete = function(f) { if (this.done) { f(this.val); } else { this.cbs.push(f); } };
      |Deferred.prototype.resolve = function(v) { this.done = true; this.val = v; for (var i = 0; i < this.cbs.length; i++) { this.cbs[i](this.val); } this.cbs = []; };
      |var Xhr = { get: function(url) { var d = new Deferred(); try { var x = new XMLHttpRequest(); x.open("GET", url, true); x.onreadystatechange = function() { if (x.readyState === 4) { d.resolve(x.responseText); } }; x.send(); } catch (e) { d.resolve(""); } return d; } };
      |var Random = function() { return { nextInt: function(b) { return Math.floor(Math.random() * b); } }; };
      |var Duration = function(o) { o = o || {}; return { ms: (o.days||0)*864e5 + (o.hours||0)*36e5 + (o.minutes||0)*6e4 + (o.seconds||0)*1e3 + (o.milliseconds||0) }; };
      |var Timer = { periodic: function(dur, cb) { var t = {}; t.id = setInterval(function() { cb(t); }, (dur && dur.ms) || 1000); t.cancel = function() { clearInterval(t.id); }; return t; } };
      |var Regex = function(o) { var p = (o != null && typeof o === "object" && "pattern" in o) ? o.pattern : o; var re = null; try { re = new RegExp(p); } catch (e) {} return { pattern: p, hasMatch: function(s) { return re ? re.test(s) : false; } }; };
      |function Exception(msg) { return new Error(msg == null ? "" : msg); }
      |function throw_(e) { throw e; }
      |
      |// ── DOM helpers ──
      |function _el(tag, cls) { var e = document.createElement(tag); if (cls) e.className = cls; return e; }
      |function _txt(s) { return document.createTextNode(s == null ? "" : String(s)); }
      |function _clear(n) { while (n.firstChild) n.removeChild(n.firstChild); }
      |var _ctx = {};
      |// Normalise any widget to a DOM node: a DOM node passes through; a
      |// StatefulWidget (has createState) gets a lifecycle host; a StatelessWidget
      |// (has build) is rendered; a string/number becomes text.
      |function _r(w) {
      |  if (w == null) return _txt("");
      |  if (w.nodeType) {
      |    // A <video> whose src was set while DETACHED plays but never paints
      |    // (its compositing layer is orphaned). Once it's actually in the
      |    // document, re-init it with load() so Chromium establishes the layer.
      |    if (w.tagName === "VIDEO" && w.src) {
      |      (function(vid){ function chk(){ if (vid.isConnected) { try { vid.load(); } catch (e) {} } else { requestAnimationFrame(chk); } } requestAnimationFrame(chk); })(w);
      |    }
      |    return w;
      |  }
      |  if (typeof w.createState === "function") return _mountStateful(w);
      |  if (typeof w.build === "function") return _r(w.build(_ctx));
      |  return _txt(w);
      |}
      |function _kids(list) { var f = document.createDocumentFragment(); if (list) for (var i = 0; i < list.length; i++) f.appendChild(_r(list[i])); return f; }
      |function _mountStateful(w) {
      |  var st = w.createState();
      |  st.widget = w;
      |  var host = _el("div", "sw-host");
      |  var mounted = false;
      |  // A synchronous setState during initState (e.g. an already-resolved
      |  // Future's callback firing inline) must NOT render before the initial
      |  // mount below — otherwise the subtree renders once here AND again below,
      |  // duplicating it and orphaning shared nodes (the single <video> gets
      |  // moved into the 2nd copy, leaving the 1st box empty). Just update state;
      |  // the initial mount picks it up.
      |  st.setState = function(fn) { if (fn) fn(); if (!mounted) return; _clear(host); host.appendChild(_r(st.build(_ctx))); };
      |  if (typeof st.initState === "function") { try { st.initState(); } catch (e) {} }
      |  host.appendChild(_r(st.build(_ctx)));
      |  mounted = true;
      |  return host;
      |}
      |function runApp(w) { var app = document.getElementById("app"); _clear(app); _navStack = []; app.appendChild(_r(w)); }
      |
      |// ── theme / enums / tokens ──
      |var Colors = { deepPurple: "#673ab7", white: "#ffffff", black: "#000000", grey: "#9e9e9e", red: "#f44336", blue: "#2196f3", green: "#4caf50", transparent: "transparent" };
      |var Icons = { menu: "☰", add: "+", check: "✓", edit: "✎", close: "✕", play_arrow: "▶", pause: "⏸", star: "★", home: "⌂", settings: "⚙", search: "⚲", favorite: "♥", arrow_back: "←", chevron_right: "›" };
      |var MainAxisAlignment = { start: "flex-start", center: "center", end: "flex-end", spaceBetween: "space-between", spaceAround: "space-around", spaceEvenly: "space-evenly" };
      |var CrossAxisAlignment = { start: "flex-start", center: "center", end: "flex-end", stretch: "stretch" };
      |var _scheme = { primary: "var(--primary)", onPrimary: "var(--on-primary)", primaryContainer: "var(--primary-container)", inversePrimary: "var(--inverse-primary)", surface: "var(--surface)", onSurface: "var(--on-surface)", secondary: "var(--secondary)" };
      |var _textTheme = { headlineLarge: "t-headline-lg", headlineMedium: "t-headline-md", headlineSmall: "t-headline-sm", titleLarge: "t-title-lg", titleMedium: "t-title-md", bodyLarge: "t-body-lg", bodyMedium: "t-body-md", labelLarge: "t-label" };
      |var ColorScheme = { fromSeed: function(o) { return _scheme; } };
      |var ThemeData = function(o) { return { colorScheme: (o && o.colorScheme) || _scheme, textTheme: _textTheme }; };
      |var Theme = { of: function(ctx) { return { colorScheme: _scheme, textTheme: _textTheme }; } };
      |var MaterialApp = function(o) { return _r((o && o.home) || _el("div")); };
      |
      |// ── layout / display widgets ──
      |function Scaffold(o) {
      |  o = o || {};
      |  var s = _el("div", "scaffold");
      |  if (o.appBar) s.appendChild(_r(o.appBar));
      |  var body = _el("div", "scaffold-body");
      |  if (o.body) body.appendChild(_r(o.body));
      |  s.appendChild(body);
      |  if (o.floatingActionButton) { var f = _r(o.floatingActionButton); f.className += " fab-pos"; s.appendChild(f); }
      |  return s;
      |}
      |function AppBar(o) {
      |  o = o || {};
      |  var bar = _el("div", "appbar");
      |  if (o.backgroundColor) bar.style.background = o.backgroundColor;
      |  if (_navStack.length > 0) { var b = _el("button", "appbar-back"); b.appendChild(_txt("←")); b.onclick = function() { _navPop(); }; bar.appendChild(b); }
      |  var t = _el("div", "appbar-title"); if (o.title) t.appendChild(_r(o.title)); bar.appendChild(t);
      |  if (o.actions && o.actions.length) { var a = _el("div", "appbar-actions"); a.appendChild(_kids(o.actions)); bar.appendChild(a); }
      |  return bar;
      |}
      |function Center(o) { var d = _el("div", "center"); if (o && o.child) d.appendChild(_r(o.child)); return d; }
      |function Column(o) { o = o || {}; var d = _el("div", "column"); if (o.mainAxisAlignment) d.style.justifyContent = o.mainAxisAlignment; if (o.crossAxisAlignment) d.style.alignItems = o.crossAxisAlignment; d.appendChild(_kids(o.children)); return d; }
      |function Row(o) { o = o || {}; var d = _el("div", "row"); if (o.mainAxisAlignment) d.style.justifyContent = o.mainAxisAlignment; if (o.crossAxisAlignment) d.style.alignItems = o.crossAxisAlignment; d.appendChild(_kids(o.children)); return d; }
      |function Text(o) { o = (o == null) ? {} : (typeof o === "object" ? o : { data: o }); var s = _el("span", "text"); if (o.style) s.className += " " + o.style; s.appendChild(_txt(o.data)); return s; }
      |function Icon(o) { var g = (o != null && typeof o === "object") ? (o.icon != null ? o.icon : "") : o; var s = _el("span", "icon"); if (o && typeof o === "object" && o.size != null) s.style.fontSize = o.size + "px"; s.appendChild(_txt(g)); return s; }
      |function SizedBox(o) { o = o || {}; var d = _el("div", "sizedbox"); if (o.width != null) d.style.width = o.width + "px"; if (o.height != null) d.style.height = o.height + "px"; if (o.child) d.appendChild(_r(o.child)); return d; }
      |function Padding(o) { o = o || {}; var d = _el("div"); if (o.padding != null) d.style.padding = o.padding; if (o.child) d.appendChild(_r(o.child)); return d; }
      |function Card(o) { var d = _el("div", "card"); if (o && o.child) d.appendChild(_r(o.child)); return d; }
      |function Expanded(o) { var d = _el("div", "expanded"); if (o && o.child) d.appendChild(_r(o.child)); return d; }
      |function Container(o) {
      |  o = o || {};
      |  var d = _el("div", "container");
      |  if (o.width != null) d.style.width = o.width + "px";
      |  if (o.height != null) d.style.height = o.height + "px";
      |  if (o.color) d.style.background = o.color;
      |  if (o.padding != null) d.style.padding = o.padding;
      |  if (o.decoration) { var dec = o.decoration; if (dec.color) d.style.background = dec.color; if (dec.borderRadius != null) d.style.borderRadius = dec.borderRadius; if (dec.boxShadow) d.style.boxShadow = dec.boxShadow; if (dec.border) d.style.border = dec.border.width + "px solid " + dec.border.color; }
      |  if (o.alignment === "center") { d.style.display = "flex"; d.style.alignItems = "center"; d.style.justifyContent = "center"; }
      |  if (o.child) d.appendChild(_r(o.child));
      |  return d;
      |}
      |var EdgeInsets = { all: function(v) { return v + "px"; }, symmetric: function(o) { o = o || {}; return (o.vertical || 0) + "px " + (o.horizontal || 0) + "px"; }, only: function(o) { o = o || {}; return (o.top||0)+"px "+(o.right||0)+"px "+(o.bottom||0)+"px "+(o.left||0)+"px"; } };
      |var BorderRadius = { circular: function(v) { return v + "px"; } };
      |var BoxShadow = function(o) { o = o || {}; return "0 " + ((o.blurRadius||4)/2) + "px " + (o.blurRadius||4) + "px " + (o.color || "rgba(0,0,0,.2)"); };
      |var BoxDecoration = function(o) { o = o || {}; var bs = o.boxShadow; if (bs && bs.length) bs = bs[0]; return { color: o.color, borderRadius: o.borderRadius, boxShadow: bs, border: o.border }; };
      |var Border = { all: function(o) { o = o || {}; return { width: (o.width || 1), color: (o.color || "transparent") }; } };
      |var BoxFit = { cover: "cover", contain: "contain", fill: "fill", fitWidth: "cover", fitHeight: "cover", none: "none", scaleDown: "scale-down" };
      |var InputDecoration = function(o) { return o || {}; };
      |var TextEditingController = function() { return { text: "" }; };
      |function TextField(o) {
      |  o = o || {};
      |  var i = _el("input", "textfield"); i.type = "text";
      |  if (o.decoration && o.decoration.labelText) i.placeholder = o.decoration.labelText;
      |  if (o.controller) { i.value = o.controller.text || ""; i.oninput = function() { o.controller.text = i.value; }; }
      |  return i;
      |}
      |function ElevatedButton(o) { o = o || {}; var b = _el("button", "btn btn-filled"); if (o.child) b.appendChild(_r(o.child)); if (o.onPressed) b.onclick = function() { o.onPressed(); }; return b; }
      |ElevatedButton.icon = function(o) { o = o || {}; var b = _el("button", "btn btn-filled"); if (o.icon) b.appendChild(_r(o.icon)); if (o.label) b.appendChild(_r(o.label)); if (o.onPressed) b.onclick = function() { o.onPressed(); }; return b; };
      |function TextButton(o) { o = o || {}; var b = _el("button", "btn btn-text"); if (o.child) b.appendChild(_r(o.child)); if (o.onPressed) b.onclick = function() { o.onPressed(); }; return b; }
      |function IconButton(o) { o = o || {}; var b = _el("button", "icon-btn"); if (o.icon) b.appendChild(_r(o.icon)); if (o.onPressed) b.onclick = function() { o.onPressed(); }; return b; }
      |function FloatingActionButton(o) { o = o || {}; var b = _el("button", "fab"); if (o.tooltip) b.title = o.tooltip; if (o.child) b.appendChild(_r(o.child)); if (o.onPressed) b.onclick = function() { o.onPressed(); }; return b; }
      |function ListView(o) { o = o || {}; var d = _el("div", "listview"); if (o.padding != null) d.style.padding = o.padding; d.appendChild(_kids(o.children)); return d; }
      |ListView.builder = function(o) { o = o || {}; var d = _el("div", "listview"); if (o.padding != null) d.style.padding = o.padding; var n = o.itemCount || 0; for (var i = 0; i < n; i++) { var w = o.itemBuilder(_ctx, i); if (w != null) d.appendChild(_r(w)); } return d; };
      |function ListTile(o) {
      |  o = o || {};
      |  var t = _el("div", "list-tile");
      |  if (o.leading) { var l = _el("div", "lt-leading"); l.appendChild(_r(o.leading)); t.appendChild(l); }
      |  var mid = _el("div", "lt-mid");
      |  if (o.title) { var ti = _el("div", "lt-title"); ti.appendChild(_r(o.title)); mid.appendChild(ti); }
      |  if (o.subtitle) { var su = _el("div", "lt-subtitle"); su.appendChild(_r(o.subtitle)); mid.appendChild(su); }
      |  t.appendChild(mid);
      |  if (o.trailing) { var tr = _el("div", "lt-trailing"); tr.appendChild(_r(o.trailing)); t.appendChild(tr); }
      |  if (o.onTap) t.onclick = function() { o.onTap(); };
      |  return t;
      |}
      |// ── navigation ──
      |var _navStack = [];
      |function _navPush(builder) { var app = document.getElementById("app"); _navStack.push(app.firstChild); var w = builder(_ctx); _clear(app); app.appendChild(_r(w)); }
      |function _navPop() { if (!_navStack.length) return; var app = document.getElementById("app"); var prev = _navStack.pop(); _clear(app); app.appendChild(prev); }
      |var MaterialPageRoute = function(o) { return { builder: (o && o.builder) }; };
      |var Navigator = { of: function(ctx) { return { push: function(route) { _navPush(route.builder); }, pop: function() { _navPop(); } }; }, pop: function() { _navPop(); } };
      |// Platform label (the @native PlatformName facade resolves here on web-lite).
      |var PlatformName = { describe: function() { return "web (web-lite)"; } };
      |
      |// ── extra common widgets ──
      |Icons.broken_image = "▧"; Icons.cloud = "☁"; Icons.check_circle = "✔"; Icons.hourglass_empty = "⌛"; Icons.check = "✓"; Icons.play_arrow = "▶"; Icons.pause = "⏸";
      |function CircularProgressIndicator(o) { return _el("div", "spinner"); }
      |function ClipRRect(o) { o = o || {}; var d = _el("div", "cliprrect"); d.style.overflow = "hidden"; if (o.borderRadius != null) d.style.borderRadius = o.borderRadius; if (o.child) d.appendChild(_r(o.child)); return d; }
      |function SingleChildScrollView(o) { o = o || {}; var d = _el("div", "scroll"); d.style.overflow = "auto"; if (o.padding != null) d.style.padding = o.padding; if (o.child) d.appendChild(_r(o.child)); return d; }
      |function Wrap(o) { o = o || {}; var d = _el("div", "wrap"); d.style.display = "flex"; d.style.flexWrap = "wrap"; var g = (o.spacing != null ? o.spacing : 8); d.style.gap = g + "px"; d.appendChild(_kids(o.children)); return d; }
      |var Uri = { parse: function(s) { return { href: s, toString: function() { return s; } }; }, file: function(s) { return { href: "file://" + s, toString: function() { return "file://" + s; } }; } };
      |
      |// ── sart-image (cached_network_image) ──
      |function CachedNetworkImage(o) { o = o || {}; var i = _el("img", "cni"); i.src = o.imageUrl; if (o.width != null) i.style.width = o.width + "px"; if (o.height != null) i.style.height = o.height + "px"; i.style.objectFit = o.fit || "cover"; i.style.display = "block"; i.style.background = "rgba(0,0,0,.06)"; return i; }
      |function CachedNetworkImageProvider(url, o) { return { url: url }; }
      |var CacheManager = function(c) { return {}; };
      |var Config = function(k, o) { return {}; };
      |
      |// ── sart-qr (qr_flutter) — real QR via a public generator, no bundled lib ──
      |function QrImageView(o) { o = o || {}; var s = o.size || 200; var i = _el("img", "qr"); i.width = s; i.height = s; i.style.width = s + "px"; i.style.height = s + "px"; i.style.background = o.backgroundColor || "#fff"; i.src = "https://api.qrserver.com/v1/create-qr-code/?size=" + s + "x" + s + "&data=" + encodeURIComponent(o.data || ""); return i; }
      |
      |// ── sart-lottie (lottie) — via lottie-web loaded in index.html ──
      |var Lottie = { network: function(url, o) { return _lottie(url, o); }, asset: function(name, o) { return _lottie(name, o); } };
      |function _lottie(path, o) { o = o || {}; var d = _el("div", "lottie"); d.style.width = "100%"; d.style.height = "100%"; setTimeout(function() { try { if (window.lottie) window.lottie.loadAnimation({ container: d, renderer: "svg", loop: (o.repeat !== false), autoplay: true, path: path }); else { d.appendChild(Icon("☁")); } } catch (e) { d.appendChild(Icon("☁")); } }, 30); return d; }
      |
      |// ── sart-webview (webview_flutter) ──
      |function WebViewController() { return {
      |  _url: null, _nav: null,
      |  setJavaScriptMode: function() { return this; }, setBackgroundColor: function() { return this; },
      |  setNavigationDelegate: function(d) { this._nav = d; return this; }, addJavaScriptChannel: function() { return this; },
      |  setUserAgent: function() { return new Deferred(); }, enableZoom: function() { return new Deferred(); },
      |  runJavaScript: function() { var d = new Deferred(); d.resolve(undefined); return d; },
      |  runJavaScriptReturningResult: function() { var d = new Deferred(); d.resolve(""); return d; },
      |  loadRequest: function(u) { this._url = (u && u.href) || String(u); var self = this; setTimeout(function() { if (self._nav && self._nav.onPageFinished) self._nav.onPageFinished(self._url); }, 400); var d = new Deferred(); d.resolve(undefined); return d; },
      |  loadHtmlString: function() { return new Deferred(); }, loadFlutterAsset: function() { return new Deferred(); }, loadFile: function() { return new Deferred(); },
      |  goBack: function() { return new Deferred(); }, goForward: function() { return new Deferred(); }, reload: function() { return new Deferred(); },
      |  clearCache: function() { return new Deferred(); }, clearLocalStorage: function() { return new Deferred(); }, removeJavaScriptChannel: function() { return new Deferred(); }
      |}; }
      |function WebViewWidget(o) { o = o || {}; var f = _el("iframe", "webview"); f.style.width = "100%"; f.style.height = "100%"; f.style.minHeight = "420px"; f.style.border = "0"; if (o.controller && o.controller._url) f.src = o.controller._url; return f; }
      |var NavigationDelegate = function(o) { return o || {}; };
      |var JavaScriptMode = { unrestricted: "unrestricted", disabled: "disabled" };
      |var JavaScriptMessage = function(o) { return o || {}; };
      |
      |// ── sart-player (VideoPlayer over <video>; YouTube over <iframe>) ──
      |function _mkVideoPlayer() {
      |  var v = _el("video", "video"); v.controls = true; v.setAttribute("playsinline", ""); v.style.width = "100%"; v.style.height = "100%"; v.style.objectFit = "contain"; v.style.background = "#000";
      |  // Force a normal composited layer: without this Chromium renders the
      |  // video as a hardware UNDERLAY behind the page and "hole-punches" through
      |  // the (opaque) page background, which fails here — the box shows white
      |  // while the video actually plays. translateZ(0) paints it in-page.
      |  v.style.transform = "translateZ(0)";
      |  return {
      |    _v: v,
      |    setSource: function(url, a, b) { v.src = url; var d = new Deferred(); d.resolve(undefined); return d; },
      |    view: function() { return v; }, play: function() { try { v.play(); } catch (e) {} }, pause: function() { v.pause(); },
      |    seek: function(s) { v.currentTime = s; }, setVolume: function(x) { v.volume = x; }, setLooping: function(x) { v.loop = x; },
      |    position: function() { return v.currentTime; }, duration: function() { return v.duration || 0; }, paused: function() { return v.paused; },
      |    dispose: function() { try { v.pause(); v.removeAttribute("src"); v.load(); } catch (e) {} }
      |  };
      |}
      |var VideoPlayer = { create: function() { return _mkVideoPlayer(); } };
      |var VideoBackendFactory = { create: function() { return _mkVideoPlayer(); } };
      |function YouTubeVideo() {
      |  var wrap = _el("div"); wrap.style.width = "100%"; wrap.style.height = "100%";
      |  var f = _el("iframe"); f.style.width = "100%"; f.style.height = "100%"; f.style.border = "0"; f.setAttribute("allow", "autoplay; encrypted-media; picture-in-picture"); f.setAttribute("allowfullscreen", ""); wrap.appendChild(f);
      |  return {
      |    setSource: function(id, a, b) { f.src = "https://www.youtube.com/embed/" + id; var d = new Deferred(); d.resolve(undefined); return d; },
      |    view: function() { return wrap; }, play: function() {}, pause: function() {}, dispose: function() { f.src = "about:blank"; }
      |  };
      |}
      |
      |// ── sart-tv (focus / remote / lifecycle / media session) ──
      |var TvKey = { Up: "Up", Down: "Down", Left: "Left", Right: "Right", Select: "Select", Back: "Back", PlayPause: "PlayPause" };
      |var TvPlatform = { current: "web", AppleTV: "AppleTV", Tizen: "Tizen", WebOS: "WebOS", AndroidTV: "AndroidTV", Other: "Other" };
      |// sart-tv widgets are real Scala classes (not @native), emitted as
      |// `new RemoteControl(onKey, child)` / `new Focusable(onSelect, builder,
      |// autofocus)` — POSITIONAL, in Scala declaration order. These runtime
      |// functions are their web-lite implementation.
      |var TvLifecycle = function(onPause, onResume) { return { dispose: function() {} }; };
      |TvLifecycle.apply = function(onPause, onResume) { return { dispose: function() {} }; };
      |function RemoteControl(onKey, child) { var d = _el("div", "remote"); d.tabIndex = 0; d.style.outline = "none"; if (child) d.appendChild(_r(child)); var map = { 38: "Up", 40: "Down", 37: "Left", 39: "Right", 13: "Select", 32: "Select", 8: "Back", 27: "Back" }; d.addEventListener("keydown", function(e) { var k = map[e.keyCode]; if (k && onKey) { var consumed = onKey(k); if (consumed) e.preventDefault(); } }); setTimeout(function() { try { d.focus(); } catch (e) {} }, 0); return d; }
      |function Focusable(onSelect, builder, autofocus) { var wrap = _el("div", "focusable"); wrap.tabIndex = 0; function render(f) { _clear(wrap); if (builder) wrap.appendChild(_r(builder(f))); } render(false); wrap.addEventListener("focus", function() { render(true); }); wrap.addEventListener("blur", function() { render(false); }); wrap.addEventListener("keydown", function(e) { if ((e.keyCode === 13 || e.keyCode === 32) && onSelect) { onSelect(); e.preventDefault(); } }); wrap.addEventListener("click", function() { try { wrap.focus(); } catch (e) {} if (onSelect) onSelect(); }); if (autofocus) setTimeout(function() { try { wrap.focus(); } catch (e) {} }, 0); return wrap; }
      |var MediaCallbacks = function(o) { return o || {}; };
      |var AudioServiceConfig = function(o) { return o || {}; };
      |var MediaItem = function(o) { return o || {}; };
      |function _mkAudioHandler(cb) { return { _cb: cb, setNowPlaying: function() {}, setPlaying: function() {}, setCallbacks: function(c) { this._cb = c; } }; }
      |var MediaSession = { init: function(cb, cfg) { var d = new Deferred(); d.resolve(_mkAudioHandler(cb)); return d; } };
      |var AudioService = { init: function(o) { var d = new Deferred(); d.resolve(_mkAudioHandler(o)); return d; } };
      |""".stripMargin

  /** Fallback host page (used when the app supplies no `web/` overlay). Links
   *  the runtime + an optional `styles.css`, then the emitted app. */
  private def indexHtml: String =
    s"""<!doctype html>
       |<html>
       |<head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>$projectName</title><link rel="stylesheet" href="styles.css"></head>
       |<body>
       |<div id="app"></div>
       |<script src="sart-runtime.js"></script>
       |<script src="app.js"></script>
       |</body>
       |</html>
       |""".stripMargin
