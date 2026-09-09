package webexample

import sart.web.*

/** Counter demo — the web-lite port of the Flutter counter. A `Component`
 *  holding an `Int` var; the button increments it and re-renders. */
class Counter extends Component:
  private var count: Int = 0

  private def increment(): Unit =
    count = count + 1
    setState()

  override def render(): Element =
    val root = document.createElement("div")
    val label = document.createElement("p")
    label.setAttribute("id", "count")
    label.textContent = "Count: " + count.toString
    root.appendChild(label)
    val btn = document.createElement("button")
    btn.setAttribute("id", "inc")
    btn.textContent = "+"
    btn.addEventListener("click", e => increment())
    root.appendChild(btn)
    root
