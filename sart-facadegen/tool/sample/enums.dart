// Small fixture with a Dart enum.
library enums_sample;

enum Priority { low, medium, high, urgent }

enum Size { small, medium, large }

class Task {
  final String title;
  final Priority priority;
  final Size size;
  Task(this.title, this.priority, this.size);
}
