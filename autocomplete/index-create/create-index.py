import argparse
import sys
import os

def is_javadoc_dir(root_files):
  JAVADOC_FILES = set([
    'allclasses-frame.html',
    'allclasses-noframe.html',
    'constant-values.html',
    'deprecated-list.html',
    'help-doc.html',
    'index-all.html',
    'index.html',
    'overview-frame.html',
    'overview-summary.html',
    'overview-tree.html',
    'package-list',
    'serialized-form.html',
    'stylesheet.css'])

  return JAVADOC_FILES.issubset(set(root_files))

def document(root_dir, dir, f):
  return {
    'class': f[:-5],
    'package': dir[len(root_dir):].replace(os.sep, '.')
  }

def documents(dir):
  for root, subdirs, files in filter(lambda (r,s,f): 'index.html' not in f, os.walk(dir)):
    for f in filter(lambda f: f.find('-') == -1, files):
      yield document(dir, root, f)

def load_json(filename):
  with codecs.open(filename,'r',encoding='utf-8') as f:
    return json.load(f)

def create_index(es):
  es.indices.create(index=new_index_name, body={
      'settings': {
          'index': {
              'number_of_shards': 1,
              'number_of_replicas': 1,
              'analysis': analysis
          },
      },
      'mappings': mapping
  })

def main(args):
  if not is_javadoc_dir(os.listdir(args.dir)):
    print "Directory '%s' is not a JavaDoc root directory." % args.dir
    sys.exit(1)

  for doc in documents(args.dir):
    print doc


def parse_args():
  parser = argparse.ArgumentParser('Load JavaDoc types into ElasticSearch')
  parser.add_argument('--dir', metavar='JAVADOC_DIR', type=str, default='.', help='Root directory of JavaDoc HTML files.')
  parser.add_argument('--index', metavar='INDEX_NAME', type=str, default='javadoc', help='Index name in ElasticSearch (will be created)')
  parser.add_argument('--host', metavar='ES_HOST', type=str, default='localhost', help='ElasticSearch hostname.')
  parser.add_argument('--port', metavar='ES_PORT', type=int, default=9200, help='ElasticSearch port number (HTTP).')
  return parser.parse_args()


if __name__ == '__main__':
  main(parse_args())
