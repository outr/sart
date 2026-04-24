package example.features

import flutter.material.*
import sart.stdlib.{Stream, StreamController}
import scala.concurrent.Future

// Feature fixture: Flutter async builders + dart:async Stream.

class AsyncBuildersExample:
  def futureWidget(f: Future[String]): Widget =
    FutureBuilder[String](
      future = f,
      builder = (ctx: BuildContext, snap: AsyncSnapshot[String]) =>
        Text(snap.data.getOrElse("loading…"))
    )

  def streamWidget(s: Stream[Int]): Widget =
    StreamBuilder[Int](
      stream = s,
      builder = (ctx: BuildContext, snap: AsyncSnapshot[Int]) =>
        Text(snap.data.fold("-")(n => n.toString))
    )

  def makeController(): StreamController[Int] =
    StreamController.broadcast[Int]()
