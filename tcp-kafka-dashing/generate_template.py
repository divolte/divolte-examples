import avro.schema
import avro.io
import argparse

def select_dashing(options):
    selected = raw_input('\tChoose which between %s, [%s]' % (", ".join(options), options[0]))
    if selected not in options + ['']:
        return select_dashing(options)
    return selected or options[0]

def generate_mapping(input_avro, output_template, output_properties):
    mapping = {}
    json_schema = ""
    with open(input_avro) as fp:
        for line in fp:
            if not line.startswith("//"):
                json_schema += line

    schema = avro.schema.parse(json_schema)
    for key, value in schema.fields_dict.iteritems():
        type = value.to_json()['type']
        if isinstance(type, basestring):
            type = [type]

        answer = raw_input('Include "%s" in the dashing dashboard? [yes]' % key)
        if answer == '' or answer == 'yes':
            if 'boolean' in type:
                dashing_type = 'graph'
            elif 'string' in type:
                dashing_type = select_dashing(['pie', 'list'])
            else:
                dashing_type = select_dashing(['graph', 'number', 'meter'])

            mapping[key] = dashing_type

    template = open(output_template, 'w')
    properties = open(output_properties, 'w')

    for key, value in mapping.iteritems():
        print >> properties, key, value

        if value == 'graph':
            dashing_template = """
            <li data-row="1" data-col="1" data-sizex="2" data-sizey="1">
                <div data-id="%s" data-view="Graph" data-title="%s" style="background-color:#ff9618"></div>
            </li>"""
        elif value == 'list':
            dashing_template = """
            <li data-row="1" data-col="1" data-sizex="1" data-sizey="1">
                <div data-id="%s" data-view="List" data-unordered="true" data-title="%s" data-moreinfo=""></div>
            </li>"""
        elif value == 'pie':
            dashing_template = """
            <li data-row="1" data-col="1" data-sizex="1" data-sizey="1">
                <div data-id="%s" data-view="Pie" data-title="%s"></div>
            </li>"""
        elif value == 'number':
            dashing_template = """
            <li data-row="1" data-col="1" data-sizex="1" data-sizey="1">
                <div data-id="%s" data-view="Number" data-title="%s" data-moreinfo="" data-prefix=""></div>
            </li>"""
        else:
            dashing_template = """
            <li data-row="1" data-col="1" data-sizex="1" data-sizey="1">
                <div data-id="%s" data-view="Meter" data-title="%s" data-min="0" data-max="100"></div>
            </li>"""

        print >> template, dashing_template % (key, key.capitalize())

    template.close()
    properties.close()

def parse_args():
    def utf8_bytes(s):
        return bytes(s, 'utf-8')

    parser = argparse.ArgumentParser(description='Runs the consumer.')
    parser.add_argument('--schema', '-s', metavar='SCHEMA', type=str, required=True, help='Avro schema of Kafka messages.')
    parser.add_argument('--template', '-t', metavar='TEMPLATE', type=str, required=False, default='template.dashing', help='Where to output the dashing widget-template.')
    parser.add_argument('--properties', '-p', metavar='PROPERTIES', type=str, required=False, default='properties.dashing', help='Where to output the property file mapping the avro fields to dashing widgets.')
    return parser.parse_args()

if __name__ == '__main__':
    args = parse_args()
    generate_mapping(args.schema, args.template, args.properties)
