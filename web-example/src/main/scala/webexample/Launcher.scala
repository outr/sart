package webexample

import sart.web.*

/** The web-lite example launcher: a Material nav bar over a content pane
 *  (mirroring the Flutter `LauncherApp`). Each nav button swaps the mounted
 *  `Component` (via the `App` router) into the content pane, so the nav stays
 *  put while you flip between demos. Reads the URL `#hash` for deep-linking a
 *  demo (used by screenshots), defaulting to Counter. */
class Launcher extends Component:
  private def open(name: String): Unit =
    if name == "todos" then App.show(Todos())
    else if name == "dice" then App.show(Dice())
    else if name == "stopwatch" then App.show(Stopwatch())
    else if name == "two" then App.show(ScreenA())
    else if name == "media" then App.show(Lite())
    else App.show(Counter())

  private def navButton(label: String, name: String): Element =
    val b = document.createElement("button")
    b.className = "navbtn"
    b.textContent = label
    b.addEventListener("click", e => open(name))
    b

  override def render(): Element =
    val root = document.createElement("div")

    val nav = document.createElement("div")
    nav.className = "nav"
    nav.appendChild(navButton("Counter", "counter"))
    nav.appendChild(navButton("Todos", "todos"))
    nav.appendChild(navButton("Dice", "dice"))
    nav.appendChild(navButton("Stopwatch", "stopwatch"))
    nav.appendChild(navButton("Two-screen", "two"))
    nav.appendChild(navButton("Media", "media"))

    val content = document.createElement("div")
    content.setAttribute("id", "content")
    // Route app switches into our content pane, not the whole page.
    App.useRoot(content)

    root.appendChild(nav)
    root.appendChild(content)

    // Initial view from the URL hash (deep-linkable for screenshots).
    val h = location.hash
    if h == "#todos" then open("todos")
    else if h == "#dice" then open("dice")
    else if h == "#stopwatch" then open("stopwatch")
    else if h == "#two" then open("two")
    else if h == "#media" then open("media")
    else open("counter")

    root

@main def main(): Unit =
  Launcher().mountInto(document.getElementById("app"))
