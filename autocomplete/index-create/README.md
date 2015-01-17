Javadoc doclet that indexes docs into a locally running elasticsearch instance.

Under construction...

To create docs:

  javadoc -d /path/to/autocomplete/webapp/static/javadoc -bottom '<script src="//localhost:8290/divolte.js" defer async></script>' -subpackages .

To index docs:

  javadoc -docletpath /path/to/autocomplete/index-create/target/index-create-0.2-SNAPSHOT-jar-with-dependencies.jar -doclet io.divolte.examples.indexer.IndexingDoclet -subpackages .
