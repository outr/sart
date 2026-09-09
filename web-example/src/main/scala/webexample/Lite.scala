package webexample

import sart.web.*
import sart.dart.{async, await}
import scala.concurrent.Future

/** A poster card in a rail. `posterPath` is a picsum image id for the demo. */
case class Card(id: Int, title: String, posterPath: String)

/** The NaboTV-Lite-style media catalogue: a rail of poster cards with D-pad
 *  focus, the web-lite port of a lean TV browser. A `Component` (render-on-
 *  change) — arrow keys move the highlight, `setState` re-renders. Kept
 *  synchronous here (static catalogue) so it renders standalone; the async
 *  fetch flow it grew from lives in [[LiteAsync]]. */
class Lite extends Component:
  private var focus: Int = 0
  private val cards: List[Card] = List(
    Card(1, "Aurora", "1015"),
    Card(2, "Skyline", "1016"),
    Card(3, "Harbor", "1018"),
    Card(4, "Wild", "1019"),
    Card(5, "Dunes", "1020"),
    Card(6, "Coast", "1024"),
    Card(7, "Summit", "1025"),
    Card(8, "Drift", "1027")
  )

  // Register D-pad handling once (constructor body — runs at construction).
  document.addEventListener("keydown", e => onKey(e))

  private def onKey(e: KeyEvent): Unit =
    if e.keyCode == 39 && focus < cards.size - 1 then focus = focus + 1
    else if e.keyCode == 37 && focus > 0 then focus = focus - 1
    setState()

  override def render(): Element =
    val rail = document.createElement("div")
    rail.className = "rail"

    val title = document.createElement("div")
    title.className = "rail-title"
    title.textContent = "Continue Watching"
    rail.appendChild(title)

    val row = document.createElement("div")
    row.className = "rail-row"
    row.setAttribute("id", "rail")

    var i = 0
    cards.foreach { card =>
      val cell = document.createElement("div")
      cell.className = if i == focus then "poster focused" else "poster"
      cell.setAttribute("data-id", card.id.toString)
      val img = document.createElement("img")
      img.setAttribute("src", "https://picsum.photos/id/" + card.posterPath + "/180/260")
      cell.appendChild(img)
      val label = document.createElement("div")
      label.className = "poster-label"
      label.textContent = card.title
      cell.appendChild(label)
      row.appendChild(cell)
      i = i + 1
    }
    rail.appendChild(row)

    val hint = document.createElement("div")
    hint.className = "rail-hint"
    hint.textContent = "◄ ► to move focus"
    rail.appendChild(hint)

    Ui.scaffold("Sart Lite", rail)

/** Async-CPS regression fixture: the original two-step `async`/`await` startup
 *  (token fetch feeding a rails fetch), lowered to callback ES5 by the web-lite
 *  backend. Exercised by the node harness; not shown in the launcher. */
object LiteAsync:
  var focus: Int = 0

  def renderRail(name: String, cards: List[Card]): Unit =
    val container = document.getElementById("app")
    val section = document.createElement("div")
    section.setAttribute("class", "rail")
    val heading = document.createElement("h2")
    heading.appendChild(document.createTextNode(name))
    section.appendChild(heading)
    container.appendChild(section)

  def loadToken(): Future[String] = async {
    await(Xhr.get("/token"))
  }

  def start(): Future[Unit] = async {
    val token = await(loadToken())
    await(Xhr.get("/api/rails?t=" + token))
    val cards: List[Card] = List(
      Card(1, "Alpha", "/a.jpg"),
      Card(2, "Beta", "/b.jpg")
    )
    renderRail("Continue Watching", cards)
  }
