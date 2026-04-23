package example.features

// Feature fixture: Scala 3 enums.
//   - Simple enums (no parameters) map to Dart `enum`.
//   - Parameterized/ADT enums map to a Dart sealed hierarchy (phase 2 of this
//     task, after the simple form is working).

enum Filter:
  case All, Active, Completed

enum Priority:
  case Low, Medium, High, Urgent
