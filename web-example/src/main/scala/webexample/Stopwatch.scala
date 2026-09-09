package webexample

import sart.web.*

/** Stopwatch demo — Start begins a `Timer.periodic` that ticks a seconds
 *  counter and re-renders; Stop cancels it. A nullable `Timer` var stands in
 *  for the Flutter `Option[Timer]`. */
class Stopwatch extends Component:
  private var seconds: Int = 0
  private var timer: Timer = null

  private def tick(): Unit =
    seconds = seconds + 1
    setState()

  private def start(): Unit =
    if timer == null then
      timer = Timer.periodic(1000, () => tick())
      setState()

  private def stop(): Unit =
    if timer != null then
      timer.cancel()
      timer = null
      setState()

  override def render(): Element =
    val card = document.createElement("div")
    card.className = "card"

    val label = document.createElement("div")
    label.className = "headline"
    label.setAttribute("id", "elapsed")
    label.textContent = "Elapsed: " + seconds.toString + "s"
    card.appendChild(label)

    val row = Ui.row()
    val startBtn = Ui.filledButton("Start", () => start())
    startBtn.setAttribute("id", "start")
    row.appendChild(startBtn)
    val stopBtn = Ui.textButton("Stop", () => stop())
    stopBtn.setAttribute("id", "stop")
    row.appendChild(stopBtn)
    card.appendChild(row)

    Ui.scaffold("Sart Stopwatch", card)
