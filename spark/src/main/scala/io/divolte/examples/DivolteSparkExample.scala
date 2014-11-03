package io.divolte.examples

import java.util.Date

import io.divolte.spark.avro._
import org.apache.avro.generic.IndexedRecord
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._

/**
 * Divolte Spark Example.
 *
 * This example processes published Divolte log files at a given location.
 *
 * It displays:
 *
 *  1. The total number of events in the log files.
 *  2. An arbitrary event.
 *  3. The ID of the session with the most events, along with the first 10
 *     events in that session.
 *
 * This is equivalent to the python example.
 */
object DivolteSparkExample extends App {
  val path = args match {
    case Array(firstArg, _*) => firstArg
    case _ =>
      System.err.println("Usage: DivolteSparkExample PATH_TO_DIVOLTE_LOGS")
      sys.exit(1)
  }

  // Create the Spark context that we're going to use.
  val sc = new SparkContext()

  // Define the RDD that we are going to manipulate.
  val events = sc.newAvroFile[IndexedRecord](path)

  // Calculate the total number of events.
  // Because we're not interested in the event count we don't extract any
  // record information.
  val totalEventCount = events.map(_ => None).count()

  // Get the first event in our dataset (which isn't ordered yet).
  // Here we do a full record conversion because we want everything.
  val anEvent = events.toRecords.take(1).headOption

  // For the longest session, we're only interested in a few fields.
  // For efficiency that's all we extract from each event.
  val eventFields = events.fields("sessionId", "location", "timestamp")
  // Find the session with the most events.
  val longestSession = eventFields
    .flatMap(_.head)
    .map(_ -> 1)
    .reduceByKey(_ + _)
    .top(1)(Ordering.by(_._2)).headOption
  // For the session with the most events, find the first 10 events.
  val firstEvents = longestSession.map { case (sessionId, _) =>
      eventFields.collect {
          case Seq(Some(`sessionId`), location, timestamp) =>
            (location, timestamp.map(_.asInstanceOf[Long]))
        }
        .takeOrdered(10)(Ordering.by(_._2))
        .map { case (location,timestamp) =>
          (location, timestamp.map(new Date(_)))
        }
  } .getOrElse(Array.empty)

  // Print the results we accumulated, with some whitespace at the
  // front to separate this from the logging.
  println("\n\n")
  println(s"Number of events in data: $totalEventCount")
  println(s"An event:\n  ${anEvent.getOrElse("N/A")}")
  longestSession.foreach { case (sessionId, sessionEventCount) =>
    println(s"Session with id '$sessionId' has the most events: $sessionEventCount")
    println("First 10 events:")
    firstEvents.foreach { event =>
      println(s"  ${event._2.getOrElse("N/A")}: ${event._1.getOrElse("N/A")}")
    }
  }
}