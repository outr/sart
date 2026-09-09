package webexample

import sart.web.*

/** A real slice of a NaboTV-Lite-style web app, animation-free and
 *  render-on-change: a Card model, a rail renderer that builds DOM nodes, a
 *  D-pad key handler, an XHR fetch (callback-style) that renders on response,
 *  and a localStorage token read at startup. Compiled to tiny ES5 by Sart's
 *  web-lite backend (`--target=js`). */
case class Card(id: Int, title: String, posterPath: String)

object Lite:
  val TMDB: String = "https://image.tmdb.org/t/p/w342"
  var focus: Int = 0

  def renderRail(name: String, cards: List[Card]): Unit =
    val container = document.getElementById("app")
    val section = document.createElement("div")
    section.setAttribute("class", "rail")
    val heading = document.createElement("h2")
    heading.appendChild(document.createTextNode(name))
    section.appendChild(heading)
    val row = document.createElement("div")
    row.setAttribute("class", "row")
    cards.foreach { card =>
      val cell = document.createElement("div")
      cell.setAttribute("class", "card")
      cell.setAttribute("data-id", card.id.toString)
      val img = document.createElement("img")
      img.setAttribute("src", s"$TMDB${card.posterPath}")
      cell.appendChild(img)
      val label = document.createElement("div")
      label.appendChild(document.createTextNode(card.title))
      cell.appendChild(label)
      row.appendChild(cell)
    }
    section.appendChild(row)
    container.appendChild(section)

  def onKey(e: KeyEvent): Unit =
    if e.keyCode == 39 then focus = focus + 1
    else if e.keyCode == 37 then focus = focus - 1

  def start(): Unit =
    val token: String = localStorage.getItem("nabo.token")
    document.getElementById("app").addEventListener("keydown", e => onKey(e))
    Xhr.get(
      "/api/rails",
      resp =>
        val cards: List[Card] = List(
          Card(1, "Alpha", "/a.jpg"),
          Card(2, "Beta", "/b.jpg"),
          Card(3, "Gamma", "/c.jpg")
        )
        renderRail("Continue Watching", cards),
      () => renderRail("Error", Nil)
    )

@main def main(): Unit = Lite.start()
