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

  /** Point the router at an existing element (used by the launcher, which
   *  hosts the app content in its own pane). */
  def useRoot(e: Element): Unit =
    root = e

class ScreenA extends Component:
  override def render(): Element =
    val card = document.createElement("div")
    card.className = "card"
    val title = document.createElement("div")
    title.className = "title"
    title.setAttribute("id", "title")
    title.textContent = "Home"
    card.appendChild(title)
    val btn = Ui.filledButton("Go to detail", () => App.show(ScreenB()))
    btn.setAttribute("id", "go")
    card.appendChild(btn)
    Ui.scaffold("Sart Two-screen", card)

class ScreenB extends Component:
  override def render(): Element =
    val card = document.createElement("div")
    card.className = "card"
    val title = document.createElement("div")
    title.className = "title"
    title.setAttribute("id", "title")
    title.textContent = "Detail"
    card.appendChild(title)
    val msg = document.createElement("div")
    msg.className = "body-text"
    msg.setAttribute("id", "msg")
    msg.textContent = "You made it!"
    card.appendChild(msg)
    val back = Ui.textButton("Back", () => App.show(ScreenA()))
    back.setAttribute("id", "back")
    card.appendChild(back)
    Ui.scaffold("Sart Two-screen", card)
