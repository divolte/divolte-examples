Divolte Example: Processing with Spark (Scala) & Spark Streaming
================================================================

This directory contains some examples of processing Divolte events with
Spark using the Scala API. The examples provided are:

1. A standalone Spark example, equivalent to the Pyspark example.
2. A Spark Streaming example.

These have been tested with the Spark distribution included with CDH.

Building the Examples
---------------------

Our examples needs to be built using [SBT][0]. Once you have this installed
you can build the example:

    % sbt assembly
    % ls -l target/scala-*/*-assembly-*.jar

Running the Examples
-------------------

### Standalone Example

To execute the standalone example:

    % spark-submit --name 'Divolte Spark Example' \
        --class io.divolte.examples.DivolteSparkExample \
        target/scala-*/*-assembly-*.jar \
        DIVOLTE_LOG_PATH

If the `DIVOLTE_LOG_PATH` is a glob, you should quote it to avoid shell expansion.
(e.g. `'/tmp/*.avro'` or `'hdfs:///divolte/published'` including the quotes).

### Spark Streaming Example

As a prerequisite for the streaming example you need to configure Divolte Collector
to publish events to Kafka on a queue named 'divolte' and have the Divolte Collector
running. For example, you could use the following Divolte configuration snippet:

```HOCON
divolte {
  kafka_flusher {
    enabled = true
    producer = {
      // Assumes a Kafka server is available locally.
      metadata.broker.list = ["127.0.0.1:9092"]
      client.id = divolte-spark-example
      message.send.max.retries = 100
      retry.backoff.ms = 250
    }
  }
}
```

To execute the Spark Streaming example:

    % spark-submit --name 'Divolte Spark Streaming Example' \
        --class io.divolte.examples.DivolteSparkStreamingExample \
        target/scala-*/*-assembly-*.jar \
        ZOOKEEPER_CONNECT_STRING

Assuming that ZooKeeper is running locally, an appropriate connect string
would be `"127.0.0.1:2181"`.

Note that the streaming example only has something to show from the moment
that it starts; there needs to be traffic for it to display anything.

  [0]: http://www.scala-sbt.org/   "SBT"
