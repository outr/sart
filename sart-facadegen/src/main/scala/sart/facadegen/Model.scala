package sart.facadegen

// Model for the JSON the Dart helper emits. Kept as plain case classes so
// we don't pull in a dependency just to parse a small JSON blob.

case class Param(name: String, tpe: String, named: Boolean, required: Boolean)

case class Method(
  name: String,
  returnType: String,
  params: List[Param],
  isStatic: Boolean,
  isGetter: Boolean
)

case class Field(name: String, tpe: String, isFinal: Boolean)

case class ClassInfo(
  name: String,
  typeParams: List[String],
  extendsClause: Option[String],
  implementsClauses: List[String],
  fields: List[Field],
  methods: List[Method]
)

case class FunctionInfo(
  name: String,
  returnType: String,
  params: List[Param]
)

case class EnumInfo(name: String, constants: List[String])

case class LibraryInfo(
  path: String,
  libraryName: String,
  classes: List[ClassInfo],
  enums: List[EnumInfo],
  functions: List[FunctionInfo]
)
