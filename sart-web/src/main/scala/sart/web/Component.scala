package sart.web

/** A minimal render-on-change UI component — the web-lite analogue of a
 *  `StatefulWidget`. Subclass it, hold state in `var`s, build a DOM subtree in
 *  [[render]], and call [[setState]] after mutating state to re-render. This is
 *  ORDINARY emitted Scala (not a facade): the whole class compiles to an ES5
 *  constructor + prototype, so the web-lite backend needs no UI runtime.
 *
 *  Re-render replaces the mounted subtree wholesale (`host.innerHTML = ""` then
 *  re-append) — no diffing, no animations: exactly what low-power targets want,
 *  and what the platforms' own models (SceneGraph observers, Lite's
 *  innerHTML-on-change) already do. */
abstract class Component:
  private var host: Element = null

  /** Build this component's DOM subtree. Called on mount and on every
   *  [[setState]]. */
  def render(): Element

  /** Mount into `h` and render once. */
  def mountInto(h: Element): Unit =
    host = h
    host.innerHTML = ""
    host.appendChild(render())

  /** Re-render in place after a state change (no-op before mount). */
  def setState(): Unit =
    if host != null then
      host.innerHTML = ""
      host.appendChild(render())
