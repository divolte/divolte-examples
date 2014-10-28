HDFS_URI="hdfs://192.168.100.151:8020" IPYTHON=1 IPYTHON_OPTS="notebook" ~/dev/spark/bin/pyspark --jars /path/to/divolte-spark/target/scala-2.10/divolte-spark-assembly-0.1.jar

On CDH:
IPYTHON=1 IPYTHON_OPTS="notebook --ip=0.0.0.0" pyspark --jars /path/to/divolte-spark/target/scala-2.10/divolte-spark-assembly-0.1.jar --driver-class-path /path/to/divolte-spark/target/scala-2.10/divolte-spark-assembly-0.1.jar



HDFS_URI="hdfs://192.168.100.151:8020" ~/dev/spark/bin/spark-submit --jars ../../divolte-spark/target/scala-2.10/divolte-spark-assembly-0.1.jar pyspark_script_example.py

Same CDH caveat
