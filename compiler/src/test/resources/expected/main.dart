import 'dart:async';
import 'dart:math';
import 'package:flutter/material.dart';
import 'sart_either.dart';
import 'sart_option.dart';
import 'sart_try.dart';

/// Source: example/src/main/scala/example/LauncherApp.scala:13
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

/// Source: example/src/main/scala/example/LauncherApp.scala:11
void main() {
  runApp(LauncherApp());
}

/// Source: example/src/main/scala/example/LauncherApp.scala:19
class LauncherApp extends StatelessWidget {
  /// Source: example/src/main/scala/example/LauncherApp.scala:20
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Sart Showcase',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
        useMaterial3: true,
      ),
      home: LauncherHome(),
    );
  }
}

/// Source: example/src/main/scala/example/LauncherApp.scala:30
class LauncherHome extends StatelessWidget {
  final List<Demo> demos = [
    Demo('Showcase', 'Kitchen-sink feature demo', (ctx) => ShowcaseApp()),
    Demo('Counter', 'Classic Flutter counter', (ctx) => MyHomePage('Counter')),
    Demo('Todos', 'TextField + list + state', (ctx) => TodoApp()),
    Demo('Dice', 'Random + history + Navigator', (ctx) => DiceApp()),
    Demo('Stopwatch', 'Timer.periodic', (ctx) => ClockApp()),
    Demo('Two-screen', 'Navigator.push demo', (ctx) => HomeScreen()),
  ];

  /// Source: example/src/main/scala/example/LauncherApp.scala:40
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        title: Text('Sart Demos'),
      ),
      body: ListView.builder(
        itemCount: demos.length,
        itemBuilder: (ctx, i) => ListTile(
          leading: Icon(Icons.menu),
          title: Text(demos[i].title),
          subtitle: Text(demos[i].subtitle),
          onTap: () {
            Navigator.of(ctx).push(MaterialPageRoute(builder: demos[i].build));
          },
        ),
      ),
    );
  }
}

/// Source: example/src/main/scala/example/CounterApp.scala:9
class MyApp extends StatelessWidget {
  /// Source: example/src/main/scala/example/CounterApp.scala:10
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
        useMaterial3: true,
      ),
      home: MyHomePage('Flutter Demo Home Page'),
    );
  }
}

/// Source: example/src/main/scala/example/CounterApp.scala:20
class MyHomePage extends StatefulWidget {
  final String title;
  MyHomePage(this.title);

  /// Source: example/src/main/scala/example/CounterApp.scala:21
  @override
  State<MyHomePage> createState() {
    return MyHomePageState();
  }
}

/// Source: example/src/main/scala/example/CounterApp.scala:24
class MyHomePageState extends State<MyHomePage> {
  int counter = 0;

  /// Source: example/src/main/scala/example/CounterApp.scala:27
  void incrementCounter() {
    setState(() {
      counter = counter + 1;
    });
  }

  /// Source: example/src/main/scala/example/CounterApp.scala:30
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        title: Text(widget.title),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text('You have pushed the button this many times:'),
            Text('$counter', style: Theme.of(context).textTheme.headlineMedium),
          ],
        ),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => incrementCounter(),
        tooltip: 'Increment',
        child: Icon(Icons.add),
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
    setState(() {
      timer = Timer.periodic(
        Duration(seconds: 1),
        (t) => setState(() {
          seconds = seconds + 1;
        }),
      );
    });
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
  List<Roll> history = [];

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
      history = [];
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
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        title: Text('Dice'),
      ),
      body: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Center(
            child: Container(
              width: 120.0,
              height: 120.0,
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(16.0),
                boxShadow: [BoxShadow(color: Colors.grey, blurRadius: 8.0)],
              ),
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
        onPressed: () {
          Navigator.of(
            context,
          ).push(MaterialPageRoute(builder: (ctx) => HistoryScreen(history)));
        },
        tooltip: 'History',
        child: Icon(Icons.menu),
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
        itemCount: history.length,
        itemBuilder: (ctx, i) => ListTile(
          leading: Icon(Icons.check),
          title: Text('Roll #' + history[i].index.toString()),
          trailing: Text(history[i].face.toString()),
        ),
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
            ).push(MaterialPageRoute(builder: (ctx) => DetailScreen()));
          },
          child: Text('Go to detail'),
        ),
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
}

/// Source: example/src/main/scala/example/apps/ShowcaseApp.scala:17
sealed class ShapeKind {}

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
      () => lookupResult =
          (contacts.where((c) => c.name == query).toList().isEmpty
                  ? null
                  : contacts.where((c) => c.name == query).toList().first)
              .fold(
                'no match for \'' + query.toString() + '\'',
                (c) => c.name.toString() + ' → ' + c.phone.toString(),
              ),
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
          Text(faceGlyph(face).toString() + '  (' + face.toString() + ')'),
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
    return section(
      'Sealed trait + match',
      Column(
        mainAxisAlignment: MainAxisAlignment.start,
        children: [
          Text(
            shapeLabel(shapes[shapeIdx]).toString() +
                '  area = ' +
                area(shapes[shapeIdx]).toString(),
          ),
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
            !emailTyped
                ? '— type something —'
                : emailValid
                ? '✓ looks like an email'
                : '✗ not a valid email',
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
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        title: Text('Showcase'),
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
  List<TodoItem> todos = [];
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
      todos = [];
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
              itemCount: todos.length,
              itemBuilder: (ctx, i) => ListTile(
                leading: Icon(todos[i].done ? Icons.check : Icons.edit),
                title: Text(todos[i].text),
              ),
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
}

/// Source: example/src/main/scala/example/features/AsyncBuilders.scala:9
class AsyncBuildersExample {
  /// Source: example/src/main/scala/example/features/AsyncBuilders.scala:10
  Widget futureWidget(Future<String> f) {
    return FutureBuilder(
      future: f,
      builder: (ctx, snap) => Text((snap.data ?? 'loading…')),
    );
  }

  /// Source: example/src/main/scala/example/features/AsyncBuilders.scala:17
  Widget streamWidget(Stream<int> s) {
    return StreamBuilder(
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
      width: 200.0,
      height: 120.0,
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
      child: Center(child: Text('Hello, styled world')),
    );
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
    return e.fold(
      (err) => 'error: ' + err.toString(),
      (n) => 'ok: ' + n.toString(),
    );
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
enum Filter { All, Active, Completed }

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
abstract class Formatter<T> {
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
    return 'hello ' + f.format(value).toString();
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
abstract class HasSize {
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
sealed class Json {}

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
}

/// Source: example/src/main/scala/example/features/SealedHierarchy.scala:16
class JsonNull extends Json {}

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
            child: Align(
              alignment: Alignment.bottomLeft,
              child: GestureDetector(onTap: () => null, child: Text('tap me')),
            ),
            left: 16.0,
            bottom: 16.0,
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
    return (o ?? 0);
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
  String typeClassify(Object x) {
    return switch (x) {
      String _ => 'string',
      int _ => 'int',
      _ => 'other',
    };
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
}

/// Source: example/src/main/scala/example/features/Enums.scala:11
enum Priority { Low, Medium, High, Urgent }

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
}

/// Source: example/src/main/scala/example/features/SealedHierarchy.scala:6
sealed class Shape {}

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
}

/// Source: example/src/main/scala/example/features/SealedHierarchy.scala:9
class UnitSquare extends Shape {}

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
}

/// Source: example/src/main/scala/example/features/Generics.scala:15
class Wrapping {
  /// Source: example/src/main/scala/example/features/Generics.scala:16
  Box<T> wrap<T>(T value) {
    return Box(value);
  }
}
