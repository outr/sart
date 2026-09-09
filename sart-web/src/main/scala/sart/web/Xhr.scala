package sart.web

import sart.dart.*
import scala.concurrent.Future

/** An XMLHttpRequest helper (`@native` facade) returning a `Future[String]`,
 *  so `await(Xhr.get(url))` reads in direct style. On old engines (webOS 3 /
 *  Tizen 2.3: no fetch, unreliable Promises) the emitted `Future` is a tiny
 *  host-provided `Deferred` (a few lines in the page), and `await` lowers to
 *  callback-passing style — never a bundled Promise library. */
@native
object Xhr:
  def get(url: String): Future[String] = native.value
