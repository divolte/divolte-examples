Divolte Collector data in Hive
==============================

Example setup for using files created by Divolte Collector in Hive. Installing Hadoop HDFS, MapReduce and Hive is beyond the scope of this document. We assume familiarity with these technologies.

To setup Divolte Collector to flush files to HDFS, use the following contents for your divolte-collector.conf configuration file:

```hocon
divolte {
  kafka_flusher {
    enabled = false
  }

  hdfs_flusher {
    enabled = true

    threads = 1

    hdfs {
      // Point to your HDFS NameNode here.
      // In case of multiple NameNode's or other configuration,
      // you can leave out this configuration altogether and
      // make sure there is a HADOOP_CONF_DIR env var that points
      // to your Hadoop configuration files.
      uri = "hdfs://127.0.0.1:8020/"

      // Set to a higher value for production use.
      replication = 1
    }

    simple_rolling_file_strategy {
      // For the example's sake, we roll files very quickly.
      // In a production scenario, you would rather set this
      // to something on the order of one hour.
      roll_every = 60 seconds

      // Syncing files requires flushing, network traffic and coordination.
      // Also, it will write a Avro block boundary, which could lead to
      // inefficiencies when there are too many. In production, syncing should
      // be set to happen once every 1000 records or more and perhaps once
      // every 30 seconds.
      sync_file_after_records = 100
      sync_file_after_duration = 5 seconds

      // Make sure these directories exist in HDFS, or Divolte will not start.
      working_dir = "/divolte/inflight"
      publish_dir = "/divolte/published"
    }
  }

  tracking {
    schema_file = /path/to/divolte-examples/avro-schema/src/main/resources/JavadocEventRecord.avsc
  }
}

include file("/path/to/code/divolte-examples/avro-schema/mapping.conf")
```
> *Make sure you correct the paths to the Avro schema and mapping configuration!*

When running Divolte Collector, you can use the following create table statement in Hive to make the data available as a Hive table:
```sql
-- Example create table statement for a Hive table based on Avro files
-- created by Divolte.

-- Note that we only explicitly create one column in the table; this is
-- required in order to have a valid SQL statement. Hive will, however,
-- create a table with all the fields present in the provided Avro schema.

-- Prior to running this, be sure to copy the schema file to HDFS.
-- (JavadocEventRecord.avsc from the avro-schema directory in this same
-- repo)

CREATE EXTERNAL TABLE javadoc (
  firstInSession boolean
  )
  ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.avro.AvroSerDe'
  STORED AS INPUTFORMAT 'org.apache.hadoop.hive.ql.io.avro.AvroContainerInputFormat'
  OUTPUTFORMAT 'org.apache.hadoop.hive.ql.io.avro.AvroContainerOutputFormat'
  LOCATION '/divolte/published'
  TBLPROPERTIES (
    'avro.schema.url'='hdfs:///JavadocEventRecord.avsc'
  );
```
