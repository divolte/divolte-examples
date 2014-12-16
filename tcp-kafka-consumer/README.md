Divolte tcp-kafka-consumer example
==================================

This example uses the [divolte-kafka-consumer](https://github.com/divolte/divolte-kafka-consumer) helper library to create a Kafka consumer that sends events as JSON string to a TCP socket. To run this, you need:
- The accompanying Javadoc Avro schema installed into your local Maven repository.
- A running HTTP server which serves the static Javadoc HTML files instrumented with the Divolte Collector tag.
- Kafka (including Zookeeper)

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

#### Step 5: listen on TCP port 1234
```sh
nc -kl 1234
```
>Note: when using netcat (nc) as TCP server, make sure that you configure the Kafka consumer to use only 1 thread, because nc won't handle multiple incoming connections.

#### Step 6: run the example
```sh
cd divolte-examples/tcp-kafka-consumer
mvn clean package
java -jar target/tcp-kafka-consumer-*-jar-with-dependencies.jar
```
> Note: for this to work, you need to have the [avro-schema](../avro-schema) project installed into your local Maven repository.

#### Step 7: click around and check that you see events being flushed to the console where you run netcat
When you click around the Javadoc pages, you console should show events in JSON format similar to this:
```
{"detectedDuplicate": false, "firstInSession": false, "timestamp": 1414926813382, "remoteHost": "127.0.0.1", "referer": "http://localhost:9090/allclasses-frame.html", "location": "http://localhost:9090/org/apache/commons/lang3/CharSequenceUtils.html", "viewportPixelWidth": 1338, "viewportPixelHeight": 895, "screenPixelWidth": 1680, "screenPixelHeight": 967, "partyId": "0:i0eau356:6i93LuQg91FmRkx4ixmq4jaX3tK_UXWW", "sessionId": "0:i20arq3w:0imfFKjRs9L693gTuMc_AlWo4sGzgAMd", "pageViewId": "0:fyghi5mGFZYfT9PMAsUvM9DI4yy9sKVe", "eventType": "pageView", "userAgentString": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/38.0.2125.104 Safari/537.36", "userAgentName": "Chrome", "userAgentFamily": "Chrome", "userAgentVendor": "Google Inc.", "userAgentType": "Browser", "userAgentVersion": "38.0.2125.104", "userAgentDeviceCategory": "Personal computer", "userAgentOsFamily": "OS X", "userAgentOsVersion": "10.9.5", "userAgentOsVendor": "Apple Computer, Inc.", "pageType": "java-class", "javaPackage": "/org/apache/commons/lang3", "javaType": "CharSequenceUtils"}
```

## A more interesting use
Of course, streaming events as JSON to your console over TCP is not very interesting. But if you happen to have [ElasticSearch, Logstash and Kibana ](http://www.elasticsearch.org/overview/elkdownloads/) running, you kan create near real-time dashboards using this example by streaming you data throuhg Logstash into ElasticSearch and creating a Kibana dashboard on top of it. Here is a example logstash configuration for use on your local machine:
```sh
logstash -e '
input {
  tcp {
    port => 1234
    codec => json
  }
}

filter {
  date {
    match => ["timestamp", "UNIX_MS"]
  }
}

output {
  elasticsearch {
    protocol => http
  }
}
'
```
