--
-- Copyright 2014 GoDataDriven B.V.
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--


-- Example create table statement for a Hive table based on Avro files
-- created by Divolte.

-- Note that we only explicitly create one column in the table; this is
-- required in order to have a valid SQL statement. Hive will, however,
-- create a table with all the fields present from the provided Avro schema.

-- Prior to running this, be sure to copy the schema file to HDFS.
-- (JavadocEventRecord.avsc is in the avro-schema/ directory of this
-- repository.)

CREATE EXTERNAL TABLE javadoc (firstInSession boolean)
  ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.avro.AvroSerDe'
  STORED AS INPUTFORMAT 'org.apache.hadoop.hive.ql.io.avro.AvroContainerInputFormat'
  OUTPUTFORMAT 'org.apache.hadoop.hive.ql.io.avro.AvroContainerOutputFormat'
  LOCATION '/divolte/published'
  TBLPROPERTIES (
    'avro.schema.url'='hdfs:///JavadocEventRecord.avsc'
  );
