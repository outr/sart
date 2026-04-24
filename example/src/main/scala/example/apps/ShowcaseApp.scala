package example.apps

import flutter.material.*
import sart.stdlib.{Random, Regex, Try, Success, Failure}

// Kitchen-sink showcase. A single scrollable screen with one Card per
// feature, each wired up to live state. Exercises (in order):
//
//   • setState counter
//   • Random + unicode face rendering
//   • sealed trait + pattern match (Shape → area)
//   • Option + List.filter.headOption lookup
//   • Try + throw + pattern match on Success/Failure
//   • for-comprehension generating a 4×4 multiplication table
//   • Regex email validation

sealed trait ShapeKind
case class CircleK(radius: Double) extends ShapeKind
case class SquareK(side: Double) extends ShapeKind
case class RectK(w: Double, h: Double) extends ShapeKind

case class Contact(name: String, phone: String)

class ShowcaseApp extends StatefulWidget:
  override def createState(): State[ShowcaseApp] = ShowcaseAppState()

class ShowcaseAppState extends State[ShowcaseApp]:
  private val rng: Random = Random()
  private val emailRegex: Regex =
    Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
  private val contacts: List[Contact] = List(
    Contact("Ada",   "+44 20 1234 5678"),
    Contact("Grace", "+1 212 555 0100"),
    Contact("Alan",  "+44 20 7946 0018")
  )
  private val shapes: List[ShapeKind] = List(
    CircleK(3.0), SquareK(4.0), RectK(3.0, 5.0)
  )

  private var counter: Int = 0
  private var face: Int = 1
  private var shapeIdx: Int = 0

  private val lookupCtrl: TextEditingController = TextEditingController()
  private var lookupResult: String = "— type a name above —"

  private var divisor: Int = 2
  private var divideResult: String = "— press compute —"

  private val emailCtrl: TextEditingController = TextEditingController()
  private var emailValid: Boolean = false
  private var emailTyped: Boolean = false

  private def faceGlyph(n: Int): String =
    "⚀⚁⚂⚃⚄⚅".substring(n - 1, n)

  private def area(s: ShapeKind): Double = s match
    case CircleK(r)    => 3.14159 * r * r
    case SquareK(a)    => a * a
    case RectK(w, h)   => w * h

  private def shapeLabel(s: ShapeKind): String = s match
    case CircleK(r)  => "Circle(r=" + r.toString + ")"
    case SquareK(a)  => "Square(side=" + a.toString + ")"
    case RectK(w, h) => "Rect(" + w.toString + "x" + h.toString + ")"

  private def runLookup(query: String): Unit =
    setState { () =>
      val hit: List[Contact] = contacts.filter(c => c.name == query)
      lookupResult = hit.headOption.fold("no match for '" + query + "'")(
        c => c.name + " → " + c.phone
      )
    }

  private def risky(d: Int): Double =
    if d == 0 then throw Exception("divide by zero")
    else 100.0 / d.toDouble

  private def tryDivide(d: Int): Try[Double] =
    try Success(risky(d))
    catch case e: Exception => Failure(e)

  private def runDivide(): Unit =
    setState { () =>
      divideResult = tryDivide(divisor) match
        case Success(n) => "100 / " + divisor.toString + " = " + n.toString
        case Failure(e) => "failed on divisor " + divisor.toString
      divisor = (divisor + 1) % 4
    }

  private def runEmail(raw: String): Unit =
    setState { () =>
      emailTyped = raw.length > 0
      emailValid = emailRegex.hasMatch(raw)
    }

  private def section(title: String, body: Widget): Widget =
    Card(
      child = Padding(
        padding = EdgeInsets.all(12.0),
        child = Column(
          mainAxisAlignment = MainAxisAlignment.start,
          children = List(
            Text(title, style = Theme.of(context).textTheme.titleMedium),
            SizedBox(height = 8.0),
            body
          )
        )
      )
    )

  private def counterCard: Widget =
    section(
      "setState counter",
      Row(
        mainAxisAlignment = MainAxisAlignment.start,
        children = List(
          Text("value: " + counter.toString),
          SizedBox(width = 12.0),
          ElevatedButton(
            onPressed = () => setState(() => counter = counter + 1),
            child = Text("+")
          ),
          SizedBox(width = 8.0),
          ElevatedButton(
            onPressed = () => setState(() => counter = counter - 1),
            child = Text("-")
          )
        )
      )
    )

  private def diceCard: Widget =
    section(
      "Random + unicode",
      Row(
        mainAxisAlignment = MainAxisAlignment.start,
        children = List(
          Text(faceGlyph(face) + "  (" + face.toString + ")"),
          SizedBox(width = 12.0),
          ElevatedButton(
            onPressed = () => setState(() => face = rng.nextInt(6) + 1),
            child = Text("Roll")
          )
        )
      )
    )

  private def shapeCard: Widget =
    val s: ShapeKind = shapes(shapeIdx)
    section(
      "Sealed trait + match",
      Column(
        mainAxisAlignment = MainAxisAlignment.start,
        children = List(
          Text(shapeLabel(s) + "  area = " + area(s).toString),
          SizedBox(height = 6.0),
          ElevatedButton(
            onPressed = () => setState(() =>
              shapeIdx = (shapeIdx + 1) % shapes.size
            ),
            child = Text("Next shape")
          )
        )
      )
    )

  private def lookupCard: Widget =
    section(
      "Option via List.filter.headOption",
      Column(
        mainAxisAlignment = MainAxisAlignment.start,
        children = List(
          TextField(
            controller = lookupCtrl,
            decoration = InputDecoration(
              labelText = "name (try Ada, Grace, Alan)"
            ),
            onChanged = (s: String) => runLookup(s)
          ),
          SizedBox(height = 6.0),
          Text(lookupResult)
        )
      )
    )

  private def tryCard: Widget =
    section(
      "Try + throw + pattern match",
      Column(
        mainAxisAlignment = MainAxisAlignment.start,
        children = List(
          Text("next divisor: " + divisor.toString),
          SizedBox(height = 6.0),
          ElevatedButton(
            onPressed = () => runDivide(),
            child = Text("Compute 100 / divisor")
          ),
          SizedBox(height = 6.0),
          Text(divideResult)
        )
      )
    )

  private def tableCard: Widget =
    val rows: List[Widget] =
      for r <- List(1, 2, 3, 4) yield
        val cells: List[Widget] =
          for c <- List(1, 2, 3, 4) yield
            SizedBox(
              width = 40.0,
              child = Text((r * c).toString)
            )
        Row(mainAxisAlignment = MainAxisAlignment.start, children = cells)
    section(
      "for-comprehension (4×4 times-table)",
      Column(mainAxisAlignment = MainAxisAlignment.start, children = rows)
    )

  private def emailCard: Widget =
    val status: String =
      if (!emailTyped) "— type something —"
      else if (emailValid) "✓ looks like an email"
      else "✗ not a valid email"
    section(
      "Regex email validator",
      Column(
        mainAxisAlignment = MainAxisAlignment.start,
        children = List(
          TextField(
            controller = emailCtrl,
            decoration = InputDecoration(labelText = "email"),
            onChanged = (s: String) => runEmail(s)
          ),
          SizedBox(height = 6.0),
          Text(status)
        )
      )
    )

  override def build(context: BuildContext): Widget =
    Scaffold(
      appBar = AppBar(
        title = Text("Showcase"),
        backgroundColor = Theme.of(context).colorScheme.inversePrimary
      ),
      body = ListView(
        padding = EdgeInsets.all(8.0),
        children = List(
          counterCard,
          diceCard,
          shapeCard,
          lookupCard,
          tryCard,
          tableCard,
          emailCard
        )
      )
    )
