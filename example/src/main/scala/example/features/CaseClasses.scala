package example.features

// Feature fixture: case classes. Expected Dart output is a class with
// synthesized `==`, `hashCode`, `toString`, `copyWith`.

case class Point(x: Int, y: Int)

case class Todo(id: Int, text: String, done: Boolean)

case class User(name: String, email: String, age: Int)
