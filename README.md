Divolte Collector usage examples
================================

Contained here are some common usage examples of [Divolte Collector](https://github.com/divolte/divolte-collector). Here you will find:
- [A custom Avro schema and Divolte schema mapping for capturing clickstream events on Javadoc pages (this is used in the other examples).](avro-schema/)
- [A howto for using Divolte Collector data in Hive / Impala on Hadoop.](hdfs-hive/)
- [A Kafka consumer example that sends events from Divolte's Kafka topic to a TCP socket.](tcp-kafka-consumer/)
- [A example of using Divolte Collector data in PySpark scripts or iPython notebooks backed by PySpark.](pyspark/)

## Before you begin

Prerequisites:
- You can run a HTTP server that serves static files locally. Serving static files over HTTP is easy if you have Python installed, this is as simple as running 'python -m SimpleHTTPServer' in a directory with static files. On a Mac, you can use [homebrew](http://brew.sh/) to install [http-server](https://www.npmjs.org/package/http-server).
- You have [Java 8](http://www.oracle.com/technetwork/java/javase/downloads/index-jsp-138363.html) installed. *
- For specific examples, we assume some familiarity with the tools used, such as [Apache Kafka](http://kafka.apache.org/), [Hadoop](http://hadoop.apache.org/) or [Apache Spark](https://spark.apache.org/). We don't go into the details of installing and configuring these. If you do want to try the Hadoop examples, but don't know how to setup Hadoop locally, we recommend using the [quickstart VM from Cloudera](http://www.cloudera.com/content/cloudera/en/documentation/DemoVMs/Cloudera-QuickStart-VM/cloudera_quickstart_vm.html) which contains Cloudera's CDH distribution.

#### Javadoc click stream data
All the examples above are based on a Divolte Collector setup that collects click stream data for Javadoc pages. Since all generated Javadoc pages use the same URL layout, it should work with the Javadocs for any project of your choice. To generate Divolte Collector enabled Javadoc for your project, you can use the following command *from the src directory* of your project:

```sh
# If you have a special source encoding, add: -encoding "ISO-8859-1"
javadoc -d <your-output-directory> -bottom '<script src="//localhost:8290/divolte.js" defer async></script>' -subpackages .
```

If you don't want to go through this step, we've provided a pre-built set of Javadocs which is built from the [Apache Commons Lang](http://commons.apache.org/proper/commons-lang/) source code: [javadoc-commons-lang-divolte.tar.gz](javadoc-commons-lang-divolte.tar.gz)

** HAVE FUN! **

<small>\* Divolte Collector itself needs Java 8 as do some of the examples. Any Java libraries we ship for use in third party applications are compatible with Java 7 and above.</small>
