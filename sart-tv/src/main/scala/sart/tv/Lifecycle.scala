package sart.tv

/** App-lifecycle handling for TV apps. TVs suspend and resume apps
 *  aggressively — the user presses Home, an assistant overlay takes over,
 *  the screensaver kicks in — so releasing the video pipeline on pause
 *  and reacquiring it on resume is essential. `TvLifecycle` is a thin
 *  factory over Flutter's [[AppLifecycleListener]] with no-op defaults, so
 *  a screen wires up only the transitions it cares about:
 *
 *  {{{
 *  private var lifecycle: AppLifecycleListener = null
 *  override def initState(): Unit =
 *    super.initState()
 *    lifecycle = TvLifecycle(onPause = () => player.pause(), onResume = () => player.play())
 *  override def dispose(): Unit =
 *    lifecycle.dispose()
 *    super.dispose()
 *  }}}
 */
object TvLifecycle:
  private def noop(): Unit = ()

  /** Register lifecycle callbacks and return the handle to [[AppLifecycleListener.dispose]]
   *  when the screen is torn down. `onHide` covers the TV "sent to
   *  background" case that `onPause` doesn't always fire on every embedder. */
  def apply(
    onResume: () => Unit = noop,
    onPause: () => Unit = noop,
    onHide: () => Unit = noop,
    onInactive: () => Unit = noop,
    onDetach: () => Unit = noop
  ): AppLifecycleListener =
    AppLifecycleListener(
      onResume = onResume,
      onPause = onPause,
      onHide = onHide,
      onInactive = onInactive,
      onDetach = onDetach
    )
