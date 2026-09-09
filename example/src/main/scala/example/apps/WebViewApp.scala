package example.apps

import sart.dart.*
import sart.stdlib.Uri
import sart.webview.{WebViewController, WebViewWidget, NavigationDelegate, JavaScriptMode}
import flutter.material.*

/** Demo of sart-webview's `webview_flutter` facade: a controller configured
 *  with unrestricted JavaScript and a navigation delegate, loading a page and
 *  flipping a "loaded" flag when the page finishes. Mirrors how NaboTV drives
 *  its module / game / YouTube WebViews.
 */
class WebViewApp extends StatefulWidget:
  override def createState(): State[WebViewApp] = WebViewAppState()

class WebViewAppState extends State[WebViewApp]:
  private var controller: WebViewController = null
  private var loaded: Boolean = false

  override def initState(): Unit =
    super.initState()
    // Configure via statements (Dart would use a `..` cascade) — the calls
    // return Futures we fire and forget, so initState stays synchronous.
    val c = WebViewController()
    c.setJavaScriptMode(JavaScriptMode.unrestricted)
    c.setNavigationDelegate(
      NavigationDelegate(
        onPageFinished = (url: String) => setState(() => loaded = true)
      )
    )
    c.loadRequest(Uri.parse("https://flutter.dev"))
    controller = c

  override def build(context: BuildContext): Widget =
    Scaffold(
      appBar = AppBar(
        title = Text("Sart WebView"),
        actions = List(
          Icon(if loaded then Icons.check_circle else Icons.hourglass_empty)
        )
      ),
      body = WebViewWidget(controller = controller)
    )
