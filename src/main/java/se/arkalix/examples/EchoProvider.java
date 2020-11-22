package se.arkalix.examples;

import se.arkalix.ArSystem;
import se.arkalix.core.plugin.HttpJsonCloudPlugin;
import se.arkalix.descriptor.EncodingDescriptor;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.service.HttpService;
import se.arkalix.security.access.AccessPolicy;
import se.arkalix.security.identity.OwnedIdentity;
import se.arkalix.security.identity.TrustStore;
import se.arkalix.util.concurrent.Future;
import se.arkalix.util.concurrent.Schedulers;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

public class EchoProvider {
    public static void main(final String[] args) {
        if (args.length != 4) {
            System.err.println("Usage: java -jar example.jar <keyStorePath> <trustStorePath> <serviceRegistryHostname> <serviceRegistryPort>");
            System.exit(1);
        }
        try {
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
            // System.out.println("Socket address: " + srSocketAddress);
            
            // Create Arrowhead system that automatically joins the local cloud
            // with a service registry available at `srSocketAddress`. For this
            // to work, the `identity` must contain an X.509 certificate that
            // is issued by the same cloud certificate as the service registry.
            // See https://arkalix.se/javadocs/kalix-base/se/arkalix/security/identity/package-summary.html
            // for more details.
            final var system = new ArSystem.Builder()
                // .identity(identity)
                // .trustStore(trustStore)
                .name("system-kalix-example-provider")
                .localPort(9001)
                .plugins(HttpJsonCloudPlugin.joinViaServiceRegistryAt(srSocketAddress))
                .insecure()
                .build();

            // Cause the Arrowhead system to provide an HTTP service, which
            // will be automatically registered in the service registry.
            system.provide(new HttpService()

                // Mandatory service configuration details.
                .name("kalix-example-provider-service")
                .encodings(EncodingDescriptor.JSON)
                // .accessPolicy(AccessPolicy.token())
                .basePath("/example")

                // HTTP GET endpoint that uses a DTO class, "Ping", which is
                // defined in this folder. This particular endpoint ignores the
                // body of the request, if any.
                .get("/pings/#id", (request, response) -> {
                    response
                        .status(HttpStatus.OK)
                        .body(new PingBuilder()
                            .ping("pong")
                            // #id is the first path parameter and has index 0.
                            .id(request.pathParameter(0))
                            .timestamp(Instant.now())
                            .build());

                    return Future.done();
                })

                // HTTP POST endpoint that accepts a "PingData" object and
                // returns it to its sender.
                .post("/pings", (request, response) ->
                    request.bodyAs(PingDto.class)
                        .map(body -> response
                            .status(HttpStatus.CREATED)
                            .body(body)))

                // HTTP POST endpoint that echoes back whatever body is in the
                // requests it receives. Note that since EncodingDescriptor.JSON
                // was specified via the ".encodings()" method above, only
                // requests that claim to carry JSON bodies, or have no bodies
                // at all, are accepted and reach the endpoints specified here.
                .post("/echoes", (request, response) ->
                    request.bodyAsString()
                        .map(body -> response
                            .status(HttpStatus.CREATED)
                            .body(body)))

                // HTTP DELETE endpoint that causes the application to exit.
                .delete("/runtime", (request, response) -> {
                    response.status(HttpStatus.NO_CONTENT);

                    // Exit in 0.5 seconds.
                    Schedulers.fixed()
                        .schedule(Duration.ofMillis(500), () -> System.exit(0))
                        .onFailure(Throwable::printStackTrace);

                    return Future.done();
                }))

                .onFailure(Throwable::printStackTrace);

            System.out.println("Echo provider running ...");
        }
        catch (final Throwable e) {
            e.printStackTrace();
        }
    }
}
