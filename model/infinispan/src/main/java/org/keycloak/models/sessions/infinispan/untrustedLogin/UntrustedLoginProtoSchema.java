package org.keycloak.models.sessions.infinispan.untrustedlogin;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.ProtoSchema;

/**
 * Declares the Protobuf schema for this module's cache types. The protostream-processor
 * annotation processor (wired in pom.xml) generates:
 *   - untrusted-login.proto (the .proto file, written under target/classes/proto/)
 *   - a *_Marshaller class per included type
 *   - UntrustedLoginProtoSchemaImpl, the concrete SerializationContextInitializer
 *
 * As of Keycloak 26.2 (see keycloak/keycloak#38421, "Load all ProtoSchemas from the
 * classpath"), Keycloak auto-discovers every SerializationContextInitializer on the
 * classpath at startup - so UntrustedLoginProtoSchemaImpl gets picked up automatically
 * once this JAR is deployed as a provider. No manual SerializationContext registration
 * call is needed, and none should be added - a manual registration would double-register
 * the schema and fail.
 *
 * VERSION FLOOR: this auto-discovery mechanism does not exist before 26.2. On earlier
 * Keycloak versions there is no supported extension point for custom cache marshalling
 * (confirmed by keycloak/keycloak#34971, where a maintainer confirmed no workaround
 * existed) - targeting < 26.2 would require either vendoring the cache as a Java-serialization
 * cache (slower, and Keycloak's default marshaller config for new caches may reject it)
 * or waiting for backport. Recommend simply requiring 26.2+ for this feature.
 */
@ProtoSchema(
        includeClasses = {
                DeviceHistoryEntry.class,
                UserDeviceHistory.class
        },
        schemaFileName = "untrusted-login.proto",
        schemaFilePath = "proto/",
        schemaPackageName = "keycloak.untrustedlogin"
)
public interface UntrustedLoginProtoSchema extends GeneratedSchema {
}