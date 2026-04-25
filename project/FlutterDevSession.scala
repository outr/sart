import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.nio.charset.StandardCharsets

/** Tracks the long-lived `flutter run` process started by `sartDev`.
 *  The task is meant to be driven by `sbt ~sartDev` — first invocation
 *  spawns flutter, subsequent invocations send `r\n` to its stdin to
 *  trigger a hot reload. Stored as a singleton so the second `~`
 *  iteration can find the process the first iteration started. A JVM
 *  shutdown hook quits the child cleanly when sbt exits.
 *
 *  Lives in `project/` (the meta-build) rather than build.sbt because
 *  `object` declarations at the top of build.sbt aren't visible inside
 *  task bodies.
 */
object FlutterDevSession {
  @volatile private var current: Option[Process] = None
  private val shutdownHookInstalled = new AtomicBoolean(false)

  def isRunning: Boolean = current.exists(_.isAlive)

  def start(cmd: Seq[String], cwd: java.io.File): Process = synchronized {
    val pb = new ProcessBuilder(cmd: _*)
    pb.directory(cwd)
    pb.redirectError(ProcessBuilder.Redirect.INHERIT)
    pb.redirectOutput(ProcessBuilder.Redirect.INHERIT)
    pb.redirectInput(ProcessBuilder.Redirect.PIPE)
    val p = pb.start()
    current = Some(p)
    if (shutdownHookInstalled.compareAndSet(false, true)) {
      Runtime.getRuntime.addShutdownHook(new Thread(() => stop()))
    }
    p
  }

  def reload():  Unit = current.filter(_.isAlive).foreach(send("r\n"))
  def restart(): Unit = current.filter(_.isAlive).foreach(send("R\n"))

  def stop(): Unit = synchronized {
    current.filter(_.isAlive).foreach { p =>
      try send("q\n")(p) catch { case _: Throwable => () }
      try p.waitFor(5, TimeUnit.SECONDS) catch { case _: Throwable => () }
      if (p.isAlive) p.destroy()
    }
    current = None
  }

  private def send(s: String)(p: Process): Unit = {
    val out = p.getOutputStream
    out.write(s.getBytes(StandardCharsets.UTF_8))
    out.flush()
  }
}
