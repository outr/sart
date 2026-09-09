package webexample

import sart.web.*

/** Counter demo — the web-lite port of the Flutter counter: a Material scaffold
 *  with an app bar, a card showing the count, and a `+` floating action button
 *  that increments and re-renders. */
class Counter extends Component:
  private var count: Int = 0

  private def increment(): Unit =
    count = count + 1
    setState()

  override def render(): Element =
    val card = document.createElement("div")
    card.className = "card"
    card.appendChild(Ui.text("body-text", "You have pushed the button this many times:"))
    val n = document.createElement("div")
    n.className = "headline"
    n.setAttribute("id", "count")
    n.textContent = "Count: " + count.toString
    card.appendChild(n)
    val f = Ui.fab("+", () => increment())
    f.setAttribute("id", "inc")
    Ui.scaffoldFab("Sart Counter", card, f)
