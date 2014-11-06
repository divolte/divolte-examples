/*
 * Copyright 2014 GoDataDriven B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.divolte.examples.tcpconsumer;

import io.divolte.examples.record.JavadocEventRecord;
import io.divolte.kafka.consumer.DivolteKafkaConsumer;
import io.divolte.kafka.consumer.DivolteKafkaConsumer.EventHandler;

import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Main {
    private static final int NUM_CONSUMER_THREADS = 1;
    private static final String KAFKA_CONSUMER_GROUP_ID = "divolte-tcp-consumer";
    private static final String ZOOKEEPER_QUORUM = "127.0.0.1:2181";
    private static final String KAFKA_TOPIC = "divolte";

    public static void main(String[] args) {
        final DivolteKafkaConsumer<JavadocEventRecord> consumer = DivolteKafkaConsumer.createConsumer(
                KAFKA_TOPIC,
                ZOOKEEPER_QUORUM,
                KAFKA_CONSUMER_GROUP_ID,
                NUM_CONSUMER_THREADS,
                () -> new JavadocEventHandler(),
                JavadocEventRecord.getClassSchema());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down consumer.");
            consumer.shutdownConsumer();
        }));

        System.out.println("Using schema: " + JavadocEventRecord.getClassSchema().toString(true));
        System.out.println("Starting consumer.");
        consumer.startConsumer();
    }

    private static class JavadocEventHandler implements EventHandler<JavadocEventRecord> {
        private static final String TCP_SERVER_HOST = "127.0.0.1";
        private static final int TCP_SERVER_PORT = 1234;

        private Socket socket = null;
        private OutputStream stream;

        @Override
        public void setup() throws Exception {
            socket = new Socket(TCP_SERVER_HOST, TCP_SERVER_PORT);
            stream = socket.getOutputStream();
        }

        @Override
        public void handle(JavadocEventRecord event) throws Exception {
            if (!event.getDetectedDuplicate()) {
                stream.write(event.toString().getBytes(StandardCharsets.UTF_8)); // Avro's toString already produces JSON.
                stream.write("\n".getBytes(StandardCharsets.UTF_8));
            }
        }

        @Override
        public void shutdown() throws Exception {
            if (null != stream) stream.close();
            if (null != socket) socket.close();
        }
    }
}
