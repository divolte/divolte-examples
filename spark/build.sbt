import AssemblyKeys._
import spray.revolver.RevolverPlugin.Revolver

organization  := "io.divolte"

name          := "spark-example"

version       := "0.1"

scalaVersion  := "2.10.4"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8", "-target:jvm-1.7", "-feature")

// Experimental: improved incremental compilation.
incOptions    := incOptions.value.withNameHashing(nameHashing = true)

// Provided: the Spark container supplies its own version.
libraryDependencies += "org.apache.spark"  %% "spark-core"    % "1.1.0" % "provided"

libraryDependencies += "io.divolte"        %% "divolte-spark" % "0.1-SNAPSHOT"

// Necessary to prevent Avro/Hadoop version conflicts.
libraryDependencies += "org.apache.hadoop" %  "hadoop-client" % "2.3.0" % "provided"

Revolver.settings

assemblySettings

net.virtualvoid.sbt.graph.Plugin.graphSettings

// If running locally, ensure that "provided" dependencies are on the classpath.
run in Compile <<= Defaults.runTask(fullClasspath in Compile, mainClass in (Compile, run), runner in (Compile, run))

fullClasspath in Revolver.reStart ++= (fullClasspath in Compile).value

// Run things in a forked JVM, so we can set the options below.
fork in run := true

// Use a local Spark master when running from within SBT.
val localSparkOptions = Seq(
  "-Dspark.master=local[*]",
  "-Dspark.app.name=Spark Divolte"
)

javaOptions in run ++= localSparkOptions

javaOptions in Revolver.reStart ++= localSparkOptions
