package webexample

import sart.web.*

/** Two-screen demo — the web-lite port of the Navigator example. `App` holds
 *  the mount root and swaps the mounted `Component`; screen A navigates to B,
 *  B's Back button returns to A. */
object App:
  private var root: Element = null

  def show(c: Component): Unit =
    if root != null then c.mountInto(root)

  def mount(id: String): Unit =
    root = document.getElementById(id)
    show(Counter())

class ScreenA extends Component:
  override def render(): Element =
    val root = document.createElement("div")
    val title = document.createElement("h1")
    title.setAttribute("id", "title")
    title.textContent = "Home"
    root.appendChild(title)
    val btn = document.createElement("button")
    btn.setAttribute("id", "go")
    btn.textContent = "Go to detail"
    btn.addEventListener("click", e => App.show(ScreenB()))
    root.appendChild(btn)
    root

class ScreenB extends Component:
  override def render(): Element =
    val root = document.createElement("div")
    val title = document.createElement("h1")
    title.setAttribute("id", "title")
    title.textContent = "Detail"
    root.appendChild(title)
    val msg = document.createElement("p")
    msg.setAttribute("id", "msg")
    msg.textContent = "You made it!"
    root.appendChild(msg)
    val back = document.createElement("button")
    back.setAttribute("id", "back")
    back.textContent = "Back"
    back.addEventListener("click", e => App.show(ScreenA()))
    root.appendChild(back)
    root

@main def main(): Unit = App.mount("app")
