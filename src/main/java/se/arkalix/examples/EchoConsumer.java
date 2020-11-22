package se.arkalix.examples;

import se.arkalix.ArSystem;
import se.arkalix.core.plugin.HttpJsonCloudPlugin;
import se.arkalix.descriptor.EncodingDescriptor;
import se.arkalix.net.http.HttpMethod;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.consumer.HttpConsumer;
import se.arkalix.net.http.consumer.HttpConsumerRequest;
import se.arkalix.net.http.service.HttpService;
import se.arkalix.security.access.AccessPolicy;
import se.arkalix.security.identity.OwnedIdentity;
import se.arkalix.security.identity.TrustStore;
import se.arkalix.util.concurrent.Schedulers;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;

public class EchoConsumer {
    public static void main(final String[] args) {
        if (args.length != 4) {
            System.err.println("Usage: java -jar example.jar <keyStorePath> <trustStorePath> <serviceRegistryHostname> <serviceRegistryPort>");
            System.exit(1);
        }
        try {
            System.out.println("Running echo consumer ...");

            // Load owned system identity and truststore.
            final var password = new char[]{'1', '2', '3', '4', '5', '6'};
            final var identity = new OwnedIdentity.Loader()
                .keyPassword(password)
                .keyStorePath(Path.of(args[0]))
                .keyStorePassword(password)
                .load();
            final var trustStore = TrustStore.read(Path.of(args[1]), password);
            Arrays.fill(password, '\0');

            final var srSocketAddress = new InetSocketAddress(args[2], Integer.parseInt(args[3]));

            // Create Arrowhead system that automatically joins the local cloud
            // with a service registry available at `srSocketAddress`. For this
            // to work, the `identity` must contain an X.509 certificate that
            // is issued by the same cloud certificate as the service registry.
            // See https://arkalix.se/javadocs/kalix-base/se/arkalix/security/identity/package-summary.html
            // for more details.
            final var system = new ArSystem.Builder()
                // .identity(identity)
                // .trustStore(trustStore)
                .name("system-kalix-example-consumer")
                .insecure()
                .localPort(9002)
                .plugins(HttpJsonCloudPlugin.joinViaServiceRegistryAt(srSocketAddress))
                .build();

            // Providing a service is, as of Arrowhead 4.1.3, the only way to
            // registering a system, which is required for the system to be
            // able to consume services.
            system.provide(new HttpService()
                .name("kalix-example-consumer-service")
                .encodings(EncodingDescriptor.JSON)
                // .accessPolicy(AccessPolicy.token())
                .basePath("/example")

                .post("/pings", (request, response) ->
                    request.bodyAs(PingDto.class)
                        .map(body -> response
                            .status(HttpStatus.CREATED)
                            .body(body))))

                .ifSuccess(ignored -> System.out.println("Consumer service is being provided ..."))
                .ifFailure(Throwable.class, throwable -> {
                    System.err.println("Failed to provide consumer service");
                    throwable.printStackTrace(System.err);
                })
                .await();

            // HTTP POST using automatic service lookup.
            system.consume()
                .name("kalix-example-provider-service")
                .encodings(EncodingDescriptor.JSON)
                .oneUsing(HttpConsumer.factory())
                .flatMap(consumer -> consumer.send(new HttpConsumerRequest()
                    .method(HttpMethod.GET)
                    .uri("/example/pings/32")))
                .flatMap(response -> response.bodyAsIfSuccess(PingDto.class))
                .ifSuccess(ping -> {
                    System.out.println("GET /example/pings/32 result:");
                    System.out.println(ping);
                })
                .onFailure(throwable -> {
                    System.err.println("\nGET /example/pings/32 failed:");
                    throwable.printStackTrace();
                });

            // HTTP DELETE using automatic service lookup.
            system.consume()
                .name("kalix-example-provider-service")
                .encodings(EncodingDescriptor.JSON)
                .oneUsing(HttpConsumer.factory())
                .flatMap(consumer -> consumer.send(new HttpConsumerRequest()
                    .method(HttpMethod.DELETE)
                    .uri("/example/runtime")))
                .onResult(result -> {
                    System.err.println("\nDELETE /example/runtime result:");
                    result.ifSuccess(response -> System.err.println(response.status()));
                    result.ifFailure(Throwable::printStackTrace);

                    // Exit in 0.5 seconds.
                    Schedulers.fixed()
                        .schedule(Duration.ofMillis(500), () -> System.exit(0))
                        .onFailure(Throwable::printStackTrace);
                });

        }
        catch (final Throwable e) {
            e.printStackTrace();
        }
    }
}
