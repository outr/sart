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
    val root = document.createElement("div")
    val label = document.createElement("p")
    label.setAttribute("id", "elapsed")
    label.textContent = "Elapsed: " + seconds.toString + "s"
    root.appendChild(label)
    val startBtn = document.createElement("button")
    startBtn.setAttribute("id", "start")
    startBtn.textContent = "Start"
    startBtn.addEventListener("click", e => start())
    root.appendChild(startBtn)
    val stopBtn = document.createElement("button")
    stopBtn.setAttribute("id", "stop")
    stopBtn.textContent = "Stop"
    stopBtn.addEventListener("click", e => stop())
    root.appendChild(stopBtn)
    root
