// Sart facade-generator Dart helper (v2 — resolved analysis).
//
// Given one or more Dart source files on the command line, resolve them
// via `package:analyzer` and dump a JSON description of their public API
// to stdout. Resolution (rather than parsing) is what lets us see:
//   - constructor parameters declared as `super.x` with their real types
//   - typedef-expanded function types (`VoidCallback` → `void Function()`)
//   - default-value presence and `required`-ness
//   - the full ancestor chain of every class and referenced type, so the
//     Scala side can collapse unknown types to the nearest facaded one.
//
// JSON shape (v2):
// {
//   "files": [
//     { "path": "...",
//       "classes": [
//         { "name": "TextButton", "abstract": false,
//           "typeParams": ["T"],
//           "ancestors": ["ButtonStyleButton", "StatefulWidget", ...],
//           "constructors": [
//             { "name": "",            // "" = unnamed; "icon" = named
//               "const": true, "factory": false,
//               "params": [ {"name": "child", "type": "Widget",
//                            "named": true, "required": true,
//                            "hasDefault": false} ] } ],
//           "staticFields": [ {"name": "red", "type": "MaterialColor"} ],
//           "staticMethods": [ ...method... ],
//           "instanceGetters": [ {"name": "text", "type": "String",
//                                 "overrides": false} ],
//           "instanceMethods": [
//             { "name": "clear", "returnType": "void", "typeParams": [],
//               "overrides": false, "params": [...] } ] } ],
//       "enums": [ {"name": "MainAxisAlignment",
//                   "constants": ["start", "center", ...]} ] } ],
//   "typeAncestors": { "MaterialColor": ["ColorSwatch", "Color", ...] }
// }

import 'dart:convert';
import 'dart:io';

import 'package:analyzer/dart/analysis/analysis_context_collection.dart';
import 'package:analyzer/dart/analysis/results.dart';
import 'package:analyzer/dart/element/element2.dart';
import 'package:analyzer/dart/element/type.dart';

final Map<String, List<String>> typeAncestors = {};

Future<void> main(List<String> args) async {
  if (args.isEmpty) {
    stderr.writeln(
        'usage: dart run bin/facadegen.dart <path.dart> [more...]\n'
        '   or: dart run bin/facadegen.dart --context <dir> <library-uri>...');
    exit(2);
  }

  final files = <Map<String, Object?>>[];

  if (args.first == '--context') {
    // URI mode: resolve libraries through an existing Dart/Flutter
    // project's analysis context, so package: imports (flutter, other
    // pub deps) resolve with the project's full package graph. Element
    // model only — facades never need statement bodies.
    final contextDir = Directory(args[1]).absolute.path;
    final uris = args.sublist(2);
    final collection = AnalysisContextCollection(includedPaths: [contextDir]);
    final session = collection.contextFor(contextDir).currentSession;
    for (final uri in uris) {
      final result = await session.getLibraryByUri(uri);
      if (result is! LibraryElementResult) {
        stderr.writeln('Failed to resolve library $uri: $result');
        exit(1);
      }
      files.add(_describeLibrary(uri, result.element2));
    }
  } else {
    final paths = args.map((a) => File(a).absolute.path).toList();
    final collection = AnalysisContextCollection(includedPaths: paths);
    for (final path in paths) {
      final context = collection.contextFor(path);
      final result = await context.currentSession.getResolvedUnit(path);
      if (result is! ResolvedUnitResult) {
        stderr.writeln('Failed to resolve $path: $result');
        exit(1);
      }
      files.add(_describeFile(path, result));
    }
  }

  final out = <String, Object?>{
    'files': files,
    'typeAncestors': typeAncestors,
  };
  stdout.writeln(const JsonEncoder.withIndent(' ').convert(out));
}

Map<String, Object?> _describeLibrary(String key, LibraryElement2 lib) {
  final classes = <Map<String, Object?>>[];
  final enums = <Map<String, Object?>>[];
  for (final cls in lib.classes) {
    final name = cls.name3;
    if (name == null || name.startsWith('_')) continue;
    classes.add(_describeClass(cls));
  }
  for (final ext in lib.extensions) {
    final desc = _describeExtensionStatics(ext);
    if (desc != null) classes.add(desc);
  }
  for (final en in lib.enums) {
    final name = en.name3;
    if (name == null || name.startsWith('_')) continue;
    enums.add({
      'name': name,
      'constants': en.fields2
          .where((f) => f.isEnumConstant && !(f.name3 ?? '_').startsWith('_'))
          .map((f) => f.name3)
          .toList(),
    });
  }
  return {'path': key, 'classes': classes, 'enums': enums};
}

Map<String, Object?> _describeFile(String path, ResolvedUnitResult result) {
  final lib = result.libraryElement2;
  final classes = <Map<String, Object?>>[];
  final enums = <Map<String, Object?>>[];

  for (final cls in lib.classes) {
    final name = cls.name3;
    if (name == null || name.startsWith('_')) continue;
    classes.add(_describeClass(cls));
  }
  for (final ext in lib.extensions) {
    final desc = _describeExtensionStatics(ext);
    if (desc != null) classes.add(desc);
  }
  for (final en in lib.enums) {
    final name = en.name3;
    if (name == null || name.startsWith('_')) continue;
    enums.add({
      'name': name,
      'constants': en.fields2
          .where((f) => f.isEnumConstant && !(f.name3 ?? '_').startsWith('_'))
          .map((f) => f.name3)
          .toList(),
    });
  }
  return {'path': path, 'classes': classes, 'enums': enums};
}

/// A Dart `extension Foo on T` can carry static members that read like
/// statics of a class named `Foo` (e.g. flex_color_scheme's
/// `FlexThemeData.dark(...)`). Instances never exist, so only the statics
/// are described; returns null when the extension is unnamed or has none.
Map<String, Object?>? _describeExtensionStatics(ExtensionElement2 ext) {
  final name = ext.name3;
  if (name == null || name.startsWith('_')) return null;
  final staticFields = <Map<String, Object?>>[];
  for (final f in ext.fields2) {
    final fname = f.name3;
    if (fname == null || fname.startsWith('_') || !f.isStatic) continue;
    staticFields.add({'name': fname, 'type': _typeStr(f.type)});
  }
  final staticMethods = <Map<String, Object?>>[];
  for (final m in ext.methods2) {
    final mname = m.name3;
    if (mname == null || mname.startsWith('_') || !m.isStatic) continue;
    if (m.isOperator) continue;
    staticMethods.add({
      'name': mname,
      'returnType': _typeStr(m.returnType),
      'typeParams':
          m.typeParameters2.map((tp) => tp.name3).whereType<String>().toList(),
      'overrides': false,
      'params': m.formalParameters.map(_describeParam).toList(),
    });
  }
  if (staticFields.isEmpty && staticMethods.isEmpty) return null;
  return {
    'name': name,
    'abstract': false,
    'extensionStatics': true,
    'typeParams': const <String>[],
    'ancestors': const <String>[],
    'constructors': const <Map<String, Object?>>[],
    'staticFields': staticFields,
    'staticMethods': staticMethods,
    'instanceGetters': const <Map<String, Object?>>[],
    'instanceMethods': const <Map<String, Object?>>[],
    'inheritedGetters': const <Map<String, Object?>>[],
    'inheritedMethods': const <Map<String, Object?>>[],
  };
}

Map<String, Object?> _describeClass(ClassElement2 cls) {
  final ancestors = <String>[];
  for (final t in cls.allSupertypes) {
    final n = t.element3.name3;
    if (n != null && !n.startsWith('_')) ancestors.add(n);
  }

  bool superHas(String memberName) => cls.allSupertypes.any((t) =>
      t.getMethod2(memberName) != null || t.getGetter2(memberName) != null);

  final constructors = <Map<String, Object?>>[];
  for (final ctor in cls.constructors2) {
    final cname = ctor.name3 ?? 'new';
    if (cname.startsWith('_')) continue;
    constructors.add({
      'name': cname == 'new' ? '' : cname,
      'const': ctor.isConst,
      'factory': ctor.isFactory,
      'params': ctor.formalParameters.map(_describeParam).toList(),
    });
  }

  final staticFields = <Map<String, Object?>>[];
  final instanceGetters = <Map<String, Object?>>[];
  for (final f in cls.fields2) {
    final fname = f.name3;
    if (fname == null || fname.startsWith('_')) continue;
    if (f.isStatic) {
      staticFields.add({'name': fname, 'type': _typeStr(f.type)});
    } else {
      instanceGetters.add({
        'name': fname,
        'type': _typeStr(f.type),
        'overrides': superHas(fname),
      });
    }
  }

  final staticMethods = <Map<String, Object?>>[];
  final instanceMethods = <Map<String, Object?>>[];
  for (final m in cls.methods2) {
    final mname = m.name3;
    if (mname == null || mname.startsWith('_')) continue;
    if (m.isOperator) continue;
    final desc = {
      'name': mname,
      'returnType': _typeStr(m.returnType),
      'typeParams':
          m.typeParameters2.map((tp) => tp.name3).whereType<String>().toList(),
      'overrides': !m.isStatic && superHas(mname),
      'params': m.formalParameters.map(_describeParam).toList(),
    };
    (m.isStatic ? staticMethods : instanceMethods).add(desc);
  }

  // Inherited public API, tagged with its declaring ancestor. The Scala
  // side flattens these into subclassable facades when the ancestor
  // itself has no facade (e.g. ChangeNotifier.notifyListeners on
  // DataGridSource) — otherwise a Scala subclass couldn't reach them.
  const skipInherited = {
    'toString', 'noSuchMethod', 'hashCode', 'runtimeType',
    'toStringShort', 'toStringDeep', 'toDiagnosticsNode',
    'debugFillProperties', 'debugDescribeChildren',
  };
  final inheritedGetters = <Map<String, Object?>>[];
  final inheritedMethods = <Map<String, Object?>>[];
  final seenInherited = <String>{};
  for (final t in cls.allSupertypes) {
    final ancestorName = t.element3.name3;
    if (ancestorName == null || ancestorName == 'Object') continue;
    for (final m in t.methods2) {
      final mname = m.name3;
      if (mname == null || mname.startsWith('_') || m.isStatic) continue;
      if (m.isOperator || skipInherited.contains(mname)) continue;
      if (!seenInherited.add(mname)) continue;
      inheritedMethods.add({
        'from': ancestorName,
        'name': mname,
        'returnType': _typeStr(m.returnType),
        'typeParams': m.typeParameters2
            .map((tp) => tp.name3)
            .whereType<String>()
            .toList(),
        'overrides': false,
        'params': m.formalParameters.map(_describeParam).toList(),
      });
    }
    for (final g in t.getters) {
      final gname = g.name3;
      if (gname == null || gname.startsWith('_') || g.isStatic) continue;
      if (skipInherited.contains(gname)) continue;
      if (!seenInherited.add(gname)) continue;
      inheritedGetters.add({
        'from': ancestorName,
        'name': gname,
        'type': _typeStr(g.returnType),
        'overrides': false,
      });
    }
  }

  return {
    'name': cls.name3,
    'abstract': cls.isAbstract,
    'typeParams':
        cls.typeParameters2.map((tp) => tp.name3).whereType<String>().toList(),
    'ancestors': ancestors,
    'constructors': constructors,
    'staticFields': staticFields,
    'staticMethods': staticMethods,
    'instanceGetters': instanceGetters,
    'instanceMethods': instanceMethods,
    'inheritedGetters': inheritedGetters,
    'inheritedMethods': inheritedMethods,
  };
}

Map<String, Object?> _describeParam(FormalParameterElement p) => {
      'name': p.name3 ?? '_',
      'type': _typeStr(p.type),
      'named': p.isNamed,
      'required': p.isRequired,
      'hasDefault': p.hasDefaultValue,
    };

/// Render a type's display string while recording the ancestor chains of
/// every interface type it mentions (so the Scala side can collapse types
/// it has no facade for onto the nearest facaded supertype).
String _typeStr(DartType t) {
  _recordAncestors(t, 0);
  return t.getDisplayString();
}

void _recordAncestors(DartType t, int depth) {
  if (depth > 6) return;
  if (t is InterfaceType) {
    final name = t.element3.name3;
    if (name != null && !name.startsWith('_') &&
        !typeAncestors.containsKey(name)) {
      typeAncestors[name] = t.element3.allSupertypes
          .map((s) => s.element3.name3)
          .whereType<String>()
          .where((n) => !n.startsWith('_'))
          .toList();
    }
    for (final arg in t.typeArguments) {
      _recordAncestors(arg, depth + 1);
    }
  } else if (t is FunctionType) {
    _recordAncestors(t.returnType, depth + 1);
    for (final p in t.formalParameters) {
      _recordAncestors(p.type, depth + 1);
    }
  }
}
