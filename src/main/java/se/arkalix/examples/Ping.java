package se.arkalix.examples;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoToString;
import se.arkalix.dto.DtoWritableAs;

import java.time.Instant;
import java.util.Optional;

import static se.arkalix.dto.DtoEncoding.JSON;

/**
 * A so called Data Transfer Object (DTO) interface. Such an interface may
 * only contain getters (methods that return something other than "void"
 * and accept no parameters) and is used to automatically generate classes
 * for instantiating, encoding and decoding Java objects that satisfies the
 * interface.
 * <p>
 * This particular DTO interface causes the "PingDto" and "PingBuilder" classes
 * to be generated with support for reading and writing "PingDto" objects
 * from/to JSON. As the @DtoToString annotation is provided, the generated
 * "PingDto" class also gets a custom toString() implementation.
 */
@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
@DtoToString
@SuppressWarnings("unused")
interface Ping {
    /**
     * Any string.
     */
    String ping();

    /**
     * An optional identifier.
     */
    Optional<String> id();

    /**
     * Optional fields that carry anything else than arrays, lists or maps
     * must use the {@link Optional} type to indicate that they are indeed
     * optional. The mentioned collections can be empty, and as there is
     * typically no reason to distingusih empty collections from such that are
     * not provided (i.e. are null), null collections are treated as if being
     * empty. If this behavior is not desired, the collections can be
     * explicitly specified as being optional (e.g. using
     * Optional&lt;List&lt;String&gt;&gt; as field type). In this case, if a
     * given field is Optional.empty(), the collection was never provided. If,
     * on the other hand, the field has a value, a collection was specified (
     * even if it still may contain zero elements).
     * <p>
     * DTO interfaces may include other DTO interfaces, as long as also
     * they have the same @DtoReadableAs and @DtoWritableAs annotations with
     * the same specified encodings.
     * <p>
     * The complete list of built-in types that are supported in DTO
     * interfaces can be read in the
     * {@code se.arkalix.dto.DtoPropertyFactory} class in the
     * "kalix-processors" module.
     */
    Optional<Instant> timestamp();

    /**
     * Any number of default or static methods may be specified without having
     * any impact on the concrete fields that will be part of the generated DTO
     * classes.
     */
    default String timestampToString() {
        return timestamp().map(Instant::toString).orElse("null");
    }
}
