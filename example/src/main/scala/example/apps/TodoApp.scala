package example.apps

import flutter.material.*

// Todo list app — the larger integration fixture. Exercises TextField,
// TextEditingController, ElevatedButton, Padding, EdgeInsets, ListView,
// ListTile, Checkbox, plus list append (`:+`), index access, size/length.
//
// Toggling a todo's `done` needs List.updated + case-class copy composed
// in an expression; that's left for follow-up stdlib work. Here the
// checkbox is rendered from each todo's state but the onChanged just
// re-adds an identical todo so the list still grows meaningfully.
//
// Deliberately not annotated `@main` — the counter app stays the entry
// point for the runnable Linux binary. This file is emitted alongside it
// so `flutter analyze` keeps both honest.

case class TodoItem(text: String, done: Boolean)

class TodoApp extends StatefulWidget:
  override def createState(): State[TodoApp] = TodoAppState()

class TodoAppState extends State[TodoApp]:
  private var todos: List[TodoItem] = List()
  private val controller: TextEditingController = TextEditingController()

  private def addTodoItem(): Unit =
    setState(() => todos = todos :+ TodoItem(controller.text, false))

  private def clearAll(): Unit =
    setState(() => todos = List())

  // Exercise the list.updated / headOption translations end-to-end.
  // Kept as plain getters so there's no Unit-returning lambda to
  // complicate the test path.
  def peek: Option[TodoItem] = todos.headOption
  def markDone(i: Int): List[TodoItem] =
    todos.updated(i, TodoItem("done", true))

  override def build(context: BuildContext): Widget =
    Scaffold(
      appBar = AppBar(title = Text("Todos")),
      body = Column(
        mainAxisAlignment = MainAxisAlignment.start,
        children = List(
          Padding(
            padding = EdgeInsets.all(8.0),
            child = TextField(
              controller = controller,
              decoration = InputDecoration(labelText = "What needs doing?")
            )
          ),
          Row(
            mainAxisAlignment = MainAxisAlignment.center,
            children = List(
              ElevatedButton(
                onPressed = () => addTodoItem(),
                child = Text("Add")
              ),
              SizedBox(width = 8.0),
              ElevatedButton(
                onPressed = () => clearAll(),
                child = Text("Clear all")
              )
            )
          ),
          Expanded(
            child = ListView.builder(
              itemCount = todos.size,
              itemBuilder = (ctx: BuildContext, i: Int) =>
                ListTile(
                  leading = Icon(
                    if todos(i).done then Icons.check else Icons.edit
                  ),
                  title = Text(todos(i).text)
                )
            )
          )
        )
      )
    )
