import AssemblyKeys._
import spray.revolver.RevolverPlugin.Revolver

organization  := "io.divolte"

name          := "spark-example"

version       := "0.1"

scalaVersion  := "2.10.4"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8", "-target:jvm-1.7", "-feature")

// Experimental: improved incremental compilation.
incOptions    := incOptions.value.withNameHashing(nameHashing = true)

// Enable during development to access local maven artifacts.
resolvers += Resolver.mavenLocal

val sparkV = "1.1.0"

// Provided: the Spark container supplies its own version.
libraryDependencies += "org.apache.spark"  %% "spark-core"            % sparkV % "provided"

libraryDependencies += "org.apache.spark"  %% "spark-streaming"       % sparkV % "provided"

libraryDependencies += "org.apache.spark"  %% "spark-streaming-kafka" % sparkV excludeAll(
  ExclusionRule(organization = "org.apache.spark", name = "spark-streaming_2.10"),
  ExclusionRule(organization = "javax.jms")
)

libraryDependencies += "io.divolte"        %% "divolte-spark"         % "0.1"

libraryDependencies += "org.apache.kafka"  %% "kafka"                 % "0.8.1.1" excludeAll(
  ExclusionRule(organization = "com.sun.jdmk"),
  ExclusionRule(organization = "com.sun.jmx"),
  ExclusionRule(organization = "javax.jms"),
  ExclusionRule(organization = "log4j")
)

// Necessary to prevent Avro/Hadoop version conflicts.
libraryDependencies += "org.apache.hadoop" %  "hadoop-client"         % "2.3.0" % "provided"

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
