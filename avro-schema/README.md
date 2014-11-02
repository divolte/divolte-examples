Divolte examples avro-schema
============================

[This](src/main/resources/JavadocEventRecord.avsc) is the Avro schema used for all the Divolte examples. This project uses [Maven](http://maven.apache.org/) as a build tool for ease of integration.

In order to run the Divolte examples, you need to:
- Install the artefacts from this Maven build into your local repository.
- Create a set of Javadocs instrumented with the Divolte Collector tag or extract [our pre-built set of instrumented Javadocs](../javadoc-commons-lang-divolte.tar.gz).

Installing into your local repo:
```sh
# Assuming you have Maven installed
mvn clean install
```

To generate Javadoc with the Divolte tag, go into the src directory of any Java project that has Javadoc comments, and run the following:
```sh
# If you have a special source encoding, add: -encoding "YOUR_ENCODING" (e.g. -encoding "ISO-8859-1")
javadoc -d <your-output-directory> -bottom '<script src="//localhost:8290/divolte.js" defer async></script>' -subpackages .
```
