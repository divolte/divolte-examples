from pyspark import SparkConf, SparkContext
from datetime import datetime
import json

def main(hdfs_uri):
    """Divolte Spark Example.

    This example processes published Divolte log files at a given location.

    It displays:

     1. The total number of events in the log files.
     2. An arbitrary event.
     3. The ID of the session with the most events, along with the first 10
        events in that session.

    This is equivalent to the scala example.
    """
    sc = SparkContext()

    # Hadoop files are always read as an RDD of key/value pairs. Avro files contain only keys, however,
    # so we immediately map out the values.
    events_rdd = sc.newAPIHadoopFile(
        hdfs_uri,
        'org.apache.avro.mapreduce.AvroKeyInputFormat',
        'org.apache.avro.mapred.AvroKey',
        'org.apache.hadoop.io.NullWritable',
        keyConverter='io.divolte.spark.pyspark.avro.AvroWrapperToJavaConverter').map(lambda (k,v): k)

    # We are going to process the RDD several times, so cache the original
    # set in cluster memory so it doesn't have to be loaded each time.
    events_rdd.cache()

    # Calculate the total number of events.
    total_event_count = events_rdd.count()

    # Get the first event in our dataset (which isn't ordered yet).
    an_event = events_rdd.take(1)

    # Find the session with the most events.
    (longest_session_id, longest_session_count) = events_rdd \
        .map(lambda event: (event['sessionId'], 1)) \
        .reduceByKey(lambda x,y: x + y) \
        .reduce(lambda x,y: max(x, y, key=lambda (e, c): c))

    # For the session with the most events, find the first 10 events.
    first_events = events_rdd \
        .filter(lambda event: event['sessionId'] == longest_session_id) \
        .map(lambda event: (event['location'], event['timestamp'])) \
        .takeOrdered(10, lambda event: event[1])

    # Simple function for rendering timestamps.
    def timestamp_to_string(ts):
        return datetime.fromtimestamp(ts / 1000.0).strftime('%Y-%m-%d %H:%M:%S')

    # Print the results we accumulated, with some whitespace at the
    # front to separate this from the logging.
    print "\n\n"
    print "Number of events in data: %d" % total_event_count
    print "An event:\n%s" % json.dumps(an_event, indent=2)
    print "Session with id '%s' has the most events: %d" % (longest_session_id, longest_session_count)
    print "First 10 events:"
    print "\n".join(["  %s: %s" % (timestamp_to_string(ts), location) for (location, ts) in first_events])

if __name__ == "__main__":
    import sys
    if (len(sys.argv) >= 2):
        main(*sys.argv[1:])
    else:
        print >> sys.stderr, "Usage: spark-submit [...] divolte_spark_example.py PATH_TO_DIVOLTE_LOGS"
