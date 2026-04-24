// Small test fixture for the facade generator. Exercises classes with
// fields/methods/type-params and top-level functions.

library greeter;

class Greeter<T> {
  final String name;
  int greetCount;

  Greeter(this.name, this.greetCount);

  String greet(T subject) {
    greetCount = greetCount + 1;
    return 'Hello, $subject! (from $name)';
  }
}

class Repeater extends Greeter<String> {
  final int times;
  Repeater(String name, this.times) : super(name, 0);

  String shout({required String line}) => line.toUpperCase();
}

int add(int a, int b) => a + b;

String banner({String title = 'default', int width = 40}) =>
    '=' * width + ' $title ' + '=' * width;
