Divolte Example: Processing with Spark (Scala)
==============================================

This directory contains an example of processing Divolte events with Spark
using the Scala API.

These have been tested with the Spark distribution included with CDH.

Building the Example
--------------------

Our example needs to be built using [SBT][0]. Once you have this installed
you can build the example:

    % sbt assembly
    % ls -l target/scala-*/*-assembly-*.jar

Running the Example
-------------------

To execute the standalone example:

    % spark-submit --name 'Divolte Spark Example' \
        --class-name io.divolte.examples.DivolteSparkExample \
        target/scala-*/*-assembly-*.jar \
        DIVOLTE_LOG_PATH

If the `DIVOLTE_LOG_PATH` is a glob, you should quote it to avoid shell expansion.
(e.g. `'/tmp/*.avro'` or `'hdfs:///divolte/published'` including the quotes).

  [0]: http://www.scala-sbt.org/   "SBT"
