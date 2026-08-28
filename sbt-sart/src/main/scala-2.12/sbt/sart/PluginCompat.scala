package sbt.sart

import sbt._
import xsbti.FileConverter

/** sbt 1.x side of the cross-build: Classpath is Seq[Attributed[File]],
 *  so entries are already Files and the converter goes unused.
 */
private[sart] object PluginCompat {
  def toFiles(cp: Seq[Attributed[File]], conv: FileConverter): Seq[File] =
    cp.map(_.data)

  def managedJarFiles(
    config: Configuration, jarTypes: Set[String], up: UpdateReport, conv: FileConverter
  ): Seq[File] =
    Classpaths.managedJars(config, jarTypes, up).map(_.data)

  /** Every resolved (module, jar) pair in the compile configuration. */
  def moduleJars(up: UpdateReport, conv: FileConverter): Seq[(ModuleID, File)] =
    up.configuration(ConfigRef("compile")).toSeq
      .flatMap(_.modules)
      .flatMap(m => m.artifacts.map { case (_, f) => (m.module, f) })
}
