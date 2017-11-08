package com.divolte.kafka.streams;

import java.io.IOException;
import java.io.EOFException;
import java.io.InputStream;
import java.io.*;

import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericData;
import org.apache.avro.io.*;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.Schema;
import com.twitter.bijection.Injection;
import com.twitter.bijection.avro.GenericAvroCodecs;

public class AvroUtil {
    public static String transform(byte[] value) {
        String returnVal = "";
        try {
            Schema.Parser parser = new Schema.Parser();
            Schema schema = parser.parse(new File("src/main/resources/MyEventRecord.avsc"));
            GenericRecord avroRecord = new GenericData.Record(schema);
            returnVal = avroRecord.toString();

            Injection<GenericRecord, byte[]> recordInjection = GenericAvroCodecs.toBinary(schema);
            GenericRecord record = recordInjection.invert(value).get();

            returnVal = record.toString();
        } catch (Exception e) {
            String ex = e.toString();
        }

        return returnVal;
    }

}
