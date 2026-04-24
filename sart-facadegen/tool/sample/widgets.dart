// A realistic Flutter-style Dart file exercising named parameters,
// required keywords, defaults, Map/List generics, function-typed
// params, and static methods — the stuff the MVP facadegen needs to
// survive to be useful on real pub packages.

library widgets;

class Widget {
  final String? key;
  Widget({this.key});
}

class Padding extends Widget {
  final double padding;
  final Widget child;
  Padding({required this.padding, required this.child, super.key});
}

class Button extends Widget {
  final String label;
  final void Function() onPressed;
  final bool enabled;
  Button({
    required this.label,
    required this.onPressed,
    this.enabled = true,
    super.key,
  });
}

class Config {
  final Map<String, int> values;
  final List<String> tags;
  Config(this.values, this.tags);

  static Config empty() => Config({}, []);
}

void debug(String message, {int level = 0}) {
  print('[L$level] $message');
}
