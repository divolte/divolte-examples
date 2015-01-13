package io.divolte.examples.indexer;

import static org.elasticsearch.common.settings.ImmutableSettings.*;
import static org.elasticsearch.common.xcontent.XContentFactory.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import com.google.common.io.Resources;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.Doclet;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.RootDoc;

public class IndexingDoclet extends Doclet {

    private static final int ES_PORT = 9300;
    private static final String ES_HOST = "127.0.0.1";

    public static boolean start(RootDoc root) {
        final Settings esSettings = settingsBuilder().put("cluster.name", "elasticsearch_friso").build();
        try (
            @SuppressWarnings("resource")
            final Client client = new TransportClient(esSettings).addTransportAddress(new InetSocketTransportAddress(ES_HOST, ES_PORT))
            ) {

            deleteIndexIfExists(client);
            createIndex(client);

            System.err.println("Indexing...");
            final BulkRequestBuilder bulk = client.prepareBulk();

            for (ClassDoc clazz: root.classes()) {
                System.err.println("Adding: " + clazz.qualifiedName());
                bulk.add(
                        client.prepareIndex("javadoc", "java_type")
                              .setSource(builderFromClassDoc(clazz))
                        );
            }

            System.err.println("Sending documents to ElasticSearch...");
            BulkResponse response = bulk.get();
            if (response.hasFailures()) {
                System.err.println("Failed to index documents! Message:\n" + response.buildFailureMessage());
            }
        } catch (IOException ioe) {
            System.err.println("Error occured while indexing: " + ioe.getMessage());
        }

        System.err.println("Done.");
        return true;
    }

    private static void createIndex(final Client client) throws IOException {
        System.err.println("Creating 'javadoc' index.");
        client.admin().indices()
            .prepareCreate("javadoc")
            .setSettings(Resources.toString(Resources.getResource("settings.json"), StandardCharsets.UTF_8))
            .addMapping("java_type",
                    Resources.toString(Resources.getResource("mapping.json"), StandardCharsets.UTF_8))
                    .get();
    }

    private static void deleteIndexIfExists(final Client client) {
        if (client.admin().indices().prepareExists("javadoc").get().isExists()) {
            System.err.println("Deleting existing 'javadoc' index.");
            final boolean acked = client.admin().indices().prepareDelete("javadoc").get().isAcknowledged();
            System.err.println("Acknowledged: " + acked);
        }
    }

    private static XContentBuilder builderFromClassDoc(ClassDoc clazz) throws IOException {
        final String link = linkFromClass(clazz);
        final XContentBuilder builder = jsonBuilder();
        builder.startObject()
               .field("link", link)
               .field("type_name", clazz.simpleTypeName())
               .field("package", clazz.containingPackage().name())
               .field("comment", stripHtmlAndJavaDocTags(clazz.commentText()))
               .startArray("methods");
        for (MethodDoc md: clazz.methods()) {
            builder.startObject()
                   .field("name", md.name())
                   .field("comment", stripHtmlAndJavaDocTags(md.commentText()))
                   .endObject();
        }
        builder.endArray()
               .endObject();
        return builder;
    }

    private static String linkFromClass(ClassDoc clazz) {
        return clazz.containingPackage().name().replace('.', '/') + '/' + clazz.typeName() + ".html";
    }

    private static String stripHtmlAndJavaDocTags(String comment) {
        return Jsoup.clean(comment, Whitelist.none()).replaceAll("\\{.*?@[^\\s]+[\\s]+([^\\}]+)\\}", "$1");
    }
}
