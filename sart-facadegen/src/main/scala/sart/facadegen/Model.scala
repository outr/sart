package sart.facadegen

// Model for the JSON the Dart helper emits (v2 — resolved analysis).
// Kept as plain case classes so we don't pull in a dependency just to
// parse a JSON blob.

case class Param(
  name: String,
  tpe: String,
  named: Boolean,
  required: Boolean,
  hasDefault: Boolean
)

case class Ctor(
  name: String,          // "" for the unnamed constructor
  isConst: Boolean,
  isFactory: Boolean,
  params: List[Param]
)

case class Method(
  name: String,
  returnType: String,
  typeParams: List[String],
  overrides: Boolean,
  params: List[Param]
)

case class Getter(name: String, tpe: String, overrides: Boolean)

case class StaticField(name: String, tpe: String)

case class ClassInfo(
  name: String,
  isAbstract: Boolean,
  typeParams: List[String],
  ancestors: List[String],       // nearest-first chain of supertypes
  ctors: List[Ctor],
  staticFields: List[StaticField],
  staticMethods: List[Method],
  instanceGetters: List[Getter],
  instanceMethods: List[Method]
)

case class EnumInfo(name: String, constants: List[String])

case class FileInfo(
  path: String,
  classes: List[ClassInfo],
  enums: List[EnumInfo]
)

/** Whole helper invocation: every requested file plus the ancestor chains
 *  of every type referenced anywhere in the emitted API (used to collapse
 *  unknown types onto the nearest facaded supertype).
 */
case class ApiDump(
  files: List[FileInfo],
  typeAncestors: Map[String, List[String]]
)
