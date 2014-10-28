from pyspark import SparkConf, SparkContext
from datetime import datetime
import json
import os

HDFS_URI = os.environ['HDFS_URI']

conf = SparkConf().setAppName("Divolte Python script example")
sc = SparkContext(conf = conf)

schema_string = open('../avro-schema/src/main/resources/JavadocEventRecord.avsc').read()
conf = { 'avro.schema.input.key': schema_string }

# Hadoop files are always read as an RDD of key/value pairs. Avro files contain only keys, however.
event_key_values_rdd = sc.newAPIHadoopFile(
  HDFS_URI + '/divolte/published/*.avro',
  'org.apache.avro.mapreduce.AvroKeyInputFormat',
  'org.apache.avro.mapred.AvroKey',
  'org.apache.hadoop.io.NullWritable',
  keyConverter='io.divolte.spark.pyspark.avro.AvroWrapperToJavaConverter',
  conf=conf)

# We map out the keys, so our RDD only contains the actual Avro records.
events_rdd = event_key_values_rdd.map(lambda (k,v): k)

# Keep the events in cluster memory and return the event count in our data set
events_rdd.cache()
event_count = events_rdd.count()
first_event, = events_rdd.take(1)

# Find session with most clicks
session_id, count = events_rdd\
.map(lambda event: (event['sessionId'], 1))\
.reduceByKey(lambda x,y: x + y)\
.reduce(lambda x,y: max(x,y, key=lambda (e,c): c))

# For the longest session, get the first 10 URLs and timestamp
def timestamp_to_string(ts):
    return datetime.fromtimestamp(ts / 1000.0).strftime('%Y-%m-%d %H:%M:%S')

first_ten = '\n'.join(events_rdd\
.filter(lambda event: event['sessionId'] == session_id)\
.sortBy(lambda event: event['timestamp'])\
.map(lambda event: (event['location'], event['timestamp']))\
.map(lambda (loc, ts): "%s @ %s" % (str(loc), timestamp_to_string(ts)) )\
.take(10))

# Print out results; we print these all the way at the bottom, because Spark is pretty verbose and the console gets cluttered a lot
print "\n\n"
print "Number of events in data: %d" % event_count
print "Showing first event:"
print json.dumps(first_event, indent=2)
print "Session with ID '%s' has the most events: %d" % (session_id, count)
print "Showing first 10 events from longest session:"
print first_ten
