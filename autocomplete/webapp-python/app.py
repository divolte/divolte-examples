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

    top_hits = [
        {
            'value': 'org.apache.commons.lang3.BooleanUtils',
            'link': 'http://localhost:5000/static/javadoc/org/apache/commons/lang3/BooleanUtils.html'
        },
        {
            'value': 'org.apache.commons.lang3.StringUtils',
            'link': 'http://localhost:5000/static/javadoc/org/apache/commons/lang3/StringUtils.html'
        }
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
