package io.divolte.examples

import java.nio.file.Files

import io.divolte.spark.avro._
import kafka.utils.ZkUtils
import org.apache.avro.generic.GenericRecord
import org.apache.spark.SparkContext
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.{Minutes, Seconds, StreamingContext}

/**
 * Divolte Spark Streaming Example.
 *
 * This example processes events from Divolte in real time.
 *
 * It displays:
 *
 *  1. The number of currently active users.
 *  2. The URLs of events, as they occur.
 *  3. The most popular 5 pages over the last 1, 5 and 15 minutes.
 */
object DivolteSparkStreamingExample extends App {
  val zookeepers = args match {
    case Array(firstArg, _*) => firstArg
    case _ =>
      Console.err.println("Usage: DivolteSparkStreamingExample ZOOKEEPER_CONNECT_STRING")
      sys.exit(1)
  }

  // Kafka configuration.
  val consumerConfig = Map(
    "group.id"                -> "divolte-spark-streaming-example",
    "zookeeper.connect"       -> zookeepers,
    "auto.commit.interval.ms" -> "5000",
    "auto.offset.reset"       -> "largest"
  )
  val topicSettings = Map("divolte" -> Runtime.getRuntime.availableProcessors())

  // On startup, ignore any previous offset information and start from current events.
  // Note: this only works because we are a single node.
  ZkUtils.maybeDeletePath(zookeepers, s"/consumers/${consumerConfig("group.id")}")

  // Create the spark and streaming contexts. Micro batches per second.
  val sc = new SparkContext()
  val ssc = new StreamingContext(sc, Seconds(1))

  // Where checkpoints will be stored.
  val checkPointDirectory = Files.createTempDirectory("divolte-streaming-example")
  checkPointDirectory.toFile.deleteOnExit()
  ssc.checkpoint(checkPointDirectory.toString)

  // Establish the source event stream.
  val stream = ssc.divolteStream[GenericRecord](consumerConfig, topicSettings, StorageLevel.MEMORY_ONLY)

  // Assuming sessions last half an hour, create a stream counting the number of active users.
  val partyIdStream: DStream[String] = stream.mapValues(_ => None).map(_._1)
  val uniquePartyCountStream = partyIdStream
    .countByValueAndWindow(windowDuration = Minutes(30), slideDuration = Seconds(5))
    .map(_._1)
    .count()

  // Create a stream creating just the page locations for each event.
  val pageIdStream: DStream[String] = stream
    .fields("location")
    .flatMap(_._2.head.map(_.asInstanceOf[String]))

  // Create streams counting the top 5 locations during over various intervals.
  val locationCountsStreams = Seq(1L, 5L, 15L).map { windowMinutes =>
    val window = Minutes(windowMinutes)
    window ->
      pageIdStream.countByValueAndWindow(windowDuration = window, slideDuration = Seconds(15))
  }

  // Configure our displays:
  //   - Every URL, as they occur.
  //   - The active count.
  //   - The top 5 counts.
  uniquePartyCountStream.foreachRDD { rdd =>
    println(s"Active user count: ${rdd.collect().head}")
  }
  pageIdStream.foreachRDD { rdd =>
    rdd.collect().foreach { location =>
      println(s"Location: $location")
    }
  }
  locationCountsStreams.foreach { case (window, locationCountsStream) =>
    locationCountsStream.foreachRDD { locationCounts =>
      val top5 = locationCounts.top(5)(Ordering.by(_._2))
      println(s"=== Top 5 (${window.prettyPrint}) ===")
      top5.zipWithIndex.foreach { case ((location, count), index) =>
          println(s" ${index+1}. $location ($count)")
      }
    }
  }

  // Start the stream and await termination.
  ssc.start()
  ssc.awaitTermination()
}
