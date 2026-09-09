package webexample

import sart.web.*

/** Todos demo — a text input + Add button appending to a `List[String]`,
 *  rendered as a `<ul>`. Exercises list append (`:+`), `foreach`, and reading a
 *  DOM input's `value` from a captured element. */
class Todos extends Component:
  private var items: List[String] = List()

  private def add(text: String): Unit =
    items = items :+ text
    setState()

  override def render(): Element =
    val root = document.createElement("div")
    val input = document.createElement("input")
    input.setAttribute("id", "todo-input")
    root.appendChild(input)
    val btn = document.createElement("button")
    btn.setAttribute("id", "add")
    btn.textContent = "Add"
    btn.addEventListener("click", e => add(input.value))
    root.appendChild(btn)
    val list = document.createElement("ul")
    list.setAttribute("id", "list")
    items.foreach { it =>
      val li = document.createElement("li")
      li.textContent = it
      list.appendChild(li)
    }
    root.appendChild(list)
    root
