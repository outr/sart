package sart.web

/** Material-flavored DOM builders — the web-lite analogue of Flutter's material
 *  widgets. Each returns an [[Element]] with the class names `styles.css`
 *  styles; this is ORDINARY emitted Scala over the `document`/[[Element]]
 *  facades, so no UI runtime is bundled. Compose them in a [[Component]]'s
 *  `render()`. */
object Ui:
  /** A text node with a typography class (`headline`/`title`/`body-text`/`label`). */
  def text(cls: String, s: String): Element =
    val e = document.createElement("div")
    e.className = cls
    e.textContent = s
    e

  /** A top app bar with a title. */
  def appBar(title: String): Element =
    val bar = document.createElement("div")
    bar.className = "appbar"
    val t = document.createElement("span")
    t.className = "appbar-title"
    t.textContent = title
    bar.appendChild(t)
    bar

  /** Scaffold: an app bar over a centered body. */
  def scaffold(title: String, body: Element): Element =
    val root = document.createElement("div")
    root.className = "scaffold"
    root.appendChild(appBar(title))
    val b = document.createElement("div")
    b.className = "body"
    b.appendChild(body)
    root.appendChild(b)
    root

  /** Scaffold with a floating action button pinned bottom-right. */
  def scaffoldFab(title: String, body: Element, fab: Element): Element =
    val root = scaffold(title, body)
    root.appendChild(fab)
    root

  /** A Material filled button. */
  def filledButton(label: String, onClick: () => Unit): Element =
    val b = document.createElement("button")
    b.className = "btn btn-filled"
    b.textContent = label
    b.addEventListener("click", e => onClick())
    b

  /** A Material text button. */
  def textButton(label: String, onClick: () => Unit): Element =
    val b = document.createElement("button")
    b.className = "btn btn-text"
    b.textContent = label
    b.addEventListener("click", e => onClick())
    b

  /** A floating action button showing `icon` (a glyph such as "+"). */
  def fab(icon: String, onClick: () => Unit): Element =
    val b = document.createElement("button")
    b.className = "fab"
    b.textContent = icon
    b.addEventListener("click", e => onClick())
    b

  /** A rounded, elevated surface wrapping `child`. */
  def card(child: Element): Element =
    val c = document.createElement("div")
    c.className = "card"
    c.appendChild(child)
    c

  /** A tappable list row with a title and subtitle. */
  def listTile(title: String, subtitle: String, onClick: () => Unit): Element =
    val tile = document.createElement("div")
    tile.className = "list-tile"
    val t = document.createElement("div")
    t.className = "list-tile-title"
    t.textContent = title
    tile.appendChild(t)
    val s = document.createElement("div")
    s.className = "list-tile-sub"
    s.textContent = subtitle
    tile.appendChild(s)
    tile.addEventListener("click", e => onClick())
    tile

  /** An outlined text input; returns the `<input>` so callers can read `value`. */
  def textField(placeholder: String): Element =
    val i = document.createElement("input")
    i.className = "textfield"
    i.setAttribute("placeholder", placeholder)
    i

  /** A horizontal row (button group). */
  def row(): Element =
    val r = document.createElement("div")
    r.className = "row"
    r
