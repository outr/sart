package sbt.sart

import sbt.*
import xsbti.{FileConverter, HashedVirtualFileRef}

/** sbt 2.x side of the cross-build: Classpath entries are
 *  HashedVirtualFileRef and need the build's FileConverter to become
 *  concrete Files.
 */
private[sart] object PluginCompat {
  def toFiles(cp: Seq[Attributed[HashedVirtualFileRef]], conv: FileConverter): Seq[File] =
    cp.map(e => conv.toPath(e.data).toFile)

  def managedJarFiles(
    config: Configuration, jarTypes: Set[String], up: UpdateReport, conv: FileConverter
  ): Seq[File] =
    Classpaths.managedJars(config, jarTypes, up, conv).map(e => conv.toPath(e.data).toFile)

  /** Every resolved (module, jar) pair in the compile configuration. */
  def moduleJars(up: UpdateReport, conv: FileConverter): Seq[(ModuleID, File)] =
    up.configuration(ConfigRef("compile")).toSeq
      .flatMap(_.modules)
      .flatMap(m => m.artifacts.map { case (_, f) => (m.module, f) })
}
