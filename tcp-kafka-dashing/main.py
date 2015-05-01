import avro.schema
import avro.io
import argparse
from kafka import KafkaConsumer
from StringIO import StringIO
from utils import RunningAverage, StringRunningAverage
from threading import Thread
import requests
import json
import time
from requests.exceptions import ConnectionError

def construct_averages(input_properties):
    values = {}
    for line in open(input_properties):
        key, dashing_type = line.strip().split()
        if dashing_type in ['pie', 'list']:
            values[key] = (dashing_type, StringRunningAverage(5, 60))
        else:
            values[key] = (dashing_type, RunningAverage(1 if key == 'pageView' else 5, 60))
    return values

def start_consumer(input_avro, metadata_broker_list, values):
    json_schema = ""
    with open(input_avro) as fp:
        for line in fp:
            if not line.startswith("//"):
                json_schema += line

    schema = avro.schema.parse(json_schema)
    dreader = avro.io.DatumReader(schema, schema)

    def parse_message(msg):
        return dreader.read(avro.io.BinaryDecoder(StringIO(msg)))

    consumer = KafkaConsumer("divolte", metadata_broker_list=metadata_broker_list, deserializer_class=lambda msg: parse_message(msg))
    for message in consumer:
        timestamp = int(message.value['timestamp'] / 1000)
        for key, value in message.value.iteritems():
            if key == 'eventType':
                key, value = value, 1

            if key in values:
                values[key][1].addValue(timestamp, value)

    consumer.close()

def start_poster(dashing_ip, dashing_auth, values):
        def gen_list(dictionary, x, y, sortByKey=False, sortByValue=False, removeZeros=False):
            keys = dictionary.keys()
            if sortByValue:
                keys.sort(cmp=lambda a, b: cmp(dictionary[a], dictionary[b]), reverse=True)
            if sortByKey:
                keys.sort()
            return [{x: key, y: dictionary[key]} for key in keys if not removeZeros or (dictionary[key] != 0 and key)]

        while True:
            for key, value in values.iteritems():
                try:
                    dashing_type, average = value
                    if dashing_type == 'list':
                        payload = {'auth_token': dashing_auth, 'items': gen_list(average.getSumAsDict(), "label", "value", sortByValue=True, removeZeros=True)}
                    elif dashing_type == 'graph':
                        payload = {'auth_token': dashing_auth, 'points': gen_list(average.getSumAsDict(), 'x', 'y', sortByKey=True)}
                    elif dashing_type == 'pie':
                        payload = {'auth_token': dashing_auth, 'value': gen_list(average.getSumAsDict(), 'label', 'value', removeZeros=True)}
                    elif dashing_type == 'number':
                        payload = {'auth_token': dashing_auth, 'current': average.getSum(), 'last': average.getSum()}
                    else:
                        raise RuntimeError("'%s' is not supported" % dashing_type)

                    requests.post("http://%s/widgets/%s" % (dashing_ip, key), data=json.dumps(payload))

                except ConnectionError:
                    pass

            time.sleep(1)

def parse_args():
    def utf8_bytes(s):
        return bytes(s, 'utf-8')

    parser = argparse.ArgumentParser(description='Runs the consumer.')
    parser.add_argument('--schema', '-s', metavar='SCHEMA', type=str, required=True, help='Avro schema of Kafka messages.')
    parser.add_argument('--properties', '-p', metavar='PROPERTIES', type=str, required=False, default='properties.dashing', help='The mapping of the avro fields to dashing widgets.')
    parser.add_argument('--dashing', '-d', metavar='DASHING_HOST_PORT', type=str, required=False, default='localhost:3030', help='Dashing hostname + port.')
    parser.add_argument('--dashingauth', '-a', metavar='DASHING_AUTH', type=str, required=False, default='YOUR_AUTH_TOKEN', help='Dashing auth token.')
    parser.add_argument('--brokers', '-b', metavar='KAFKA_BROKERS', type=str, nargs="+", help='A list of Kafka brokers (host:port).', default=['localhost:9092'])
    return parser.parse_args()

if __name__ == '__main__':
    args = parse_args()

    values = construct_averages(args.properties)

    t = Thread(target=start_poster, args=(args.dashing, args.dashingauth, values))
    t.start()

    start_consumer(args.schema, args.brokers, values)
