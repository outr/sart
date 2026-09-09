package webexample

import sart.web.*

/** Dice demo — a Roll button picks `Random.nextInt(6) + 1`, appends it to a
 *  history `List[Int]`, and renders the latest roll, the count, and the
 *  history. */
class Dice extends Component:
  private var history: List[Int] = List()
  private var last: Int = 0

  private def roll(): Unit =
    last = Random.nextInt(6) + 1
    history = history :+ last
    setState()

  override def render(): Element =
    val root = document.createElement("div")
    val cur = document.createElement("p")
    cur.setAttribute("id", "last")
    cur.textContent = "Last: " + last.toString
    root.appendChild(cur)
    val count = document.createElement("p")
    count.setAttribute("id", "count")
    count.textContent = "Rolls: " + history.size.toString
    root.appendChild(count)
    val btn = document.createElement("button")
    btn.setAttribute("id", "roll")
    btn.textContent = "Roll"
    btn.addEventListener("click", e => roll())
    root.appendChild(btn)
    val list = document.createElement("ul")
    list.setAttribute("id", "history")
    history.foreach { h =>
      val li = document.createElement("li")
      li.textContent = h.toString
      list.appendChild(li)
    }
    root.appendChild(list)
    root
