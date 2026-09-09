package webexample

import sart.web.*

/** Dice demo — a filled Roll button picks `Random.nextInt(6) + 1`, appends it
 *  to a history `List[Int]`, and renders the latest roll, the count, and the
 *  history in a card. */
class Dice extends Component:
  private var history: List[Int] = List()
  private var last: Int = 0

  private def roll(): Unit =
    last = Random.nextInt(6) + 1
    history = history :+ last
    setState()

  override def render(): Element =
    val card = document.createElement("div")
    card.className = "card"

    val cur = document.createElement("div")
    cur.className = "headline"
    cur.setAttribute("id", "last")
    cur.textContent = "Last: " + last.toString
    card.appendChild(cur)

    val count = document.createElement("div")
    count.className = "body-text"
    count.setAttribute("id", "count")
    count.textContent = "Rolls: " + history.size.toString
    card.appendChild(count)

    val btn = Ui.filledButton("Roll", () => roll())
    btn.setAttribute("id", "roll")
    card.appendChild(btn)

    val list = document.createElement("ul")
    list.className = "plainlist"
    list.setAttribute("id", "history")
    history.foreach { h =>
      val li = document.createElement("li")
      li.textContent = h.toString
      list.appendChild(li)
    }
    card.appendChild(list)

    Ui.scaffold("Sart Dice", card)
