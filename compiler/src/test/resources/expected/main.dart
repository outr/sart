import 'dart:async';
import 'dart:convert';
import 'dart:math';
import 'package:audio_service/audio_service.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';
import 'package:media_kit/media_kit.dart';
import 'package:media_kit_video/media_kit_video.dart';
import 'platform/platform_name.dart';
import 'sart_either.dart';
import 'sart_option.dart';
import 'sart_try.dart';

/// Source: example/src/main/scala/example/LauncherApp.scala:14
class Demo {
  final String title;
  final String subtitle;
  final Widget Function(BuildContext) build;
  Demo(this.title, this.subtitle, this.build);

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is Demo &&
          other.title == title &&
          other.subtitle == subtitle &&
          other.build == build;

  @override
  int get hashCode => Object.hash(title, subtitle, build);

  @override
  String toString() =>
      'Demo(title: $title, subtitle: $subtitle, build: $build)';

  Demo copyWith({
    String? title,
    String? subtitle,
    Widget Function(BuildContext)? build,
  }) =>
      Demo(title ?? this.title, subtitle ?? this.subtitle, build ?? this.build);
}

/// Source: example/src/main/scala/example/LauncherApp.scala:12
void main() {
  runApp(LauncherApp());
}

/// Source: example/src/main/scala/example/LauncherApp.scala:20
class LauncherApp extends StatelessWidget {
  /// Source: example/src/main/scala/example/LauncherApp.scala:21
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: LauncherHome(),
      title: 'Sart Showcase',
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
      ),
    );
  }
}

/// Source: example/src/main/scala/example/LauncherApp.scala:31
class LauncherHome extends StatelessWidget {
  final List<Demo> demos = [
    Demo('Player', 'Cross-platform video/audio', (ctx) => PlayerApp()),
    Demo('TV', 'Remote/D-pad, focus, lifecycle', (ctx) => TvApp()),
    Demo('Showcase', 'Kitchen-sink feature demo', (ctx) => ShowcaseApp()),
    Demo('Counter', 'Classic Flutter counter', (ctx) => MyHomePage('Counter')),
    Demo('Todos', 'TextField + list + state', (ctx) => TodoApp()),
    Demo('Dice', 'Random + history + Navigator', (ctx) => DiceApp()),
    Demo('Stopwatch', 'Timer.periodic', (ctx) => ClockApp()),
    Demo('Two-screen', 'Navigator.push demo', (ctx) => HomeScreen()),
  ];

  /// Source: example/src/main/scala/example/LauncherApp.scala:43
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Sart Demos — ${PlatformName.describe()}'),
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
      ),
      body: ListView.builder(
        itemBuilder: (ctx, i) => (() {
          final d = demos[i];
          return ListTile(
            leading: Icon(Icons.menu),
            title: Text(d.title),
            subtitle: Text(d.subtitle),
            onTap: () {
              Navigator.of(ctx).push(MaterialPageRoute<void>(builder: d.build));
            },
          );
        })(),
        itemCount: demos.length,
      ),
    );
  }
}

/// Source: example/src/main/scala/example/CounterApp.scala:5
class MyApp extends StatelessWidget {
  /// Source: example/src/main/scala/example/CounterApp.scala:6
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: MyHomePage('Flutter Demo Home Page'),
      title: 'Flutter Demo',
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
      ),
    );
  }
}

/// Source: example/src/main/scala/example/CounterApp.scala:16
class MyHomePage extends StatefulWidget {
  final String title;
  MyHomePage(this.title);

  /// Source: example/src/main/scala/example/CounterApp.scala:17
  @override
  State<MyHomePage> createState() {
    return MyHomePageState();
  }
}

/// Source: example/src/main/scala/example/CounterApp.scala:20
class MyHomePageState extends State<MyHomePage> {
  int counter = 0;

  /// Source: example/src/main/scala/example/CounterApp.scala:23
  void incrementCounter() {
    setState(() {
      counter = counter + 1;
    });
  }

  /// Source: example/src/main/scala/example/CounterApp.scala:26
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.title),
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text('You have pushed the button this many times:'),
            Text(
              '${counter}',
              style: Theme.of(context).textTheme.headlineMedium,
            ),
          ],
        ),
      ),
      floatingActionButton: FloatingActionButton(
        child: Icon(Icons.add),
        tooltip: 'Increment',
        onPressed: () => incrementCounter(),
      ),
    );
  }
}

/// Source: example/src/main/scala/example/apps/ShowcaseApp.scala:18
class CircleK extends ShapeKind {
  final double radius;
  CircleK(this.radius);

  @override
  bool operator ==(Object other) =>
      identical(this, other) || other is CircleK && other.radius == radius;

  @override
  int get hashCode => radius.hashCode;

  @override
  String toString() => 'CircleK(radius: $radius)';

  CircleK copyWith({double? radius}) => CircleK(radius ?? this.radius);
  static CircleK fromJson(Map<String, dynamic> json) =>
      CircleK((json['radius'] as num).toDouble());

  @override
  Map<String, dynamic> toJson() => {'radius': radius, 'type': 'CircleK'};
}

/// Source: example/src/main/scala/example/apps/ClockApp.scala:11
class ClockApp extends StatefulWidget {
  /// Source: example/src/main/scala/example/apps/ClockApp.scala:12
  @override
  State<ClockApp> createState() {
    return ClockAppState();
  }
}

/// Source: example/src/main/scala/example/apps/ClockApp.scala:14
class ClockAppState extends State<ClockApp> {
  int seconds = 0;
  Timer? timer = null;

  /// Source: example/src/main/scala/example/apps/ClockApp.scala:18
  void start() {
    setState(
      () => timer = Timer.periodic(
        Duration(seconds: 1),
        (t) => setState(() {
          seconds = seconds + 1;
        }),
      ),
    );
  }

  /// Source: example/src/main/scala/example/apps/ClockApp.scala:28
  void stop() {
    setState(() {
      timer.foreach((t) => t.cancel());
      timer = null;
    });
  }

  /// Source: example/src/main/scala/example/apps/ClockApp.scala:34
  void reset() {
    setState(() {
      seconds = 0;
    });
  }

  /// Source: example/src/main/scala/example/apps/ClockApp.scala:37
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('Stopwatch')),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text('Elapsed: ' + seconds.toString() + 's'),
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                ElevatedButton(onPressed: () => start(), child: Text('Start')),
                ElevatedButton(onPressed: () => stop(), child: Text('Stop')),
                ElevatedButton(onPressed: () => reset(), child: Text('Reset')),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

/// Source: example/src/main/scala/example/apps/ShowcaseApp.scala:22
class Contact {
  final String name;
  final String phone;
  Contact(this.name, this.phone);

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is Contact && other.name == name && other.phone == phone;

  @override
  int get hashCode => Object.hash(name, phone);

  @override
  String toString() => 'Contact(name: $name, phone: $phone)';

  Contact copyWith({String? name, String? phone}) =>
      Contact(name ?? this.name, phone ?? this.phone);
  static Contact fromJson(Map<String, dynamic> json) =>
      Contact((json['name'] as String), (json['phone'] as String));

  Map<String, dynamic> toJson() => {'name': name, 'phone': phone};
}

/// Source: example/src/main/scala/example/apps/TwoScreenApp.scala:26
class DetailScreen extends StatelessWidget {
  /// Source: example/src/main/scala/example/apps/TwoScreenApp.scala:27
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('Detail')),
      body: Center(child: Text('You made it!')),
    );
  }
}

/// Source: example/src/main/scala/example/apps/DiceApp.scala:26
class DiceApp extends StatefulWidget {
  /// Source: example/src/main/scala/example/apps/DiceApp.scala:27
  @override
  State<DiceApp> createState() {
    return DiceAppState();
  }
}

/// Source: example/src/main/scala/example/apps/DiceApp.scala:29
class DiceAppState extends State<DiceApp> {
  final Random rng = Random();
  List<Roll> history = <Never>[];

  /// Source: example/src/main/scala/example/apps/DiceApp.scala:33
  void roll() {
    setState(
      () =>
          history = [...history, Roll(rng.nextInt(6) + 1, history.length + 1)],
    );
  }

  /// Source: example/src/main/scala/example/apps/DiceApp.scala:39
  void clearHistory() {
    setState(() {
      history = <Never>[];
    });
  }

  /// Source: example/src/main/scala/example/apps/DiceApp.scala:42
  Roll? get latestRoll {
    return (history.isEmpty ? null : history.last);
  }

  /// Source: example/src/main/scala/example/apps/DiceApp.scala:45
  String faceLabel(Roll? o) {
    return o.fold('—', (r) => '⚀⚁⚂⚃⚄⚅'.substring(r.face - 1, r.face));
  }

  /// Source: example/src/main/scala/example/apps/DiceApp.scala:48
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Dice'),
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
      ),
      body: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Center(
            child: Container(
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(16.0),
                boxShadow: [BoxShadow(color: Colors.grey, blurRadius: 8.0)],
              ),
              width: 120.0,
              height: 120.0,
              child: Center(child: Text(faceLabel(latestRoll))),
            ),
          ),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              ElevatedButton(onPressed: () => roll(), child: Text('Roll')),
              ElevatedButton(
                onPressed: () => clearHistory(),
                child: Text('Clear'),
              ),
            ],
          ),
          Text('Rolled ' + history.length.toString() + ' times'),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        child: Icon(Icons.menu),
        tooltip: 'History',
        onPressed: () {
          Navigator.of(context).push(
            MaterialPageRoute<void>(builder: (ctx) => HistoryScreen(history)),
          );
        },
      ),
    );
  }
}

/// Source: example/src/main/scala/example/apps/DiceApp.scala:93
class HistoryScreen extends StatelessWidget {
  final List<Roll> history;
  HistoryScreen(this.history);

  /// Source: example/src/main/scala/example/apps/DiceApp.scala:94
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('History')),
      body: ListView.builder(
        itemBuilder: (ctx, i) => ListTile(
          leading: Icon(Icons.check),
          title: Text('Roll #' + history[i].index.toString()),
          trailing: Text(history[i].face.toString()),
        ),
        itemCount: history.length,
      ),
    );
  }
}

/// Source: example/src/main/scala/example/apps/TwoScreenApp.scala:9
class HomeScreen extends StatelessWidget {
  /// Source: example/src/main/scala/example/apps/TwoScreenApp.scala:10
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('Home')),
      body: Center(
        child: ElevatedButton(
          onPressed: () {
            Navigator.of(
              context,
            ).push(MaterialPageRoute<void>(builder: (ctx) => DetailScreen()));
          },
          child: Text('Go to detail'),
        ),
      ),
    );
  }
}

/// Source: example/src/main/scala/example/apps/PlayerApp.scala:12
class PlayerApp extends StatefulWidget {
  /// Source: example/src/main/scala/example/apps/PlayerApp.scala:13
  @override
  State<PlayerApp> createState() {
    return PlayerAppState();
  }
}

/// Source: example/src/main/scala/example/apps/PlayerApp.scala:15
class PlayerAppState extends State<PlayerApp> {
  late VideoPlayer player;
  bool ready = false;

  /// Source: example/src/main/scala/example/apps/PlayerApp.scala:20
  @override
  void initState() {
    super.initState();
    load();
  }

  /// Source: example/src/main/scala/example/apps/PlayerApp.scala:24
  void load() async {
    player = VideoPlayer.create();
    (await player.setSource('sample.mp4'));
    setState(() {
      ready = true;
    });
  }

  /// Source: example/src/main/scala/example/apps/PlayerApp.scala:29
  @override
  void dispose() {
    player.dispose();
    super.dispose();
  }

  /// Source: example/src/main/scala/example/apps/PlayerApp.scala:33
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('Sart Player')),
      body: Center(
        child: (!ready
            ? CircularProgressIndicator()
            : Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  SizedBox(width: 480.0, height: 270.0, child: player.view()),
                  SizedBox(height: 16.0),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      ElevatedButton.icon(
                        onPressed: () => player.play(),
                        icon: Icon(Icons.play_arrow),
                        label: Text('Play'),
                      ),
                      SizedBox(width: 12.0),
                      ElevatedButton.icon(
                        onPressed: () => player.pause(),
                        icon: Icon(Icons.pause),
                        label: Text('Pause'),
                      ),
                    ],
                  ),
                ],
              )),
      ),
    );
  }
}

/// Source: example/src/main/scala/example/apps/ShowcaseApp.scala:20
class RectK extends ShapeKind {
  final double w;
  final double h;
  RectK(this.w, this.h);

  @override
  bool operator ==(Object other) =>
      identical(this, other) || other is RectK && other.w == w && other.h == h;

  @override
  int get hashCode => Object.hash(w, h);

  @override
  String toString() => 'RectK(w: $w, h: $h)';

  RectK copyWith({double? w, double? h}) => RectK(w ?? this.w, h ?? this.h);
  static RectK fromJson(Map<String, dynamic> json) =>
      RectK((json['w'] as num).toDouble(), (json['h'] as num).toDouble());

  @override
  Map<String, dynamic> toJson() => {'w': w, 'h': h, 'type': 'RectK'};
}

/// Source: example/src/main/scala/example/apps/DiceApp.scala:24
class Roll {
  final int face;
  final int index;
  Roll(this.face, this.index);

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is Roll && other.face == face && other.index == index;

  @override
  int get hashCode => Object.hash(face, index);

  @override
  String toString() => 'Roll(face: $face, index: $index)';

  Roll copyWith({int? face, int? index}) =>
      Roll(face ?? this.face, index ?? this.index);
  static Roll fromJson(Map<String, dynamic> json) =>
      Roll((json['face'] as num).toInt(), (json['index'] as num).toInt());

  Map<String, dynamic> toJson() => {'face': face, 'index': index};
}

/// Source: example/src/main/scala/example/apps/ShowcaseApp.scala:17
sealed class ShapeKind {
  static ShapeKind fromJson(Map<String, dynamic> json) {
    final String t = json['type'] as String;
    if (t == 'CircleK') return CircleK.fromJson(json);
    if (t == 'SquareK') return SquareK.fromJson(json);
    if (t == 'RectK') return RectK.fromJson(json);
    throw Exception('Unsupported type: ' + t);
  }

  Map<String, dynamic> toJson();
}

/// Source: example/src/main/scala/example/apps/ShowcaseApp.scala:24
class ShowcaseApp extends StatefulWidget {
  /// Source: example/src/main/scala/example/apps/ShowcaseApp.scala:25
  @override
  State<ShowcaseApp> createState() {
    return ShowcaseAppState();
  }
}

/// Source: example/src/main/scala/example/apps/ShowcaseApp.scala:27
class ShowcaseAppState extends State<ShowcaseApp> {
  final Random rng = Random();
  final RegExp emailRegex = RegExp('^[^@\\s]+@[^@\\s]+\\.[^@\\s]+\$');
  final List<Contact> contacts = [
    Contact('Ada', '+44 20 1234 5678'),
    Contact('Grace', '+1 212 555 0100'),
    Contact('Alan', '+44 20 7946 0018'),
  ];
  final List<ShapeKind> shapes = [CircleK(3.0), SquareK(4.0), RectK(3.0, 5.0)];
  int counter = 0;
  int face = 1;
  int shapeIdx = 0;
  final TextEditingController lookupCtrl = TextEditingController();
  String lookupResult = '— type a name above —';
  int divisor = 2;
  String divideResult = '— press compute —';
  final TextEditingController emailCtrl = TextEditingController();
  bool emailValid = false;
  bool emailTyped = false;

  /// Source: example/src/main/scala/example/apps/ShowcaseApp.scala:54
  String faceGlyph(int n) {
    return '⚀⚁⚂⚃⚄⚅'.substring(n - 1, n);
  }

  /// Source: example/src/main/scala/example/apps/ShowcaseApp.scala:57
  double area(ShapeKind s) {
    return switch (s) {
      CircleK(radius: var r) => (3.14159 * r) * r,
      SquareK(side: var a) => a * a,
      RectK(w: var w, h: var h) => w * h,
    };
  }

  /// Source: example/src/main/scala/example/apps/ShowcaseApp.scala:62
  String shapeLabel(ShapeKind s) {
    return switch (s) {
      CircleK(radius: var r) => 'Circle(r=' + r.toString() + ')',
      SquareK(side: var a) => 'Square(side=' + a.toString() + ')',
      RectK(w: var w, h: var h) =>
        'Rect(' + w.toString() + 'x' + h.toString() + ')',
    };
  }

  /// Source: example/src/main/scala/example/apps/ShowcaseApp.scala:67
  void runLookup(String query) {
    setState(
      () => (() {
        final hit = contacts.where((c) => c.name == query).toList();
        return lookupResult = (hit.isEmpty ? null : hit.first).fold(
          'no match for \'' + query + '\'',
          (c) => c.name + ' → ' + c.phone,
        );
      })(),
    );
  }

  /// Source: example/src/main/scala/example/apps/ShowcaseApp.scala:75
  double risky(int d) {
    return d == 0 ? throw Exception('divide by zero') : 100.0 / d.toDouble();
  }

  /// Source: example/src/main/scala/example/apps/ShowcaseApp.scala:79
  Try<double> tryDivide(int d) {
    try {
      return Success(risky(d));
    } on Object catch (e) {
      return Failure(e);
    }
  }

  /// Source: example/src/main/scala/example/apps/ShowcaseApp.scala:83
  void runDivide() {
    setState(() {
      divideResult = switch (tryDivide(divisor)) {
        Success<double>(value: var n) =>
          '100 / ' + divisor.toString() + ' = ' + n.toString(),
        Failure<double>(error: _) => 'failed on divisor ' + divisor.toString(),
      };
      divisor = (divisor + 1) % 4;
    });
  }

  /// Source: example/src/main/scala/example/apps/ShowcaseApp.scala:91
  void runEmail(String raw) {
    setState(() {
      emailTyped = raw.length > 0;
      emailValid = emailRegex.hasMatch(raw);
    });
  }

  /// Source: example/src/main/scala/example/apps/ShowcaseApp.scala:97
  Widget section(String title, Widget body) {
    return Card(
      child: Padding(
        padding: EdgeInsets.all(12.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.start,
          children: [
            Text(title, style: Theme.of(context).textTheme.titleMedium),
            SizedBox(height: 8.0),
            body,
          ],
        ),
      ),
    );
  }

  /// Source: example/src/main/scala/example/apps/ShowcaseApp.scala:112
  Widget get counterCard {
    return section(
      'setState counter',
      Row(
        mainAxisAlignment: MainAxisAlignment.start,
        children: [
          Text('value: ' + counter.toString()),
          SizedBox(width: 12.0),
          ElevatedButton(
            onPressed: () => setState(() {
              counter = counter + 1;
            }),
            child: Text('+'),
          ),
          SizedBox(width: 8.0),
          ElevatedButton(
            onPressed: () => setState(() {
              counter = counter - 1;
            }),
            child: Text('-'),
          ),
        ],
      ),
    );
  }

  /// Source: example/src/main/scala/example/apps/ShowcaseApp.scala:133
  Widget get diceCard {
    return section(
      'Random + unicode',
      Row(
        mainAxisAlignment: MainAxisAlignment.start,
        children: [
          Text(faceGlyph(face) + '  (' + face.toString() + ')'),
          SizedBox(width: 12.0),
          ElevatedButton(
            onPressed: () => setState(() {
              face = rng.nextInt(6) + 1;
            }),
            child: Text('Roll'),
          ),
        ],
      ),
    );
  }

  /// Source: example/src/main/scala/example/apps/ShowcaseApp.scala:149
  Widget get shapeCard {
    return (() {
      final s = shapes[shapeIdx];
      return section(
        'Sealed trait + match',
        Column(
          mainAxisAlignment: MainAxisAlignment.start,
          children: [
            Text(shapeLabel(s) + '  area = ' + area(s).toString()),
            SizedBox(height: 6.0),
            ElevatedButton(
              onPressed: () => setState(() {
                shapeIdx = (shapeIdx + 1) % shapes.length;
              }),
              child: Text('Next shape'),
            ),
          ],
        ),
      );
    })();
  }

  /// Source: example/src/main/scala/example/apps/ShowcaseApp.scala:168
  Widget get lookupCard {
    return section(
      'Option via List.filter.headOption',
      Column(
        mainAxisAlignment: MainAxisAlignment.start,
        children: [
          TextField(
            controller: lookupCtrl,
            decoration: InputDecoration(
              labelText: 'name (try Ada, Grace, Alan)',
            ),
            onChanged: (s) => runLookup(s),
          ),
          SizedBox(height: 6.0),
          Text(lookupResult),
        ],
      ),
    );
  }

  /// Source: example/src/main/scala/example/apps/ShowcaseApp.scala:187
  Widget get tryCard {
    return section(
      'Try + throw + pattern match',
      Column(
        mainAxisAlignment: MainAxisAlignment.start,
        children: [
          Text('next divisor: ' + divisor.toString()),
          SizedBox(height: 6.0),
          ElevatedButton(
            onPressed: () => runDivide(),
            child: Text('Compute 100 / divisor'),
          ),
          SizedBox(height: 6.0),
          Text(divideResult),
        ],
      ),
    );
  }

  /// Source: example/src/main/scala/example/apps/ShowcaseApp.scala:205
  Widget get tableCard {
    return section(
      'for-comprehension (4×4 times-table)',
      Column(
        mainAxisAlignment: MainAxisAlignment.start,
        children: [1, 2, 3, 4]
            .map(
              (r) => Row(
                mainAxisAlignment: MainAxisAlignment.start,
                children: [1, 2, 3, 4]
                    .map(
                      (c) => SizedBox(
                        width: 40.0,
                        child: Text((r * c).toString()),
                      ),
                    )
                    .toList(),
              ),
            )
            .toList(),
      ),
    );
  }

  /// Source: example/src/main/scala/example/apps/ShowcaseApp.scala:220
  Widget get emailCard {
    return section(
      'Regex email validator',
      Column(
        mainAxisAlignment: MainAxisAlignment.start,
        children: [
          TextField(
            controller: emailCtrl,
            decoration: InputDecoration(labelText: 'email'),
            onChanged: (s) => runEmail(s),
          ),
          SizedBox(height: 6.0),
          Text(
            (!emailTyped
                ? '— type something —'
                : emailValid
                ? '✓ looks like an email'
                : '✗ not a valid email'),
          ),
        ],
      ),
    );
  }

  /// Source: example/src/main/scala/example/apps/ShowcaseApp.scala:241
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Showcase'),
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
      ),
      body: ListView(
        padding: EdgeInsets.all(8.0),
        children: [
          counterCard,
          diceCard,
          shapeCard,
          lookupCard,
          tryCard,
          tableCard,
          emailCard,
        ],
      ),
    );
  }
}

/// Source: example/src/main/scala/example/apps/ShowcaseApp.scala:19
class SquareK extends ShapeKind {
  final double side;
  SquareK(this.side);

  @override
  bool operator ==(Object other) =>
      identical(this, other) || other is SquareK && other.side == side;

  @override
  int get hashCode => side.hashCode;

  @override
  String toString() => 'SquareK(side: $side)';

  SquareK copyWith({double? side}) => SquareK(side ?? this.side);
  static SquareK fromJson(Map<String, dynamic> json) =>
      SquareK((json['side'] as num).toDouble());

  @override
  Map<String, dynamic> toJson() => {'side': side, 'type': 'SquareK'};
}

/// Source: example/src/main/scala/example/apps/TodoApp.scala:20
class TodoApp extends StatefulWidget {
  /// Source: example/src/main/scala/example/apps/TodoApp.scala:21
  @override
  State<TodoApp> createState() {
    return TodoAppState();
  }
}

/// Source: example/src/main/scala/example/apps/TodoApp.scala:23
class TodoAppState extends State<TodoApp> {
  List<TodoItem> todos = <Never>[];
  final TextEditingController controller = TextEditingController();

  /// Source: example/src/main/scala/example/apps/TodoApp.scala:27
  void addTodoItem() {
    setState(() {
      todos = [...todos, TodoItem(controller.text, false)];
    });
  }

  /// Source: example/src/main/scala/example/apps/TodoApp.scala:30
  void clearAll() {
    setState(() {
      todos = <Never>[];
    });
  }

  /// Source: example/src/main/scala/example/apps/TodoApp.scala:36
  TodoItem? get peek {
    return (todos.isEmpty ? null : todos.first);
  }

  /// Source: example/src/main/scala/example/apps/TodoApp.scala:37
  List<TodoItem> markDone(int i) {
    return [
      ...todos.sublist(0, i),
      TodoItem('done', true),
      ...todos.sublist(i + 1),
    ];
  }

  /// Source: example/src/main/scala/example/apps/TodoApp.scala:40
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('Todos')),
      body: Column(
        mainAxisAlignment: MainAxisAlignment.start,
        children: [
          Padding(
            padding: EdgeInsets.all(8.0),
            child: TextField(
              controller: controller,
              decoration: InputDecoration(labelText: 'What needs doing?'),
            ),
          ),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              ElevatedButton(
                onPressed: () => addTodoItem(),
                child: Text('Add'),
              ),
              SizedBox(width: 8.0),
              ElevatedButton(
                onPressed: () => clearAll(),
                child: Text('Clear all'),
              ),
            ],
          ),
          Expanded(
            child: ListView.builder(
              itemBuilder: (ctx, i) => ListTile(
                leading: Icon(todos[i].done ? Icons.check : Icons.edit),
                title: Text(todos[i].text),
              ),
              itemCount: todos.length,
            ),
          ),
        ],
      ),
    );
  }
}

/// Source: example/src/main/scala/example/apps/TodoApp.scala:18
class TodoItem {
  final String text;
  final bool done;
  TodoItem(this.text, this.done);

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is TodoItem && other.text == text && other.done == done;

  @override
  int get hashCode => Object.hash(text, done);

  @override
  String toString() => 'TodoItem(text: $text, done: $done)';

  TodoItem copyWith({String? text, bool? done}) =>
      TodoItem(text ?? this.text, done ?? this.done);
  static TodoItem fromJson(Map<String, dynamic> json) =>
      TodoItem((json['text'] as String), (json['done'] as bool));

  Map<String, dynamic> toJson() => {'text': text, 'done': done};
}

/// Source: example/src/main/scala/example/apps/TvApp.scala:14
class TvApp extends StatefulWidget {
  /// Source: example/src/main/scala/example/apps/TvApp.scala:15
  @override
  State<TvApp> createState() {
    return TvAppState();
  }
}

/// Source: example/src/main/scala/example/apps/TvApp.scala:17
class TvAppState extends State<TvApp> {
  String lastKey = '—';
  int selected = -1;
  late AppLifecycleListener lifecycle;
  late SartAudioHandler session;

  /// Source: example/src/main/scala/example/apps/TvApp.scala:25
  @override
  void initState() {
    super.initState();
    lifecycle = TvLifecycle.apply(
      () => null,
      () => null,
      TvLifecycle.apply$default$3,
      TvLifecycle.apply$default$4,
      TvLifecycle.apply$default$5,
    );
    startSession();
  }

  /// Source: example/src/main/scala/example/apps/TvApp.scala:37
  void startSession() async {
    session = (await MediaSession.init(
      MediaCallbacks(
        onPlay: () => setState(() {
          lastKey = 'OS: play';
        }),
        onPause: () => setState(() {
          lastKey = 'OS: pause';
        }),
        onSkipToNext: () => setState(() {
          lastKey = 'OS: next';
        }),
      ),
      AudioServiceConfig(
        androidNotificationChannelId: 'tv.sart.example.audio',
        androidNotificationChannelName: 'Playback',
      ),
    ));
    session.setNowPlaying(
      MediaItem(id: 'demo', title: 'Sart TV demo', artist: 'sart-tv'),
    );
    session.setPlaying(false);
  }

  /// Source: example/src/main/scala/example/apps/TvApp.scala:56
  @override
  void dispose() {
    lifecycle.dispose();
    super.dispose();
  }

  /// Source: example/src/main/scala/example/apps/TvApp.scala:60
  bool onKey(TvKey key) {
    setState(() {
      lastKey = key.toJson();
    });
    final directional =
        (((key == TvKey.Up) || (key == TvKey.Down)) || (key == TvKey.Left)) ||
        (key == TvKey.Right);
    return !directional;
  }

  /// Source: example/src/main/scala/example/apps/TvApp.scala:68
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('Sart TV')),
      body: RemoteControl(
        (key) => onKey(key),
        Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text('Platform: ${TvPlatform.current}'),
              SizedBox(height: 8.0),
              Text('Last key: ${lastKey}'),
              SizedBox(height: 24.0),
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [card(0), gap, card(1), gap, card(2)],
              ),
              SizedBox(height: 24.0),
              selected >= 0
                  ? Text('Selected card ${selected}')
                  : Text('Nothing selected'),
            ],
          ),
        ),
      ),
    );
  }

  /// Source: example/src/main/scala/example/apps/TvApp.scala:94
  Widget get gap {
    return SizedBox(width: 16.0);
  }

  /// Source: example/src/main/scala/example/apps/TvApp.scala:96
  Widget card(int index) {
    return Focusable(
      () => setState(() {
        selected = index;
      }),
      (focused) => Container(
        decoration: BoxDecoration(
          color: (focused ? Colors.blue : Colors.grey),
          border: Border.all(
            color: focused ? Colors.white : Colors.transparent,
            width: 3.0,
          ),
          borderRadius: BorderRadius.circular(12.0),
        ),
        width: 120.0,
        height: 120.0,
        child: Center(child: Text('Card ${index}')),
      ),
      autofocus: (index == 0),
    );
  }
}

/// Source: example/src/main/scala/example/features/AsyncBuilders.scala:9
class AsyncBuildersExample {
  /// Source: example/src/main/scala/example/features/AsyncBuilders.scala:10
  Widget futureWidget(Future<String> f) {
    return FutureBuilder<String>(
      future: f,
      builder: (ctx, snap) => Text((snap.data ?? ('loading…'))),
    );
  }

  /// Source: example/src/main/scala/example/features/AsyncBuilders.scala:17
  Widget streamWidget(Stream<int> s) {
    return StreamBuilder<int>(
      stream: s,
      builder: (ctx, snap) => Text(snap.data.fold('-', (n) => n.toString())),
    );
  }

  /// Source: example/src/main/scala/example/features/AsyncBuilders.scala:24
  StreamController<int> makeController() {
    return StreamController.broadcast();
  }
}

/// Source: example/src/main/scala/example/features/FunctionTypes.scala:10
class BinaryOp {
  final int Function(int, int) op;
  BinaryOp(this.op);

  /// Source: example/src/main/scala/example/features/FunctionTypes.scala:11
  int apply(int a, int b) {
    return op(a, b);
  }
}

/// Source: example/src/main/scala/example/features/Generics.scala:22
class BoundedBox<T extends HasSize> {
  final T inner;
  BoundedBox(this.inner);

  /// Source: example/src/main/scala/example/features/Generics.scala:23
  int get measure {
    return inner.size;
  }
}

/// Source: example/src/main/scala/example/features/Generics.scala:9
class Box<T> {
  final T value;
  Box(this.value);

  /// Source: example/src/main/scala/example/features/Generics.scala:10
  T get() {
    return value;
  }
}

/// Source: example/src/main/scala/example/features/FunctionTypes.scala:7
class Callback {
  final void Function() fn;
  Callback(this.fn);

  /// Source: example/src/main/scala/example/features/FunctionTypes.scala:8
  void invoke() {
    fn();
  }
}

/// Source: example/src/main/scala/example/features/SealedHierarchy.scala:7
class Circle extends Shape {
  final double radius;
  Circle(this.radius);

  @override
  bool operator ==(Object other) =>
      identical(this, other) || other is Circle && other.radius == radius;

  @override
  int get hashCode => radius.hashCode;

  @override
  String toString() => 'Circle(radius: $radius)';

  Circle copyWith({double? radius}) => Circle(radius ?? this.radius);
  static Circle fromJson(Map<String, dynamic> json) =>
      Circle((json['radius'] as num).toDouble());

  @override
  Map<String, dynamic> toJson() => {'radius': radius, 'type': 'Circle'};
}

/// Source: example/src/main/scala/example/features/CtorPatterns.scala:11
class CtorPatternsExample {
  /// Source: example/src/main/scala/example/features/CtorPatterns.scala:12
  String describe(Vec p) {
    return switch (p) {
      Vec(x: 0, y: 0) => 'origin',
      Vec(x: 0, y: _) => 'on y-axis',
      Vec(x: _, y: 0) => 'on x-axis',
      Vec(x: var a, y: var b) => '(' + a.toString() + ', ' + b.toString() + ')',
    };
  }

  /// Source: example/src/main/scala/example/features/CtorPatterns.scala:19
  int recover(Try<int> t) {
    return switch (t) {
      Success<int>(value: var v) => v,
      Failure<int>(error: _) => -1,
    };
  }

  /// Source: example/src/main/scala/example/features/CtorPatterns.scala:24
  String eitherToString(Either<String, int> e) {
    return switch (e) {
      Left<String, int>(value: var err) => 'err: ' + err,
      Right<String, int>(value: var v) => 'ok: ' + v.toString(),
    };
  }
}

/// Source: example/src/main/scala/example/features/Currying.scala:6
class CurryingExample {
  /// Source: example/src/main/scala/example/features/Currying.scala:7
  int total(List<int> xs) {
    return xs.fold(0, (acc, x) => acc + x);
  }

  /// Source: example/src/main/scala/example/features/Currying.scala:10
  int userDefined(int x, int y) {
    return x + y;
  }

  /// Source: example/src/main/scala/example/features/Currying.scala:12
  int invoke() {
    return userDefined(3, 4);
  }
}

/// Source: example/src/main/scala/example/features/Decorations.scala:8
class DecorationsExample extends StatelessWidget {
  /// Source: example/src/main/scala/example/features/Decorations.scala:9
  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: Color.fromARGB(255, 200, 230, 255),
        borderRadius: BorderRadius.circular(12.0),
        boxShadow: [
          BoxShadow(
            color: Colors.grey,
            offset: Offset(0.0, 4.0),
            blurRadius: 8.0,
          ),
        ],
      ),
      width: 200.0,
      height: 120.0,
      child: Center(child: Text('Hello, styled world')),
    );
  }
}

/// Source: example/src/main/scala/example/features/DynJson.scala:17
class DynJson {
  /// Source: example/src/main/scala/example/features/DynJson.scala:18
  String nameOf(String payload) {
    return (() {
      final json = jsonDecode(payload);
      return (json['name'] == null) ? 'unknown' : (json['name'] as String);
    })();
  }

  /// Source: example/src/main/scala/example/features/DynJson.scala:22
  int ageOf(String payload) {
    return (jsonDecode(payload)['age'] as int);
  }

  /// Source: example/src/main/scala/example/features/DynJson.scala:25
  String render(String name, int age) {
    return jsonEncode({'name': name, 'age': age});
  }
}

/// Source: example/src/main/scala/example/features/TryEither.scala:20
class EitherExampleSart {
  /// Source: example/src/main/scala/example/features/TryEither.scala:21
  Either<String, int> okRight(int x) {
    return Right(x);
  }

  /// Source: example/src/main/scala/example/features/TryEither.scala:22
  Either<String, int> okLeft(String s) {
    return Left(s);
  }

  /// Source: example/src/main/scala/example/features/TryEither.scala:24
  Either<String, String> bimapRight(Either<String, int> e) {
    return e.map((n) => n.toString());
  }

  /// Source: example/src/main/scala/example/features/TryEither.scala:27
  String extract(Either<String, int> e) {
    return e.fold((err) => 'error: ' + err, (n) => 'ok: ' + n.toString());
  }

  /// Source: example/src/main/scala/example/features/TryEither.scala:31
  int rightOr(Either<String, int> e, int default_) {
    return e.getOrElse(default_);
  }

  /// Source: example/src/main/scala/example/features/TryEither.scala:33
  int? maybeRight(Either<String, int> e) {
    return e.toOption;
  }

  /// Source: example/src/main/scala/example/features/TryEither.scala:35
  Either<int, String> flip(Either<String, int> e) {
    return e.swap;
  }
}

extension SartIntSquared on int {
  int get squared {
    final int n = this;
    return n * n;
  }
}

extension SartIntPlusOne on int {
  int get plusOne {
    final int n = this;
    return n + 1;
  }
}

extension SartStringRepeatN on String {
  String repeatN(int count) {
    final String s = this;
    return s * count;
  }
}

/// Source: example/src/main/scala/example/features/Enums.scala:8
enum Filter {
  All,
  Active,
  Completed;

  String toJson() => switch (this) {
    Filter.All => 'Filter.All',
    Filter.Active => 'Filter.Active',
    Filter.Completed => 'Filter.Completed',
  };

  static Filter fromJson(dynamic json) {
    final String s = (json as String).toLowerCase();
    for (final v in values) {
      if (v.toJson().toLowerCase() == s) return v;
    }
    throw Exception('Unsupported Filter: ' + s);
  }
}

/// Source: example/src/main/scala/example/features/ForComp.scala:8
class ForCompExample {
  /// Source: example/src/main/scala/example/features/ForComp.scala:9
  List<int> incremented(List<int> xs) {
    return xs.map((x) => x + 1).toList();
  }

  /// Source: example/src/main/scala/example/features/ForComp.scala:12
  List<int> positives(List<int> xs) {
    return xs.where((x) => x > 0).toList().map((x) => x).toList();
  }

  /// Source: example/src/main/scala/example/features/ForComp.scala:18
  List<int> pairsSum(List<int> xs, List<int> ys) {
    return xs.expand((x) => ys.map((y) => x + y).toList()).toList();
  }
}

/// Source: example/src/main/scala/example/features/GivenUsing.scala:7
abstract mixin class Formatter<T> {
  /// Source: example/src/main/scala/example/features/GivenUsing.scala:8
  String format(T value);
}

/// Source: example/src/main/scala/example/features/Futures.scala:16
class FutureExample {
  /// Source: example/src/main/scala/example/features/Futures.scala:17
  Future<int> chain(Future<int> f) {
    return f.then((x) => x + 1);
  }

  /// Source: example/src/main/scala/example/features/Futures.scala:19
  Future<int> compose(Future<int> a, Future<int> b) {
    return a.then((x) => b.then((y) => x + y));
  }
}

/// Source: example/src/main/scala/example/features/GivenUsing.scala:13
class GivenExample {
  /// Source: example/src/main/scala/example/features/GivenUsing.scala:14
  String greet<T>(T value, Formatter<T> f) {
    return 'hello ' + f.format(value);
  }

  /// Source: example/src/main/scala/example/features/GivenUsing.scala:17
  String get demo {
    return greet(42, intFormatter);
  }
}

/// Source: example/src/main/scala/example/features/GivenUsing.scala:10
final Formatter<int> intFormatter = intFormatter$();

/// Source: example/src/main/scala/example/features/GivenUsing.scala:10
class intFormatter$ extends Formatter<int> {
  /// Source: example/src/main/scala/example/features/GivenUsing.scala:11
  String format(int value) {
    return 'int=' + value.toString();
  }
}

/// Source: example/src/main/scala/example/features/Generics.scala:19
abstract mixin class HasSize {
  /// Source: example/src/main/scala/example/features/Generics.scala:20
  int get size;
}

/// Source: example/src/main/scala/example/features/Inlines.scala:10
class InlineExample {
  /// Source: example/src/main/scala/example/features/Inlines.scala:11
  int tripleWrap(int n) {
    return doubled(n) + n;
  }
}

/// Source: example/src/main/scala/example/features/Inlines.scala:8
int doubled(int x) {
  return x * 2;
}

/// Source: example/src/main/scala/example/features/SealedHierarchy.scala:12
sealed class Json {
  const Json();
  static Json fromJson(Map<String, dynamic> json) {
    final String t = json['type'] as String;
    if (t == 'JsonString') return JsonString.fromJson(json);
    if (t == 'JsonNumber') return JsonNumber.fromJson(json);
    if (t == 'JsonBool') return JsonBool.fromJson(json);
    if (t == 'JsonNull') return JsonNull.fromJson(json);
    throw Exception('Unsupported type: ' + t);
  }

  Map<String, dynamic> toJson();
}

/// Source: example/src/main/scala/example/features/SealedHierarchy.scala:15
class JsonBool extends Json {
  final bool value;
  JsonBool(this.value);

  @override
  bool operator ==(Object other) =>
      identical(this, other) || other is JsonBool && other.value == value;

  @override
  int get hashCode => value.hashCode;

  @override
  String toString() => 'JsonBool(value: $value)';

  JsonBool copyWith({bool? value}) => JsonBool(value ?? this.value);
  static JsonBool fromJson(Map<String, dynamic> json) =>
      JsonBool((json['value'] as bool));

  @override
  Map<String, dynamic> toJson() => {'value': value, 'type': 'JsonBool'};
}

/// Source: example/src/main/scala/example/features/SealedHierarchy.scala:16
class JsonNull extends Json {
  const JsonNull();

  static JsonNull fromJson(Map<String, dynamic> json) => JsonNull();

  @override
  Map<String, dynamic> toJson() => {'type': 'JsonNull'};
}

/// Source: example/src/main/scala/example/features/SealedHierarchy.scala:14
class JsonNumber extends Json {
  final double value;
  JsonNumber(this.value);

  @override
  bool operator ==(Object other) =>
      identical(this, other) || other is JsonNumber && other.value == value;

  @override
  int get hashCode => value.hashCode;

  @override
  String toString() => 'JsonNumber(value: $value)';

  JsonNumber copyWith({double? value}) => JsonNumber(value ?? this.value);
  static JsonNumber fromJson(Map<String, dynamic> json) =>
      JsonNumber((json['value'] as num).toDouble());

  @override
  Map<String, dynamic> toJson() => {'value': value, 'type': 'JsonNumber'};
}

/// Source: example/src/main/scala/example/features/SealedHierarchy.scala:13
class JsonString extends Json {
  final String value;
  JsonString(this.value);

  @override
  bool operator ==(Object other) =>
      identical(this, other) || other is JsonString && other.value == value;

  @override
  int get hashCode => value.hashCode;

  @override
  String toString() => 'JsonString(value: $value)';

  JsonString copyWith({String? value}) => JsonString(value ?? this.value);
  static JsonString fromJson(Map<String, dynamic> json) =>
      JsonString((json['value'] as String));

  @override
  Map<String, dynamic> toJson() => {'value': value, 'type': 'JsonString'};
}

/// Source: example/src/main/scala/example/features/LayoutFacades.scala:8
class LayoutFacadesExample extends StatelessWidget {
  /// Source: example/src/main/scala/example/features/LayoutFacades.scala:9
  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 2.0,
      child: Stack(
        children: [
          Image.network('https://flutter.dev/assets/logo.png'),
          Positioned(
            left: 16.0,
            bottom: 16.0,
            child: Align(
              alignment: Alignment.bottomLeft,
              child: GestureDetector(child: Text('tap me'), onTap: () => null),
            ),
          ),
        ],
      ),
    );
  }
}

/// Source: example/src/main/scala/example/features/Lists.scala:8
class ListExample {
  /// Source: example/src/main/scala/example/features/Lists.scala:9
  List<int> get build {
    return [1, 2, 3];
  }

  /// Source: example/src/main/scala/example/features/Lists.scala:11
  List<int> triple(List<int> xs) {
    return xs;
  }
}

/// Source: example/src/main/scala/example/features/ListOps.scala:7
class ListOpsExample {
  /// Source: example/src/main/scala/example/features/ListOps.scala:8
  int count(List<int> xs) {
    return xs.length;
  }

  /// Source: example/src/main/scala/example/features/ListOps.scala:9
  int first(List<int> xs) {
    return xs.first;
  }

  /// Source: example/src/main/scala/example/features/ListOps.scala:10
  bool present(List<int> xs) {
    return xs.isNotEmpty;
  }

  /// Source: example/src/main/scala/example/features/ListOps.scala:11
  String joined(List<String> xs) {
    return xs.join(', ');
  }

  /// Source: example/src/main/scala/example/features/ListOps.scala:15
  int total(List<int> xs) {
    return xs.fold(0, (a, b) => a + b);
  }

  /// Source: example/src/main/scala/example/features/ListOps.scala:16
  int factor(List<int> xs) {
    return xs.fold(1, (a, b) => a * b);
  }

  /// Source: example/src/main/scala/example/features/ListOps.scala:17
  int smallest(List<int> xs) {
    return xs.reduce((a, b) => a < b ? a : b);
  }

  /// Source: example/src/main/scala/example/features/ListOps.scala:18
  int largest(List<int> xs) {
    return xs.reduce((a, b) => a > b ? a : b);
  }
}

/// Source: example/src/main/scala/example/features/Maps.scala:9
class MapSetExample {
  /// Source: example/src/main/scala/example/features/Maps.scala:10
  Map<String, int> get ages {
    return {'alice': 30, 'bob': 25};
  }

  /// Source: example/src/main/scala/example/features/Maps.scala:11
  Set<int> get primes {
    return {2, 3, 5, 7, 11};
  }

  /// Source: example/src/main/scala/example/features/Maps.scala:13
  bool contains(Map<String, int> m, String key) {
    return m.containsKey(key);
  }
}

/// Source: example/src/main/scala/example/features/FunctionTypes.scala:13
class Mapper<A, B> {
  final B Function(A) f;
  Mapper(this.f);

  /// Source: example/src/main/scala/example/features/FunctionTypes.scala:14
  B run(A a) {
    return f(a);
  }
}

/// Source: example/src/main/scala/example/features/MathRegex.scala:8
class MathRegexExample {
  /// Source: example/src/main/scala/example/features/MathRegex.scala:9
  double hypot(double a, double b) {
    return sqrt(pow(a, 2.0) + pow(b, 2.0));
  }

  /// Source: example/src/main/scala/example/features/MathRegex.scala:12
  double clampAngle(double radians) {
    return atan2(sin(radians), cos(radians));
  }

  /// Source: example/src/main/scala/example/features/MathRegex.scala:14
  double get quarterCircle {
    return pi / 4.0;
  }

  /// Source: example/src/main/scala/example/features/MathRegex.scala:16
  int dice(Random r) {
    return r.nextInt(6) + 1;
  }

  /// Source: example/src/main/scala/example/features/MathRegex.scala:18
  bool matchesNumber(String s) {
    return RegExp('^\\d+\$').hasMatch(s);
  }
}

/// Source: example/src/main/scala/example/features/Options.scala:11
class OptionExample {
  /// Source: example/src/main/scala/example/features/Options.scala:12
  String? lookup(int id) {
    return id == 0 ? null : 'found';
  }

  /// Source: example/src/main/scala/example/features/Options.scala:17
  int valueOrZero(int? o) {
    return (o ?? (0));
  }

  /// Source: example/src/main/scala/example/features/Options.scala:18
  bool isPresent(String? o) {
    return (o != null);
  }

  /// Source: example/src/main/scala/example/features/Options.scala:19
  bool isAbsent(String? o) {
    return (o == null);
  }

  /// Source: example/src/main/scala/example/features/Options.scala:24
  int? addOne(int? o) {
    return o.map((x) => x + 1);
  }

  /// Source: example/src/main/scala/example/features/Options.scala:25
  int? chain(int? o) {
    return o.flatMap((x) => x * 2);
  }

  /// Source: example/src/main/scala/example/features/Options.scala:26
  String describe(int? o) {
    return o.fold('nothing', (x) => 'got ' + x.toString());
  }
}

/// Source: example/src/main/scala/example/features/Generics.scala:12
class Pair<A, B> {
  final A first;
  final B second;
  Pair(this.first, this.second);

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is Pair && other.first == first && other.second == second;

  @override
  int get hashCode => Object.hash(first, second);

  @override
  String toString() => 'Pair(first: $first, second: $second)';

  Pair copyWith({A? first, B? second}) =>
      Pair(first ?? this.first, second ?? this.second);
}

/// Source: example/src/main/scala/example/features/PatternMatching.scala:5
class PatternMatchExample {
  /// Source: example/src/main/scala/example/features/PatternMatching.scala:6
  String label(int n) {
    return switch (n) {
      0 => 'zero',
      1 => 'one',
      _ => 'other',
    };
  }

  /// Source: example/src/main/scala/example/features/PatternMatching.scala:12
  String classifyEnum(Filter f) {
    return switch (f) {
      Filter.All => 'all',
      Filter.Active => 'active',
      Filter.Completed => 'completed',
    };
  }

  /// Source: example/src/main/scala/example/features/PatternMatching.scala:18
  String positiveOnly(int n) {
    return switch (n) {
      var x when x > 0 => 'positive',
      0 => 'zero',
      _ => 'negative',
    };
  }

  /// Source: example/src/main/scala/example/features/PatternMatching.scala:24
  String typeClassify(dynamic x) {
    return switch (x) {
      String _ => 'string',
      int _ => 'int',
      _ => 'other',
    };
  }
}

/// Source: example/src/main/scala/example/features/PlatformVariants.scala:39
class PlatformVariantDemo {
  /// Source: example/src/main/scala/example/features/PlatformVariants.scala:40
  String platformLabel() {
    return PlatformName.describe();
  }
}

/// Source: example/src/main/scala/example/features/CaseClasses.scala:6
class Point {
  final int x;
  final int y;
  Point(this.x, this.y);

  @override
  bool operator ==(Object other) =>
      identical(this, other) || other is Point && other.x == x && other.y == y;

  @override
  int get hashCode => Object.hash(x, y);

  @override
  String toString() => 'Point(x: $x, y: $y)';

  Point copyWith({int? x, int? y}) => Point(x ?? this.x, y ?? this.y);
  static Point fromJson(Map<String, dynamic> json) =>
      Point((json['x'] as num).toInt(), (json['y'] as num).toInt());

  Map<String, dynamic> toJson() => {'x': x, 'y': y};
}

/// Source: example/src/main/scala/example/features/Enums.scala:11
enum Priority {
  Low,
  Medium,
  High,
  Urgent;

  String toJson() => switch (this) {
    Priority.Low => 'Priority.Low',
    Priority.Medium => 'Priority.Medium',
    Priority.High => 'Priority.High',
    Priority.Urgent => 'Priority.Urgent',
  };

  static Priority fromJson(dynamic json) {
    final String s = (json as String).toLowerCase();
    for (final v in values) {
      if (v.toJson().toLowerCase() == s) return v;
    }
    throw Exception('Unsupported Priority: ' + s);
  }
}

/// Source: example/src/main/scala/example/features/Ranges.scala:8
class RangeExample {
  /// Source: example/src/main/scala/example/features/Ranges.scala:10
  List<int> inclusive(int n) {
    return List<int>.generate(n - 1 + 1, (i) => 1 + i).toList();
  }

  /// Source: example/src/main/scala/example/features/Ranges.scala:12
  List<int> exclusive(int n) {
    return List<int>.generate(n - 0, (i) => 0 + i).toList();
  }

  /// Source: example/src/main/scala/example/features/Ranges.scala:14
  List<int> squared(int n) {
    return List<int>.generate(
      n - 0,
      (i) => 0 + i,
    ).map((i) => i * i).toList().toList();
  }
}

/// Source: example/src/main/scala/example/features/SealedHierarchy.scala:8
class Rectangle extends Shape {
  final double width;
  final double height;
  Rectangle(this.width, this.height);

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is Rectangle && other.width == width && other.height == height;

  @override
  int get hashCode => Object.hash(width, height);

  @override
  String toString() => 'Rectangle(width: $width, height: $height)';

  Rectangle copyWith({double? width, double? height}) =>
      Rectangle(width ?? this.width, height ?? this.height);
  static Rectangle fromJson(Map<String, dynamic> json) => Rectangle(
    (json['width'] as num).toDouble(),
    (json['height'] as num).toDouble(),
  );

  @override
  Map<String, dynamic> toJson() => {
    'width': width,
    'height': height,
    'type': 'Rectangle',
  };
}

/// Source: example/src/main/scala/example/features/SealedHierarchy.scala:6
sealed class Shape {
  const Shape();
  static Shape fromJson(Map<String, dynamic> json) {
    final String t = json['type'] as String;
    if (t == 'Circle') return Circle.fromJson(json);
    if (t == 'Rectangle') return Rectangle.fromJson(json);
    if (t == 'UnitSquare') return UnitSquare.fromJson(json);
    throw Exception('Unsupported type: ' + t);
  }

  Map<String, dynamic> toJson();
}

/// Source: example/src/main/scala/example/features/Strings.scala:7
class StringExample {
  /// Source: example/src/main/scala/example/features/Strings.scala:8
  String shout(String s) {
    return s.toUpperCase();
  }

  /// Source: example/src/main/scala/example/features/Strings.scala:9
  String whisper(String s) {
    return s.toLowerCase();
  }

  /// Source: example/src/main/scala/example/features/Strings.scala:10
  String trimmed(String s) {
    return s.trim();
  }

  /// Source: example/src/main/scala/example/features/Strings.scala:11
  bool empty(String s) {
    return s.isEmpty;
  }

  /// Source: example/src/main/scala/example/features/Strings.scala:12
  List<String> parts(String s) {
    return s.split(',').toList();
  }

  /// Source: example/src/main/scala/example/features/Strings.scala:13
  String swap(String s) {
    return s.replaceAll('a', 'b');
  }

  /// Source: example/src/main/scala/example/features/Strings.scala:14
  int len(String s) {
    return s.length;
  }
}

/// Source: example/src/main/scala/example/features/CaseClasses.scala:8
class Todo {
  final int id;
  final String text;
  final bool done;
  Todo(this.id, this.text, this.done);

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is Todo &&
          other.id == id &&
          other.text == text &&
          other.done == done;

  @override
  int get hashCode => Object.hash(id, text, done);

  @override
  String toString() => 'Todo(id: $id, text: $text, done: $done)';

  Todo copyWith({int? id, String? text, bool? done}) =>
      Todo(id ?? this.id, text ?? this.text, done ?? this.done);
  static Todo fromJson(Map<String, dynamic> json) => Todo(
    (json['id'] as num).toInt(),
    (json['text'] as String),
    (json['done'] as bool),
  );

  Map<String, dynamic> toJson() => {'id': id, 'text': text, 'done': done};
}

/// Source: example/src/main/scala/example/features/TryCatch.scala:10
class TryExample {
  /// Source: example/src/main/scala/example/features/TryCatch.scala:12
  int fetchOrDefault(int lookup, int fallback) {
    try {
      return lookup + 1;
    } on Object {
      return fallback;
    }
  }

  /// Source: example/src/main/scala/example/features/TryCatch.scala:18
  int failing() {
    return throw Exception('boom');
  }

  /// Source: example/src/main/scala/example/features/TryCatch.scala:20
  String recoverBound() {
    try {
      return 'ok';
    } on Object {
      return 'failed';
    } finally {}
  }
}

/// Source: example/src/main/scala/example/features/TryEither.scala:11
class TryExampleSart {
  /// Source: example/src/main/scala/example/features/TryEither.scala:12
  Try<int> ok(int x) {
    return Success(x);
  }

  /// Source: example/src/main/scala/example/features/TryEither.scala:13
  Try<int> bad(Object e) {
    return Failure(e);
  }

  /// Source: example/src/main/scala/example/features/TryEither.scala:15
  Try<int> doubled(Try<int> t) {
    return t.map((n) => n * 2);
  }

  /// Source: example/src/main/scala/example/features/TryEither.scala:16
  int value(Try<int> t, int default_) {
    return t.getOrElse(default_);
  }

  /// Source: example/src/main/scala/example/features/TryEither.scala:18
  int? maybe(Try<int> t) {
    return t.toOption;
  }
}

/// Source: example/src/main/scala/example/features/Tuples.scala:11
class TupleExample {
  /// Source: example/src/main/scala/example/features/Tuples.scala:12
  (int, String) pair(int a, String b) {
    return (a, b);
  }

  /// Source: example/src/main/scala/example/features/Tuples.scala:13
  int first((int, String) t) {
    return t.$1;
  }

  /// Source: example/src/main/scala/example/features/Tuples.scala:14
  String second((int, String) t) {
    return t.$2;
  }

  /// Source: example/src/main/scala/example/features/Tuples.scala:15
  (String, int) swap((int, String) t) {
    return (t.$2, t.$1);
  }

  /// Source: example/src/main/scala/example/features/Tuples.scala:18
  List<(int, String)> zipped(List<int> xs, List<String> ys) {
    return List.generate(
      (xs.length < ys.length ? xs.length : ys.length),
      (i) => (xs[i], ys[i]),
    );
  }

  /// Source: example/src/main/scala/example/features/Tuples.scala:19
  List<(String, int)> indexed(List<String> xs) {
    return List.generate(xs.length, (i) => (xs[i], i));
  }

  /// Source: example/src/main/scala/example/features/Tuples.scala:20
  (List<int>, List<int>) split(List<int> xs) {
    return (
      xs.where((_$1) => _$1 > 0).toList(),
      xs.where((x) => !((_$1) => _$1 > 0)(x)).toList(),
    );
  }

  /// Source: example/src/main/scala/example/features/Tuples.scala:23
  Map<String, int> addEntry(Map<String, int> m, String k, int v) {
    return {...m, k: v};
  }
}

/// Source: example/src/main/scala/example/features/SealedHierarchy.scala:9
class UnitSquare extends Shape {
  const UnitSquare();

  static UnitSquare fromJson(Map<String, dynamic> json) => UnitSquare();

  @override
  Map<String, dynamic> toJson() => {'type': 'UnitSquare'};
}

/// Source: example/src/main/scala/example/features/CaseClasses.scala:10
class User {
  final String name;
  final String email;
  final int age;
  User(this.name, this.email, this.age);

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is User &&
          other.name == name &&
          other.email == email &&
          other.age == age;

  @override
  int get hashCode => Object.hash(name, email, age);

  @override
  String toString() => 'User(name: $name, email: $email, age: $age)';

  User copyWith({String? name, String? email, int? age}) =>
      User(name ?? this.name, email ?? this.email, age ?? this.age);
  static User fromJson(Map<String, dynamic> json) => User(
    (json['name'] as String),
    (json['email'] as String),
    (json['age'] as num).toInt(),
  );

  Map<String, dynamic> toJson() => {'name': name, 'email': email, 'age': age};
}

/// Source: example/src/main/scala/example/features/CtorPatterns.scala:9
class Vec {
  final int x;
  final int y;
  Vec(this.x, this.y);

  @override
  bool operator ==(Object other) =>
      identical(this, other) || other is Vec && other.x == x && other.y == y;

  @override
  int get hashCode => Object.hash(x, y);

  @override
  String toString() => 'Vec(x: $x, y: $y)';

  Vec copyWith({int? x, int? y}) => Vec(x ?? this.x, y ?? this.y);
  static Vec fromJson(Map<String, dynamic> json) =>
      Vec((json['x'] as num).toInt(), (json['y'] as num).toInt());

  Map<String, dynamic> toJson() => {'x': x, 'y': y};
}

/// Source: example/src/main/scala/example/features/Generics.scala:15
class Wrapping {
  /// Source: example/src/main/scala/example/features/Generics.scala:16
  Box<T> wrap<T>(T value) {
    return Box<T>(value);
  }
}

/// Source: sart-player/src/main/scala/sart/player/AudioTrackInfo.scala:6
class AudioTrackInfo {
  final String id;
  final String label;
  final String? language;
  AudioTrackInfo(this.id, this.label, this.language);

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is AudioTrackInfo &&
          other.id == id &&
          other.label == label &&
          other.language == language;

  @override
  int get hashCode => Object.hash(id, label, language);

  @override
  String toString() =>
      'AudioTrackInfo(id: $id, label: $label, language: $language)';

  AudioTrackInfo copyWith({String? id, String? label, String? language}) =>
      AudioTrackInfo(
        id ?? this.id,
        label ?? this.label,
        language ?? this.language,
      );
  static AudioTrackInfo fromJson(Map<String, dynamic> json) => AudioTrackInfo(
    (json['id'] as String),
    (json['label'] as String),
    json['language'] == null ? null : (json['language'] as String),
  );

  Map<String, dynamic> toJson() => {
    'id': id,
    'label': label,
    'language': language,
  };
}

/// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:19
class MediaKitVideo extends VideoPlayer {
  MediaKitVideo() {
    controller = VideoController(player);
    player.stream.completed.listen((done) {
      if (done && !endedFired) {
        endedFired = true;
        onEndedCb.foreach((cb) => cb());
      }
    });
    player.stream.error.listen((_$1) => onErrorCb.foreach((cb) => cb()));
  }

  final Player player = Player(
    configuration: PlayerConfiguration(logLevel: MPVLogLevel.info),
  );
  late VideoController controller;
  void Function()? onEndedCb = null;
  void Function()? onErrorCb = null;
  bool endedFired = false;
  bool disposed = false;
  bool coverFit = false;
  double volumeLevel = 1.0;
  double gainLevel = 1.0;

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:42
  void cmd(Future<void> Function() op) {
    if (!disposed) {
      op();
    }
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:45
  @override
  void setOnEnded(void Function() cb) {
    onEndedCb = cb;
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:46
  @override
  void setOnError(void Function() cb) {
    onErrorCb = cb;
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:48
  @override
  VideoSize? get videoSize {
    return (() {
      final w = (player.state.width ?? (0));
      final h = (player.state.height ?? (0));
      return (w > 0) && (h > 0) ? VideoSize(w, h) : null;
    })();
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:53
  @override
  bool get rendersImageSubtitles {
    return false;
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:55
  @override
  Future<void> setSource(
    String url, {
    double startSeconds = 0.0,
    List<SubtitleSource> sideloaded = const [],
  }) async {
    return disposed
        ? Future.value(null)
        : (await (() async {
            endedFired = false;
            try {
              if (startSeconds > 0.5) {
                (await player.open(Media(url), play: false));
                (await seekWhenReady(startSeconds));
                (await player.play());
              } else {
                (await player.open(Media(url)));
              }
              if (sideloaded.isNotEmpty) {
                (await addSubtitles(sideloaded));
              }
            } on Object {
              if (!disposed) {
                null;
              }
            }
            return Future.value(null);
          })());
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:57
  double get setSource$default$2 {
    return 0.0;
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:58
  List get setSource$default$3 {
    return [];
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:75
  Future<void> seekWhenReady(double seconds) async {
    if (player.state.duration.inMilliseconds <= 0) {
      (await player.stream.duration.firstWhere((d) => d.inMilliseconds > 0));
    }
    (await player.seek(Duration(milliseconds: (seconds * 1000).round())));
    return Future.value(null);
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:81
  @override
  Future<void> addSubtitles(List<SubtitleSource> subs) async {
    return (await (() async {
      final defaults = subs.where((s) => s.isDefault).toList();
      return defaults.isEmpty
          ? Future.value(null)
          : (await (() async {
              final pick = defaults.first;
              try {
                (await player.setSubtitleTrack(
                  SubtitleTrack.uri(
                    pick.url,
                    title: pick.label,
                    language: pick.language,
                  ),
                ));
              } on Object {
                null;
              }
              return Future.value(null);
            })());
    })());
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:94
  @override
  List<SubtitleTrackInfo> get subtitleTracks {
    return player.state.tracks.subtitle
        .where((t) => (t.id != 'no') && (t.id != 'auto'))
        .toList()
        .map(
          (t) => SubtitleTrackInfo(
            t.id,
            (t.title ??
                ((t.language.map((l) => l.toUpperCase()) ?? ('Subtitles')))),
            t.language,
            t.codec,
          ),
        )
        .toList();
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:106
  @override
  String? get currentSubtitleId {
    return (() {
      final s = player.state.track.subtitle;
      return (s.id == 'no') || (s.id == 'auto') ? null : s.id;
    })();
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:110
  @override
  Stream<void> get subtitleTracksStream {
    return player.stream.tracks.map((_$2) => null);
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:112
  @override
  void selectEmbeddedSubtitle(String id) {
    cmd(
      () => player.setSubtitleTrack(
        ((() {
              final r = player.state.tracks.subtitle
                  .where((s) => s.id == id)
                  .toList();
              return r.isEmpty ? null : r.first;
            })() ??
            (SubtitleTrack.no())),
      ),
    );
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:116
  @override
  void selectUriSubtitle(String url, String? label, String? language) {
    cmd(
      () => player.setSubtitleTrack(
        SubtitleTrack.uri(url, title: label, language: language),
      ),
    );
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:118
  Object? get selectUriSubtitle$default$2 {
    return null;
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:119
  Object? get selectUriSubtitle$default$3 {
    return null;
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:123
  @override
  void subtitlesOff() {
    cmd(() => player.setSubtitleTrack(SubtitleTrack.no()));
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:125
  @override
  List<AudioTrackInfo> get audioTracks {
    return player.state.tracks.audio
        .where((t) => (t.id != 'no') && (t.id != 'auto'))
        .toList()
        .map(
          (t) => AudioTrackInfo(
            t.id,
            (t.title ??
                ((t.language.map((l) => l.toUpperCase()) ?? ('Audio')))),
            t.language,
          ),
        )
        .toList();
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:136
  @override
  String? get currentAudioId {
    return (() {
      final a = player.state.track.audio;
      return (a.id == 'no') || (a.id == 'auto') ? null : a.id;
    })();
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:140
  @override
  Stream<void> get audioTracksStream {
    return player.stream.tracks.map((_$3) => null);
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:142
  @override
  void selectAudioTrack(String id) {
    cmd(
      () => player.setAudioTrack(
        ((() {
              final r = player.state.tracks.audio
                  .where((a) => a.id == id)
                  .toList();
              return r.isEmpty ? null : r.first;
            })() ??
            (AudioTrack.auto())),
      ),
    );
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:146
  @override
  void seek(double seconds) {
    cmd(() => player.seek(Duration(milliseconds: (seconds * 1000).round())));
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:149
  void applyVolume() {
    cmd(
      () =>
          player.setVolume(((volumeLevel * gainLevel) * 100).clamp(0.0, 200.0)),
    );
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:152
  @override
  void setVolume(double v) {
    volumeLevel = v;
    applyVolume();
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:153
  @override
  void setGain(double g) {
    gainLevel = g;
    applyVolume();
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:154
  @override
  void setLooping(bool v) {
    cmd(
      () => player.setPlaylistMode(v ? PlaylistMode.loop : PlaylistMode.none),
    );
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:156
  @override
  void setCover(bool v) {
    coverFit = v;
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:157
  @override
  void setPreferredSubtitleId(String? id) {}

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:158
  @override
  void setAuthHeader(String? v) {}

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:159
  @override
  void setAudioFocus(bool v) {}

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:160
  @override
  void setAudioOnly(bool v) {}

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:161
  @override
  Stream<List<double>>? get audioBands {
    return null;
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:163
  @override
  void play() {
    cmd(() => player.play());
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:164
  @override
  void pause() {
    cmd(() => player.pause());
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:165
  @override
  void setRate(double v) {
    cmd(() => player.setRate(v));
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:167
  @override
  double get position {
    return player.state.position.inMilliseconds / 1000.0;
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:168
  @override
  double get duration {
    return player.state.duration.inMilliseconds / 1000.0;
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:169
  @override
  bool get paused {
    return !player.state.playing;
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:170
  @override
  bool get ended {
    return (() {
      final dms = player.state.duration.inMilliseconds;
      return (dms > 0) && (player.state.position.inMilliseconds >= dms);
    })();
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:174
  @override
  void dispose() {
    if (!disposed) {
      disposed = true;
      (() {
        player.dispose();
      })();
    }
  }

  /// Source: sart-player/src/main/scala/sart/player/MediaKitVideo.scala:179
  @override
  Widget view() {
    return Video(
      controller: controller,
      controls: NoVideoControls,
      fit: coverFit ? BoxFit.cover : BoxFit.contain,
    );
  }
}

/// Source: sart-player/src/main/scala/sart/player/SubtitleSource.scala:6
class SubtitleSource {
  final String url;
  final String? language;
  final String? label;
  final bool isDefault;
  final String? id;
  SubtitleSource(
    this.url,
    this.language,
    this.label,
    this.id, {
    this.isDefault = false,
  });

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is SubtitleSource &&
          other.url == url &&
          other.language == language &&
          other.label == label &&
          other.isDefault == isDefault &&
          other.id == id;

  @override
  int get hashCode => Object.hash(url, language, label, isDefault, id);

  @override
  String toString() =>
      'SubtitleSource(url: $url, language: $language, label: $label, isDefault: $isDefault, id: $id)';

  SubtitleSource copyWith({
    String? url,
    String? language,
    String? label,
    bool? isDefault,
    String? id,
  }) => SubtitleSource(
    url ?? this.url,
    language ?? this.language,
    label ?? this.label,
    id ?? this.id,
    isDefault: isDefault ?? this.isDefault,
  );
  static SubtitleSource fromJson(Map<String, dynamic> json) => SubtitleSource(
    (json['url'] as String),
    json['language'] == null ? null : (json['language'] as String),
    json['label'] == null ? null : (json['label'] as String),
    json['id'] == null ? null : (json['id'] as String),
    isDefault: (json['isDefault'] as bool),
  );

  Map<String, dynamic> toJson() => {
    'url': url,
    'language': language,
    'label': label,
    'id': id,
    'isDefault': isDefault,
  };
}

/// Source: sart-player/src/main/scala/sart/player/SubtitleTrackInfo.scala:6
class SubtitleTrackInfo {
  final String id;
  final String label;
  final String? language;
  final String? codec;
  SubtitleTrackInfo(this.id, this.label, this.language, this.codec);

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is SubtitleTrackInfo &&
          other.id == id &&
          other.label == label &&
          other.language == language &&
          other.codec == codec;

  @override
  int get hashCode => Object.hash(id, label, language, codec);

  @override
  String toString() =>
      'SubtitleTrackInfo(id: $id, label: $label, language: $language, codec: $codec)';

  SubtitleTrackInfo copyWith({
    String? id,
    String? label,
    String? language,
    String? codec,
  }) => SubtitleTrackInfo(
    id ?? this.id,
    label ?? this.label,
    language ?? this.language,
    codec ?? this.codec,
  );
  static SubtitleTrackInfo fromJson(Map<String, dynamic> json) =>
      SubtitleTrackInfo(
        (json['id'] as String),
        (json['label'] as String),
        json['language'] == null ? null : (json['language'] as String),
        json['codec'] == null ? null : (json['codec'] as String),
      );

  Map<String, dynamic> toJson() => {
    'id': id,
    'label': label,
    'language': language,
    'codec': codec,
  };
}

/// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:16
abstract mixin class VideoPlayer {
  /// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:20
  void setTextureSurface(bool texture) {}

  /// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:24
  void relayout() {}

  /// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:28
  void setPreferredLanguages(String audio, String text) {}

  /// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:32
  void setVideoAdjust(double brightness, double warmth) {}

  /// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:34
  void setOnEnded(void Function() cb);

  /// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:35
  void setOnError(void Function() cb);

  /// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:39
  bool get ready {
    return (duration > 0) || (position > 0);
  }

  /// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:42
  String? get statusNote {
    return null;
  }

  /// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:45
  bool get buffering {
    return false;
  }

  /// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:48
  VideoSize? get videoSize {
    return null;
  }

  /// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:50
  Future<void> setSource(
    String url, {
    double startSeconds = 0.0,
    List<SubtitleSource> sideloaded = const [],
  });

  /// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:52
  double get setSource$default$2 {
    return 0.0;
  }

  /// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:53
  List get setSource$default$3 {
    return [];
  }

  /// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:55
  Future<void> addSubtitles(List<SubtitleSource> subs);

  /// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:59
  bool get rendersImageSubtitles;

  /// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:61
  List<SubtitleTrackInfo> get subtitleTracks;

  /// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:62
  String? get currentSubtitleId;

  /// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:63
  Stream<void> get subtitleTracksStream;

  /// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:64
  void selectEmbeddedSubtitle(String id);

  /// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:65
  void selectUriSubtitle(String url, String? label, String? language);

  /// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:67
  Object? get selectUriSubtitle$default$2 {
    return null;
  }

  /// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:68
  Object? get selectUriSubtitle$default$3 {
    return null;
  }

  /// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:70
  void subtitlesOff();

  /// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:72
  List<AudioTrackInfo> get audioTracks;

  /// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:73
  String? get currentAudioId;

  /// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:74
  Stream<void> get audioTracksStream;

  /// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:75
  void selectAudioTrack(String id);

  /// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:77
  void seek(double seconds);

  /// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:78
  void play();

  /// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:79
  void pause();

  /// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:82
  void setRate(double v);

  /// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:84
  void setVolume(double v);

  /// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:87
  void setGain(double g);

  /// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:89
  void setLooping(bool v);

  /// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:91
  void setCover(bool v);

  /// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:93
  void setPreferredSubtitleId(String? id);

  /// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:96
  void setAuthHeader(String? v);

  /// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:99
  void setAudioFocus(bool v);

  /// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:102
  void setAudioOnly(bool v);

  /// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:106
  Stream<List<double>>? get audioBands;

  /// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:108
  double get position;

  /// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:109
  double get duration;

  /// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:110
  bool get paused;

  /// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:111
  bool get ended;

  /// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:113
  void dispose();

  /// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:114
  Widget view();

  /// Source: sart-player/src/main/scala/sart/player/VideoPlayer.scala:120
  static VideoPlayer create() {
    MediaKit.ensureInitialized();
    return MediaKitVideo();
  }
}

/// Source: sart-player/src/main/scala/sart/player/VideoSize.scala:5
class VideoSize {
  final int width;
  final int height;
  VideoSize(this.width, this.height);

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is VideoSize && other.width == width && other.height == height;

  @override
  int get hashCode => Object.hash(width, height);

  @override
  String toString() => 'VideoSize(width: $width, height: $height)';

  VideoSize copyWith({int? width, int? height}) =>
      VideoSize(width ?? this.width, height ?? this.height);
  static VideoSize fromJson(Map<String, dynamic> json) => VideoSize(
    (json['width'] as num).toInt(),
    (json['height'] as num).toInt(),
  );

  Map<String, dynamic> toJson() => {'width': width, 'height': height};
}

/// Source: sart-tv/src/main/scala/sart/tv/Focusable.scala:20
class Focusable extends StatefulWidget {
  final void Function() onSelect;
  final Widget Function(bool) builder;
  final bool autofocus;
  Focusable(this.onSelect, this.builder, {this.autofocus = false});

  /// Source: sart-tv/src/main/scala/sart/tv/Focusable.scala:25
  @override
  State<Focusable> createState() {
    return FocusableState();
  }
}

/// Source: sart-tv/src/main/scala/sart/tv/Focusable.scala:27
class FocusableState extends State<Focusable> {
  bool focused = false;

  /// Source: sart-tv/src/main/scala/sart/tv/Focusable.scala:30
  @override
  Widget build(BuildContext context) {
    return Focus(
      child: widget.builder(focused),
      autofocus: widget.autofocus,
      onFocusChange: (f) => setState(() {
        focused = f;
      }),
      onKeyEvent: (node, event) => handle(event),
    );
  }

  /// Source: sart-tv/src/main/scala/sart/tv/Focusable.scala:38
  KeyEventResult handle(Object event) {
    return !(event is KeyDownEvent)
        ? KeyEventResult.ignored
        : (() {
            final decoded = TvKey.fromLogicalKey(
              (event as KeyEvent).logicalKey,
            );
            return !(decoded == null) && (decoded! == TvKey.Select)
                ? (() {
                    widget.onSelect();
                    return KeyEventResult.handled;
                  })()
                : KeyEventResult.ignored;
          })();
  }
}

/// Source: sart-tv/src/main/scala/sart/tv/Remote.scala:18
class RemoteControl extends StatelessWidget {
  final bool Function(TvKey) onKey;
  final Widget child;
  RemoteControl(this.onKey, this.child);

  /// Source: sart-tv/src/main/scala/sart/tv/Remote.scala:22
  @override
  Widget build(BuildContext context) {
    return Focus(
      child: child,
      onKeyEvent: (node, event) => handle(event),
      canRequestFocus: false,
    );
  }

  /// Source: sart-tv/src/main/scala/sart/tv/Remote.scala:29
  KeyEventResult handle(Object event) {
    return !((event is KeyDownEvent) || (event is KeyRepeatEvent))
        ? KeyEventResult.ignored
        : (() {
            final decoded = TvKey.fromLogicalKey(
              (event as KeyEvent).logicalKey,
            );
            return (decoded == null)
                ? KeyEventResult.ignored
                : onKey(decoded!)
                ? KeyEventResult.handled
                : KeyEventResult.ignored;
          })();
  }
}

/// Source: sart-tv/src/main/scala/sart/tv/TvKey.scala:16
enum TvKey {
  Up,
  Down,
  Left,
  Right,
  Select,
  Back,
  Home,
  Menu,
  PlayPause,
  Play,
  Pause,
  Stop,
  FastForward,
  Rewind,
  Next,
  Previous,
  ChannelUp,
  ChannelDown,
  Red,
  Green,
  Yellow,
  Blue,
  Digit0,
  Digit1,
  Digit2,
  Digit3,
  Digit4,
  Digit5,
  Digit6,
  Digit7,
  Digit8,
  Digit9;

  String toJson() => switch (this) {
    TvKey.Up => 'TvKey.Up',
    TvKey.Down => 'TvKey.Down',
    TvKey.Left => 'TvKey.Left',
    TvKey.Right => 'TvKey.Right',
    TvKey.Select => 'TvKey.Select',
    TvKey.Back => 'TvKey.Back',
    TvKey.Home => 'TvKey.Home',
    TvKey.Menu => 'TvKey.Menu',
    TvKey.PlayPause => 'TvKey.PlayPause',
    TvKey.Play => 'TvKey.Play',
    TvKey.Pause => 'TvKey.Pause',
    TvKey.Stop => 'TvKey.Stop',
    TvKey.FastForward => 'TvKey.FastForward',
    TvKey.Rewind => 'TvKey.Rewind',
    TvKey.Next => 'TvKey.Next',
    TvKey.Previous => 'TvKey.Previous',
    TvKey.ChannelUp => 'TvKey.ChannelUp',
    TvKey.ChannelDown => 'TvKey.ChannelDown',
    TvKey.Red => 'TvKey.Red',
    TvKey.Green => 'TvKey.Green',
    TvKey.Yellow => 'TvKey.Yellow',
    TvKey.Blue => 'TvKey.Blue',
    TvKey.Digit0 => 'TvKey.Digit0',
    TvKey.Digit1 => 'TvKey.Digit1',
    TvKey.Digit2 => 'TvKey.Digit2',
    TvKey.Digit3 => 'TvKey.Digit3',
    TvKey.Digit4 => 'TvKey.Digit4',
    TvKey.Digit5 => 'TvKey.Digit5',
    TvKey.Digit6 => 'TvKey.Digit6',
    TvKey.Digit7 => 'TvKey.Digit7',
    TvKey.Digit8 => 'TvKey.Digit8',
    TvKey.Digit9 => 'TvKey.Digit9',
  };

  static TvKey fromJson(dynamic json) {
    final String s = (json as String).toLowerCase();
    for (final v in values) {
      if (v.toJson().toLowerCase() == s) return v;
    }
    throw Exception('Unsupported TvKey: ' + s);
  }

  /// Source: sart-tv/src/main/scala/sart/tv/TvKey.scala:32
  static TvKey? fromLogicalKey(LogicalKeyboardKey key) {
    return byId[key.keyId];
  }

  static late final Map<int, TvKey> byId = {
    LogicalKeyboardKey.arrowUp.keyId: Up,
    LogicalKeyboardKey.arrowDown.keyId: Down,
    LogicalKeyboardKey.arrowLeft.keyId: Left,
    LogicalKeyboardKey.arrowRight.keyId: Right,
    LogicalKeyboardKey.select.keyId: Select,
    LogicalKeyboardKey.enter.keyId: Select,
    LogicalKeyboardKey.numpadEnter.keyId: Select,
    LogicalKeyboardKey.gameButtonA.keyId: Select,
    LogicalKeyboardKey.space.keyId: Select,
    LogicalKeyboardKey.goBack.keyId: Back,
    LogicalKeyboardKey.escape.keyId: Back,
    LogicalKeyboardKey.browserBack.keyId: Back,
    LogicalKeyboardKey.contextMenu.keyId: Menu,
    LogicalKeyboardKey.home.keyId: Home,
    LogicalKeyboardKey.mediaPlayPause.keyId: PlayPause,
    LogicalKeyboardKey.mediaPlay.keyId: Play,
    LogicalKeyboardKey.mediaPause.keyId: Pause,
    LogicalKeyboardKey.mediaStop.keyId: Stop,
    LogicalKeyboardKey.mediaFastForward.keyId: FastForward,
    LogicalKeyboardKey.mediaRewind.keyId: Rewind,
    LogicalKeyboardKey.mediaTrackNext.keyId: Next,
    LogicalKeyboardKey.mediaTrackPrevious.keyId: Previous,
    LogicalKeyboardKey.channelUp.keyId: ChannelUp,
    LogicalKeyboardKey.channelDown.keyId: ChannelDown,
    LogicalKeyboardKey.colorF0Red.keyId: Red,
    LogicalKeyboardKey.colorF1Green.keyId: Green,
    LogicalKeyboardKey.colorF2Yellow.keyId: Yellow,
    LogicalKeyboardKey.colorF3Blue.keyId: Blue,
    LogicalKeyboardKey.digit0.keyId: Digit0,
    LogicalKeyboardKey.digit1.keyId: Digit1,
    LogicalKeyboardKey.digit2.keyId: Digit2,
    LogicalKeyboardKey.digit3.keyId: Digit3,
    LogicalKeyboardKey.digit4.keyId: Digit4,
    LogicalKeyboardKey.digit5.keyId: Digit5,
    LogicalKeyboardKey.digit6.keyId: Digit6,
    LogicalKeyboardKey.digit7.keyId: Digit7,
    LogicalKeyboardKey.digit8.keyId: Digit8,
    LogicalKeyboardKey.digit9.keyId: Digit9,
  };
}

/// Source: sart-tv/src/main/scala/sart/tv/Lifecycle.scala:20
class TvLifecycle {
  TvLifecycle._();

  /// Source: sart-tv/src/main/scala/sart/tv/Lifecycle.scala:21
  static void noop() {}

  /// Source: sart-tv/src/main/scala/sart/tv/Lifecycle.scala:26
  static AppLifecycleListener apply(
    void Function() onResume,
    void Function() onPause,
    void Function() onHide,
    void Function() onInactive,
    void Function() onDetach,
  ) {
    return AppLifecycleListener(
      onResume: onResume,
      onPause: onPause,
      onInactive: onInactive,
      onHide: onHide,
      onDetach: onDetach,
    );
  }

  /// Source: sart-tv/src/main/scala/sart/tv/Lifecycle.scala:27
  static void Function() get apply$default$1 {
    return () => noop();
  }

  /// Source: sart-tv/src/main/scala/sart/tv/Lifecycle.scala:28
  static void Function() get apply$default$2 {
    return () => noop();
  }

  /// Source: sart-tv/src/main/scala/sart/tv/Lifecycle.scala:29
  static void Function() get apply$default$3 {
    return () => noop();
  }

  /// Source: sart-tv/src/main/scala/sart/tv/Lifecycle.scala:30
  static void Function() get apply$default$4 {
    return () => noop();
  }

  /// Source: sart-tv/src/main/scala/sart/tv/Lifecycle.scala:31
  static void Function() get apply$default$5 {
    return () => noop();
  }
}

/// Source: sart-tv/src/main/scala/sart/tv/Platform.scala:16
enum TvPlatform {
  AppleTV,
  Tizen,
  WebOS,
  AndroidTV,
  Other;

  String toJson() => switch (this) {
    TvPlatform.AppleTV => 'TvPlatform.AppleTV',
    TvPlatform.Tizen => 'TvPlatform.Tizen',
    TvPlatform.WebOS => 'TvPlatform.WebOS',
    TvPlatform.AndroidTV => 'TvPlatform.AndroidTV',
    TvPlatform.Other => 'TvPlatform.Other',
  };

  static TvPlatform fromJson(dynamic json) {
    final String s = (json as String).toLowerCase();
    for (final v in values) {
      if (v.toJson().toLowerCase() == s) return v;
    }
    throw Exception('Unsupported TvPlatform: ' + s);
  }

  static final String configured = const String.fromEnvironment(
    'SART_TV_PLATFORM',
  );

  /// Source: sart-tv/src/main/scala/sart/tv/Platform.scala:25
  static TvPlatform get current {
    return fromName(configured);
  }

  /// Source: sart-tv/src/main/scala/sart/tv/Platform.scala:28
  static bool get isTv {
    return current != Other;
  }

  /// Source: sart-tv/src/main/scala/sart/tv/Platform.scala:32
  static TvPlatform fromName(String name) {
    return (name == 'appletv') || (name == 'tvos')
        ? AppleTV
        : (name == 'tizen') || (name == 'samsung')
        ? Tizen
        : (name == 'webos') || (name == 'lg')
        ? WebOS
        : (name == 'androidtv') || (name == 'android-tv')
        ? AndroidTV
        : Other;
  }
}

/// Source: sart-tv/src/main/scala/sart/tv/media/MediaSession.scala:11
class MediaCallbacks {
  final void Function()? onPlay;
  final void Function()? onPause;
  final void Function()? onStop;
  final void Function(Duration)? onSeek;
  final void Function()? onSkipToNext;
  final void Function()? onSkipToPrevious;
  final void Function()? onFastForward;
  final void Function()? onRewind;
  MediaCallbacks({
    this.onPlay = null,
    this.onPause = null,
    this.onStop = null,
    this.onSeek = null,
    this.onSkipToNext = null,
    this.onSkipToPrevious = null,
    this.onFastForward = null,
    this.onRewind = null,
  });

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is MediaCallbacks &&
          other.onPlay == onPlay &&
          other.onPause == onPause &&
          other.onStop == onStop &&
          other.onSeek == onSeek &&
          other.onSkipToNext == onSkipToNext &&
          other.onSkipToPrevious == onSkipToPrevious &&
          other.onFastForward == onFastForward &&
          other.onRewind == onRewind;

  @override
  int get hashCode => Object.hash(
    onPlay,
    onPause,
    onStop,
    onSeek,
    onSkipToNext,
    onSkipToPrevious,
    onFastForward,
    onRewind,
  );

  @override
  String toString() =>
      'MediaCallbacks(onPlay: $onPlay, onPause: $onPause, onStop: $onStop, onSeek: $onSeek, onSkipToNext: $onSkipToNext, onSkipToPrevious: $onSkipToPrevious, onFastForward: $onFastForward, onRewind: $onRewind)';

  MediaCallbacks copyWith({
    void Function()? onPlay,
    void Function()? onPause,
    void Function()? onStop,
    void Function(Duration)? onSeek,
    void Function()? onSkipToNext,
    void Function()? onSkipToPrevious,
    void Function()? onFastForward,
    void Function()? onRewind,
  }) => MediaCallbacks(
    onPlay: onPlay ?? this.onPlay,
    onPause: onPause ?? this.onPause,
    onStop: onStop ?? this.onStop,
    onSeek: onSeek ?? this.onSeek,
    onSkipToNext: onSkipToNext ?? this.onSkipToNext,
    onSkipToPrevious: onSkipToPrevious ?? this.onSkipToPrevious,
    onFastForward: onFastForward ?? this.onFastForward,
    onRewind: onRewind ?? this.onRewind,
  );
}

/// Source: sart-tv/src/main/scala/sart/tv/media/MediaSession.scala:132
class MediaSession {
  MediaSession._();

  static late SartAudioHandler handler;
  static bool started = false;

  /// Source: sart-tv/src/main/scala/sart/tv/media/MediaSession.scala:144
  static Future<SartAudioHandler> init(
    MediaCallbacks callbacks,
    AudioServiceConfig config,
  ) async {
    return started
        ? (() {
            handler.setCallbacks(callbacks);
            return Future.value(handler);
          })()
        : (await (() async {
            final h = (await AudioService.init(
              builder: () => SartAudioHandler(callbacks),
              config: config,
            ));
            handler = h;
            started = true;
            return Future.value(h);
          })());
  }

  /// Source: sart-tv/src/main/scala/sart/tv/media/MediaSession.scala:146
  static AudioServiceConfig get init$default$2 {
    return AudioServiceConfig();
  }
}

/// Source: sart-tv/src/main/scala/sart/tv/media/MediaSession.scala:27
class SartAudioHandler extends BaseAudioHandler {
  MediaCallbacks callbacks;
  SartAudioHandler(this.callbacks);

  bool playing = false;
  Duration position = Duration();

  /// Source: sart-tv/src/main/scala/sart/tv/media/MediaSession.scala:33
  void setCallbacks(MediaCallbacks cb) {
    callbacks = cb;
  }

  /// Source: sart-tv/src/main/scala/sart/tv/media/MediaSession.scala:37
  @override
  Future<void> play() {
    final cb = callbacks.onPlay;
    if (cb != null) {
      cb();
    }
    playing = true;
    broadcast();
    return Future.value(null);
  }

  /// Source: sart-tv/src/main/scala/sart/tv/media/MediaSession.scala:44
  @override
  Future<void> pause() {
    final cb = callbacks.onPause;
    if (cb != null) {
      cb();
    }
    playing = false;
    broadcast();
    return Future.value(null);
  }

  /// Source: sart-tv/src/main/scala/sart/tv/media/MediaSession.scala:51
  @override
  Future<void> stop() {
    final cb = callbacks.onStop;
    if (cb != null) {
      cb();
    }
    playing = false;
    broadcast();
    return Future.value(null);
  }

  /// Source: sart-tv/src/main/scala/sart/tv/media/MediaSession.scala:58
  @override
  Future<void> seek(Duration pos) {
    final cb = callbacks.onSeek;
    if (cb != null) {
      cb(pos);
    }
    position = pos;
    broadcast();
    return Future.value(null);
  }

  /// Source: sart-tv/src/main/scala/sart/tv/media/MediaSession.scala:65
  @override
  Future<void> skipToNext() {
    final cb = callbacks.onSkipToNext;
    if (cb != null) {
      cb();
    }
    return Future.value(null);
  }

  /// Source: sart-tv/src/main/scala/sart/tv/media/MediaSession.scala:70
  @override
  Future<void> skipToPrevious() {
    final cb = callbacks.onSkipToPrevious;
    if (cb != null) {
      cb();
    }
    return Future.value(null);
  }

  /// Source: sart-tv/src/main/scala/sart/tv/media/MediaSession.scala:75
  @override
  Future<void> fastForward() {
    final cb = callbacks.onFastForward;
    if (cb != null) {
      cb();
    }
    return Future.value(null);
  }

  /// Source: sart-tv/src/main/scala/sart/tv/media/MediaSession.scala:80
  @override
  Future<void> rewind() {
    final cb = callbacks.onRewind;
    if (cb != null) {
      cb();
    }
    return Future.value(null);
  }

  /// Source: sart-tv/src/main/scala/sart/tv/media/MediaSession.scala:86
  void setNowPlaying(MediaItem item) {
    mediaItem.add(item);
  }

  /// Source: sart-tv/src/main/scala/sart/tv/media/MediaSession.scala:89
  void setPlaying(bool isPlaying) {
    playing = isPlaying;
    broadcast();
  }

  /// Source: sart-tv/src/main/scala/sart/tv/media/MediaSession.scala:94
  void setPosition(Duration pos) {
    position = pos;
    broadcast();
  }

  /// Source: sart-tv/src/main/scala/sart/tv/media/MediaSession.scala:98
  void broadcast() {
    playbackState.add(
      PlaybackState(
        processingState: AudioProcessingState.ready,
        playing: playing,
        controls: [
          MediaControl.rewind,
          playing ? MediaControl.pause : MediaControl.play,
          MediaControl.stop,
          MediaControl.fastForward,
        ],
        systemActions: ({MediaAction.seek}),
        updatePosition: position,
      ),
    );
  }
}
