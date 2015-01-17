package io.divolte.examples.indexer;

import static org.elasticsearch.common.settings.ImmutableSettings.*;
import static org.elasticsearch.common.xcontent.XContentFactory.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.Doclet;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.RootDoc;

public class IndexingDoclet extends Doclet {

    private static final int ES_DEFAULT_PORT = 9300;
    private static final String ES_HOST_OPTION = "-eshost";
    private static final String ES_CLUSTERNAME_OPTION = "-esclustername";
    private static final String ES_PORT_OPTION = "-esport";
    private static final ImmutableSet<String> ES_OPTIONS = ImmutableSet.of(ES_PORT_OPTION, ES_CLUSTERNAME_OPTION, ES_HOST_OPTION);

    public static boolean start(RootDoc root) {
        final Settings esSettings = settingsBuilder().put("cluster.name", esClusterName(root.options())).build();
        final int esPort = esPort(root.options());
        final Set<String> esHosts = esHosts(root.options());

        System.err.println("Using ElasticSearch settings:");
        System.err.println("    cluster name: " + esClusterName(root.options()));
        System.err.println("    hosts:        " + esHosts);
        System.err.println("    port:         " + esPort);

        try (
            final TransportClient client = new TransportClient(esSettings)
            ) {
            for (String host : esHosts) {
                client.addTransportAddress(new InetSocketTransportAddress(host, esPort));
            }
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

    public static int optionLength(String option) {
        if (ES_OPTIONS.contains(option)) {
            return 2;
        } else {
            return 0;
        }
    }

    private static Set<String> esHosts(String[][] options) {
        final Set<String> result = Stream.of(options)
                                         .filter((o) -> o.length == 2 && o[0].equals(ES_HOST_OPTION))
                                         .map((o) -> o[1]).collect(Collectors.toSet());

        return result.isEmpty() ? ImmutableSet.of("localhost") : result;
    }

    private static int esPort(String[][] options) {
        return Stream.of(options)
                .filter((o) -> o.length == 2 && o[0].equals(ES_PORT_OPTION))
                .findFirst()
                .map((o) -> Integer.parseInt(o[1]))
                .orElse(ES_DEFAULT_PORT);
    }

    private static String esClusterName(String[][] options) {
        return Stream.of(options)
                .filter((o) -> o.length == 2 && o[0].equals(ES_CLUSTERNAME_OPTION))
                .findFirst()
                .map((o) -> o[1])
                .orElse("elasticsearch");
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
