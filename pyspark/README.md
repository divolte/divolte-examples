Divolte Example: Processing with Spark (Python)
===============================================

This directory contains some examples of processing Divolte events with Spark
using Python.

The examples are:

 - `divolte_spark_example_notebook.ipynb`: An [IPython](http://ipython.org) notebook
    which demonstrates how to interactively process Divolte events using Spark.
 - `divolte_spark_example.py`: A standalone python script which can be submitted
    using `spark-submit`.

These have been tested with the Spark distribution included with CDH.

Building the Required Helper Library
------------------------------------

Our examples make use of a helper library that we provide in our
[Divolte Spark](https://github.com/divolte/divolte-spark) project. We use
[SBT](http://www.scala-sbt.org/) for building this. Once you have it installed you
can build the helper library:

    % git clone https://github.com/divolte/divolte-spark.git
    % cd divolte-spark
    % sbt assembly
    % DIVOLTE_SPARK_JAR="$PWD"/target/scala-*/divolte-spark-assembly-*.jar

Using the Example IPython Notebook
----------------------------------

To start the IPython notebook:

    % DIVOLTE_SPARK_JAR="<PATH_TO_DIVOLTE_SPARK_JAR>"
    % export IPYTHON=1
    % export IPYTHON_OPTS="notebook"
    % pyspark --jars "$DIVOLTE_SPARK_JAR" --driver-class-path "$DIVOLTE_SPARK_JAR"

You should set `DIVOLTE_SPARK_JAR` to match the location of the helper library built
in the previous section.

If run locally, your browser should automatically open to the notebook. If not,
open the URL displayed by IPython in your browser.

Running the Standalone Example
------------------------------

To execute the standalone example:

    % DIVOLTE_SPARK_JAR="<PATH_TO_DIVOLTE_SPARK_JAR>"
    % spark-submit --jars "$DIVOLTE_SPARK_JAR" --driver-class-path "$DIVOLTE_SPARK_JAR" divolte_spark_example.py DIVOLTE_LOG_PATH

As with the IPython notebook example you should set `DIVOLTE_SPARK_JAR` to match
the location of where you built the helper library.
