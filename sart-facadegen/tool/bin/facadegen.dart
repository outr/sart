// Sart facade-generator Dart helper.
//
// Given a path to a Dart source file on the command line, parse it via
// `package:analyzer` and dump a JSON description of the public API to
// stdout. The Scala CLI then reads the JSON and emits Scala facades.
//
// JSON shape (MVP):
// {
//   "path": "<input path>",
//   "libraryName": "<library directive or filename>",
//   "classes": [
//     {
//       "name": "Greeter",
//       "typeParams": ["T"],
//       "extends": "SomeBase",           // or null
//       "implements": ["Comparable"],
//       "fields":  [{"name": "x", "type": "int", "final": true}],
//       "methods": [{"name": "greet", "returnType": "String", "params": [...]}]
//     }
//   ],
//   "functions": [
//     {"name": "add", "returnType": "int", "params": [...]}
//   ]
// }

import 'dart:convert';
import 'dart:io';

import 'package:analyzer/dart/analysis/analysis_context_collection.dart';
import 'package:analyzer/dart/analysis/results.dart';
import 'package:analyzer/dart/ast/ast.dart';
import 'package:analyzer/dart/ast/visitor.dart';

Future<void> main(List<String> args) async {
  if (args.isEmpty) {
    stderr.writeln('usage: dart run bin/facadegen.dart <path.dart>');
    exit(2);
  }
  final path = File(args.first).absolute.path;
  final collection = AnalysisContextCollection(includedPaths: [path]);
  final context = collection.contextFor(path);
  final result = context.currentSession.getParsedUnit(path);
  if (result is! ParsedUnitResult) {
    stderr.writeln('Failed to parse $path: $result');
    exit(1);
  }

  final visitor = _ApiVisitor();
  result.unit.accept(visitor);

  final out = <String, Object?>{
    'path': path,
    'libraryName': _libraryName(result.unit, path),
    'classes': visitor.classes,
    'enums': visitor.enums,
    'functions': visitor.functions,
  };
  stdout.writeln(const JsonEncoder.withIndent('  ').convert(out));
}

String _libraryName(CompilationUnit unit, String path) {
  for (final directive in unit.directives) {
    if (directive is LibraryDirective) {
      return directive.name2?.toSource() ?? '';
    }
  }
  return path.split('/').last.replaceAll('.dart', '');
}

class _ApiVisitor extends RecursiveAstVisitor<void> {
  final List<Map<String, Object?>> classes = [];
  final List<Map<String, Object?>> enums = [];
  final List<Map<String, Object?>> functions = [];

  @override
  void visitEnumDeclaration(EnumDeclaration node) {
    if (node.name.lexeme.startsWith('_')) return;
    enums.add({
      'name': node.name.lexeme,
      'constants': node.constants
        .where((c) => !c.name.lexeme.startsWith('_'))
        .map((c) => c.name.lexeme)
        .toList(),
    });
  }

  @override
  void visitClassDeclaration(ClassDeclaration node) {
    if (node.name.lexeme.startsWith('_')) return; // skip private
    final fields = <Map<String, Object?>>[];
    final methods = <Map<String, Object?>>[];

    for (final member in node.members) {
      if (member is FieldDeclaration) {
        final isFinal = member.fields.isFinal;
        for (final v in member.fields.variables) {
          if (v.name.lexeme.startsWith('_')) continue;
          fields.add({
            'name': v.name.lexeme,
            'type': member.fields.type?.toSource() ?? 'dynamic',
            'final': isFinal,
          });
        }
      } else if (member is MethodDeclaration) {
        if (member.name.lexeme.startsWith('_')) continue;
        methods.add({
          'name': member.name.lexeme,
          'returnType': member.returnType?.toSource() ?? 'dynamic',
          'params': _paramList(member.parameters),
          'isStatic': member.isStatic,
          'isGetter': member.isGetter,
        });
      }
    }

    classes.add({
      'name': node.name.lexeme,
      'typeParams': node.typeParameters?.typeParameters.map((tp) => tp.name.lexeme).toList() ?? <String>[],
      'extends': node.extendsClause?.superclass.toSource(),
      'implements': node.implementsClause?.interfaces.map((i) => i.toSource()).toList() ?? <String>[],
      'fields': fields,
      'methods': methods,
    });
  }

  @override
  void visitFunctionDeclaration(FunctionDeclaration node) {
    if (node.name.lexeme.startsWith('_')) return;
    functions.add({
      'name': node.name.lexeme,
      'returnType': node.returnType?.toSource() ?? 'dynamic',
      'params': _paramList(node.functionExpression.parameters),
    });
  }

  List<Map<String, Object?>> _paramList(FormalParameterList? params) {
    if (params == null) return const [];
    final out = <Map<String, Object?>>[];
    for (final p in params.parameters) {
      final normal = p is DefaultFormalParameter ? p.parameter : p;
      if (normal is SimpleFormalParameter) {
        out.add({
          'name': normal.name?.lexeme ?? '_',
          'type': normal.type?.toSource() ?? 'dynamic',
          'named': p.isNamed,
          'required': p.isRequired || (p is DefaultFormalParameter && p.parameter.isRequired),
        });
      } else if (normal is FieldFormalParameter) {
        out.add({
          'name': normal.name.lexeme,
          'type': normal.type?.toSource() ?? 'dynamic',
          'named': p.isNamed,
          'required': p.isRequired,
        });
      }
    }
    return out;
  }
}
