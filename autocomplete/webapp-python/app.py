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

from flask import *
from elasticsearch import Elasticsearch
import json

app = Flask(__name__)
app.config.from_pyfile('config.py')

@app.route('/api/complete/', methods = ['GET'])
def complete():
    q = request.args.get('q')
    if not q:
        return jsonify()

    es_query = {
        'suggest': {
            'text': q,
            'completion': {
                'field': 'suggest'
            }
        }
    }

    searches = [{
                    'value': option['text']
                }
                for option in es.suggest(index='suggestion', body=es_query)['suggest'][0]['options']]

    if es.suggest(index='suggestion', body=es_query)['suggest'][0]['options']:
        top_hits = [
            {
                'value': hit['name'],
                'link': hit['link']
            }
            for hit in es.suggest(index='suggestion', body=es_query)['suggest'][0]['options'][0]['payload']['top_hits']
        ]
    else:
        top_hits = []
    
    return jsonify(searches = searches, top_hits=top_hits)

def es_search(q):
    es_query = {
        'query': {
            'multi_match': {
                'query': q,
                'fields': [ 'type_name', 'comment', 'package', 'methods.comment', 'methods.name'],
                'type': 'best_fields'
            }
        },
        'size': 10
    }
    es_result = es.search(index='javadoc', body=es_query)
    return [hit['_source'] for hit in es_result['hits']['hits']]

@app.route('/search/', methods = ['GET'])
def render_dashboard():
    q = request.args.get('q')
    if q:
        hits = es_search(q)
    else:
        hits = []
    return render_template('result.j2', hits=hits, q=q)

# Main
if __name__ == '__main__':
    global es
    es = Elasticsearch()

    app.run(debug = True)
