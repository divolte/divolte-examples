#
# Copyright 2015 GoDataDriven B.V.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

import argparse
import sys
import json
from elasticsearch import Elasticsearch
from elasticsearch import helpers as es_helpers
from pyspark import SparkContext
from functools import partial

def search_fragments(session):
    def fragment_dict(
            start_time,
            phrase,
            num_results,
            time_to_first_result,
            time_to_next_search,
            event_count,
            results):
        return {
                    'start_time': start_time,
                    'phrase': phrase,
                    'num_results': num_results,
                    'time_to_first_result': None if time_to_first_result == sys.maxint else time_to_first_result,
                    'time_to_next_search': None if time_to_next_search == sys.maxint else time_to_next_search,
                    'event_count': event_count,
                    'results': results
                  }

    phrase = None
    start_time = sys.maxint
    num_results = 0
    time_to_first_result = sys.maxint
    event_count = 0
    results = []
    
    for event in session:
        if (event['pageType'] == 'search') and (event['searchPhrase'] != phrase):
            if phrase:
                yield fragment_dict(start_time, phrase, num_results, time_to_first_result,
                                    event['timestamp'] - start_time, event_count, results)
                
            start_time = event['timestamp']
            phrase = event['searchPhrase']
            num_results = 0
            time_to_first_result = sys.maxint
            event_count = 0
            results = []
        elif event['pageType'] != 'search' and event['isSearchResult']:
            num_results += 1
            time_to_first_result = min(time_to_first_result, event['timestamp'] - start_time)
            results += [ { 'type': event['javaType'], 'package': event['javaPackage'] } ]

        event_count += 1
    
    if phrase:
        yield fragment_dict(start_time, phrase, num_results, time_to_first_result,
                            sys.maxint, event_count, results)

def events_rdd(sc, hdfs_uri, avro_schema):
    conf = { 'avro.schema.input.key': file(avro_schema).read() }
    return sc.newAPIHadoopFile(
        hdfs_uri,
        'org.apache.avro.mapreduce.AvroKeyInputFormat',
        'org.apache.avro.mapred.AvroKey',
        'org.apache.hadoop.io.NullWritable',
        keyConverter='io.divolte.spark.pyspark.avro.AvroWrapperToJavaConverter',
        conf=conf).map(lambda (k,v): k)

def strip_accents(s):
   return ''.join([c for c in unicodedata.normalize('NFD', s) if unicodedata.category(c) != 'Mn'])

def search_extraction((id, session)):
    return [sf for sf in search_fragments(session)]

def create_index(es, idx):
    if es.indices.exists(idx):
        es.indices.delete(idx)

    body = {
        'settings': json.load(file('settings.json')),
        'mappings': json.load(file('mapping.json'))
    }

    es.indices.create(index=idx, body=body)

def index_completion_partition(completions, hosts, port):
    def actions():
        for completion in completions:
            yield {
                '_index': 'suggestion',
                '_type': 'suggestion',
                '_op_type': 'index',
                '_source': {
                    'suggest': {
                        'input': completion['phrase'],
                        'weight': completion['weight'],
                        'payload': {
                            'top_hits': completion['top_hits']
                        }
                    }
                }
            }

    es = Elasticsearch([ {'host': host, 'port': port } for host in hosts ])
    for x in es_helpers.streaming_bulk(client=es, actions=iter(actions()), chunk_size=100, raise_on_error=True):
        pass # We MUST exhaust the result of the streaming_bulk operation, otherwise notihing is performed

def main(args):
    print 'Connecting to Elasticsearch...'
    es = Elasticsearch([ {'host': host, 'port': args.es_port } for host in args.es_hosts ])
    print '(Re)createing index...'
    create_index(es, args.index_name)

    print 'Executing Spark job...'
    sc = SparkContext()
    events = events_rdd(sc, args.input, args.schema)
    searches = events.keyBy(lambda e: e['sessionId']).groupByKey().flatMap(search_extraction)

    successes = searches.filter(lambda s: s['num_results'] > 0)
    suggestions = successes.map(lambda s: (s['phrase'], 1)).reduceByKey(lambda x,y: x+y)
    
    top_hits = successes\
    .map(lambda s: ((s['phrase'], frozenset(s['results'][-1].items())), 1))\
    .reduceByKey(lambda x,y: x+y)\
    .map(lambda ((l,r), cnt): (l, [(dict(r), cnt)]))\
    .reduceByKey(lambda x,y: sorted(x + y, key=lambda (r,c): c)[:5])

    completions = suggestions.join(top_hits)\
    .map(lambda (phrase, (cnt, hits)): {
        'phrase': phrase,
        'weight': cnt,
        'top_hits': [
            {
                'name': '%s.%s' % (hit['package'].replace('/','.'), hit['type']),
                'link': '/static/javadoc/%s/%s.html' % (hit['package'], hit['type'])
            }
            for hit, hit_cnt in hits
        ]
    })


    print 'Sending suggestions to Elasticsearch...'

    completions.foreachPartition(partial(index_completion_partition, hosts=args.es_hosts, port=args.es_port))

def parse_args():
    parser = argparse.ArgumentParser(description='Take click stream data from HDFS and create and populate a ElasticSearch index for autocomplete suggestions.')
    parser.add_argument('--input', '-i', metavar='HDFS_URI', type=str, required=True, help='HDFS URI pointing to the published Avro files created by Divolte Collector. Can be a glob.')
    parser.add_argument('--schema', '-s', metavar='AVRO_SCHEMA_FILE', type=str, required=True, help='The Avro schema file to use as reading schema for the data.')
    parser.add_argument('--es-hosts', '-e', metavar='HOSTNAME', type=str, nargs="+", help='A list of ElasticSearch hosts to connect to.', default=['localhost'])
    parser.add_argument('--es-port', '-p', metavar='PORT', type=int, help='Port number that ElasticSearch runs on.', default=9200)
    parser.add_argument('--index-name', '-n', metavar='INDEX_NAME', type=str, help='The name of the ElasticSearch index to use. This index will be deleted if it already exists.', default='suggestion')

    return parser.parse_args()


if __name__ == '__main__':
    main(parse_args())
