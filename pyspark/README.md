Python based Spark examples for Divolte Collector data
======================================================

We publish [a helper library](divolte/divolte-spark) for using Avro files in [Apache Spark](https://spark.apache.org/). Here are two examples of using this in PySpark. One is a Python script, the other is a iPython notebook.

## Building the required helper library
Clone the [divolte/divolte-spark](divolte/divolte-spark) project from Github. We use [sbt](http://www.scala-sbt.org/) for building this library (as it is Scala). After cloning, run:
```sh
cd divolte-spark
sbt assembly
```

## Running iPython notebook
```sh
IPYTHON=1 IPYTHON_OPTS="notebook" /path/to/spark/bin/pyspark --jars /path/to/divolte-spark/target/scala-2.10/divolte-spark-assembly-*.jar
```

On some Spark distributions (most notably CDH), the --jars option doesn't actually do what the documentation says. In that case, you need to run with:
```sh
IPYTHON=1 IPYTHON_OPTS="notebook --ip=0.0.0.0" /path/to/spark/bin/pyspark --jars /path/to/divolte-spark/target/scala-2.10/divolte-spark-assembly-*.jar --driver-class-path /path/to/divolte-spark/target/scala-2.10/divolte-spark-assembly-*.jar
```

## Running the Python script
```sh
/path/to/spark/bin/spark-submit --jars ../../divolte-spark/target/scala-2.10/divolte-spark-assembly-*.jar pyspark_script_example.py
```
