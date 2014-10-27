-- Example create table statement for a Hive table based on Avro files
-- created by Divolte.

-- Note that we only explicitly create one column in the table; this is
-- required in order to have a valid SQL statement. Hive will, however, 
-- create a table with all the fields present in the provided Avro schema.

-- Prior to running this, be sure to copy the schema file to HDFS.
-- (JavadocEventRecord.avsc from the avro-schema directory in this same
-- project)

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
