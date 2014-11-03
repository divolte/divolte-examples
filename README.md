Divolte Collector usage examples
================================

Contained here are some common usage examples of
[Divolte Collector][1]. Here you will find:

 - [`avro-schmaa/`](avro-schema/):
   A custom Avro schema and Divolte schema mapping for capturing clickstream
   events on Javadoc pages (this is used in the other examples).
 - [`hdfs-hive/`](hdfs-hive/):
   A howto for using Divolte Collector data in Hive/Impala on Hadoop.
 - [`tcp-kafka-consumer/`](tcp-kafka-consumer/):
   A Kafka consumer example that sends events from Divolte's Kafka topic to
   a TCP socket.
 - Some examples of processing Divolte Collector data using spark:
    - [`pyspark/`](pyspark/):
      Python API, standalone and using IPython notebook.
    - [`spark/`](spark/):
      Scala API.

Before You Begin
----------------

Prerequisites:

 - **You need a HTTP server that can server static files locally.**  
   Serving static files over HTTP is easy if you have Python installed;
   it is as simple as running `python -m SimpleHTTPServer` in a
   directory with static files. (On a Mac, you could install use
   [Homebrew][2] to install [http-server][3].)
 - **You must have [Java 8][4] installed.** *

For specific examples we assume some familiarity with the tools used, such
as [Apache Kafka][5], [Hadoop][6] or [Apache Spark][7]. We don't go into
the details of installing and configuring these. If you do want to try the
Hadoop examples, but don't know how to setup Hadoop locally, we recommend
using the [Quickstart VM from Cloudera][8] which contains Cloudera's CDH
distribution.

### Javadoc Click Stream Data ###

All the examples above are based on a Divolte Collector setup that collects
click stream data for Javadoc pages. Since all generated Javadoc pages use
the same URL layout, it should work with the Javadocs for any project of
your choice. To generate Divolte Collector enabled Javadoc for your project,
you can use the following command *from the source directory* of your project:

    % javadoc -d YOUR_OUTPUT_DIRECTORY -bottom <script src="//localhost:8290/divolte.js" defer async></script>' -subpackages .

Note that if you have a special source encoding you should add
`-encoding "YOUR_ENCODING"` (e.g. `-encoding "ISO-8859-1"`) to the command.

For convenience, the `javadoc-commons-lang-divolte.tar.gz` archive contains
a pre-built set of Javadoc for the [Apache Commons Lang][9] project.
project.

**HAVE FUN!**

---
<small>\* Divolte Collector itself needs Java 8 as do some of the examples.
          Any Java libraries we ship for use in third party applications
          are compatible with Java 7 and above.</small>

  [1]: divolte/divolte-collector                       "Divolte Collector"
  [2]: http://brew.sh                                  "Homebrew"
  [3]: https://www.npmjs.org/package/http-server       "http-server"
  [4]: http://www.oracle.com/technetwork/java/javase/downloads/index-jsp-138363.html "Java 8"
  [5]: https://kafka.apache.org/                       "Apache Kafka"
  [6]: https://hadoop.apache.org/                      "Hadoop"
  [7]: https://spark.apache.org/                       "Apache Spark"
  [8]: http://www.cloudera.com/content/cloudera/en/documentation/DemoVMs/Cloudera-QuickStart-VM/cloudera_quickstart_vm.html "Quickstart VM from Cloudera"
  [9]: http://commons.apache.org/proper/commons-lang/  "Apache Commons Lang"
