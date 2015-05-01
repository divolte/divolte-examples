Divolte tcp-kafka-dashing example
==================================

This example uses the python to create a Kafka consumer that sends events from Divolte to Dashing. To run this, you need:
- The accompanying Javadoc Avro schema installed into your local Maven repository.
- A running HTTP server which serves the static Javadoc HTML files instrumented with the Divolte Collector tag.
- Kafka (including Zookeeper)
- Dashing
- kafka-python (pip install kafka-python)
- apache avro (http://avro.apache.org/docs/1.7.6/gettingstartedpython.html#download_install)

## Building and running

#### Step 1: install and configure Divolte Collector
Download the latest [Divolte Collector](https://github.com/divolte/divolte-collector) release. Use either the .zip or the .tar.gz archive. Extract the archive to a directory of your choice. In the installation, there is a conf/ directory. In here, create a file called divolte-collector.conf with the following contents:
```hocon
divolte {
  kafka_flusher {
    enabled = true
    threads = 1
  }

  hdfs_flusher {
    enabled = false
  }

  javascript {
    logging = true
    debug = true
  }

  tracking {
    schema_file = /path/to/divolte-examples/avro-schema/src/main/resources/JavadocEventRecord.avsc
    schema_mapping {
      version = 2
      mapping_script_file = "/path/to/divolte-examples/avro-schema/mapping.groovy"
    }
  }
}
```
> *Make sure you correct the paths to the Avro schema and mapping configuration!*

#### Step 2: download, unpack and run Kafka
Setting up Kafka is beyond the scope of this document. It is however very simple to get Kafka up and running on your local machine using all default settings. [Download a Kafka release](https://www.apache.org/dyn/closer.cgi?path=/kafka/0.8.1.1/kafka_2.10-0.8.1.1.tgz), unpack it and run as follows:
```sh
# In one terminal session
cd kafka_2.10-0.8.1.1/bin
./zookeeper-server-start.sh ../config/zookeeper.properties

# Leave Zookeeper running and in another terminal session, do:
cd kafka_2.10-0.8.1.1/bin
./kafka-server-start.sh ../config/server.properties
```
#### Step 3: start Divolte Collector
Go into the bin directory of your installation and run:
```sh
cd divolte-collector-0.2/bin
./divolte-collector
```

#### Step 4: host your Javadoc files
Setup a HTTP server that serves the Javadoc files that you generated or downloaded for the examples. If you have Python installed, you can use this:
```sh
cd <your-javadoc-directory>
python -m SimpleHTTPServer
```

#### Step 5: build a dashing template based on your avro-schema
```sh
cd divolte-examples/tcp-kafka-dashing
python generate_template.py --schema /path/to/divolte-examples/avro-schema/src/main/resources/JavadocEventRecord.avsc
```

#### Step 7: generate a new dashing dashboard and install the required pie widget
```sh
cd <location-for-your-dashing-dashboard>
dashing new divolte_dashboard_project
cd divolte_dashboard_project
dashing install 6273841
bundle
```

#### Step 8: copy/paste the template into your new dashboard
Edit dashboards/sample.erb, replace the body of the ul-element with the contents of the template.dashing file your generated in step 5.  

#### Step 9: start everything
```sh
cd <location-for-your-dashing-dashboard>/divolte_dashboard_project
dashing start

# Leaving it running and in another terminal do
cd divolte-examples/tcp-kafka-dashing
python main.py --schema /path/to/divolte-examples/avro-schema/src/main/resources/JavadocEventRecord.avsc 
```

#### Step 10: enjoy
Go to localhost:8000 to view the javadoc, and localhost:3030 to view the dashboard