package webexample

import sart.web.*

/** Todos demo — a Material text field + filled Add button appending to a
 *  `List[String]`, rendered as a card with a list. */
class Todos extends Component:
  private var items: List[String] = List()

  private def add(text: String): Unit =
    items = items :+ text
    setState()

  override def render(): Element =
    val card = document.createElement("div")
    card.className = "card"

    val row = Ui.row()
    val input = Ui.textField("New todo")
    input.setAttribute("id", "todo-input")
    row.appendChild(input)
    val addBtn = Ui.filledButton("Add", () => add(input.value))
    addBtn.setAttribute("id", "add")
    row.appendChild(addBtn)
    card.appendChild(row)

    val list = document.createElement("ul")
    list.className = "plainlist"
    list.setAttribute("id", "list")
    items.foreach { it =>
      val li = document.createElement("li")
      li.textContent = it
      list.appendChild(li)
    }
    card.appendChild(list)

    Ui.scaffold("Sart Todos", card)
