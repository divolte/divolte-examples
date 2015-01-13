from flask import *
from elasticsearch import Elasticsearch
import json

import config

app = Flask(__name__)
app.config.from_pyfile('config.py')

@app.route('/api/complete/', methods = ['GET'])
def search():
    searches = [
        { 'value': 'aap' },
        { 'value': 'noot' },
        { 'value': 'mies' }
    ]

    top_hits = [
        { 'value': 'BooleanUtils' },
        { 'value': 'StringUtils' }
    ]
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
