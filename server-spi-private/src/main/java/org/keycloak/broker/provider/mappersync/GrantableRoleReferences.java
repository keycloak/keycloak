/*
 * Copyright 2026 Andrzej Świętek
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.broker.provider.mappersync;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.keycloak.broker.provider.RoleGrantingIdentityProviderMapper;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.utils.StringUtil;

import org.jboss.logging.Logger;

/**
 * Role references that identity provider mappers implementing {@link RoleGrantingIdentityProviderMapper} keep in their
 * configuration beyond {@link org.keycloak.broker.provider.ConfigConstants#ROLE}.
 */
final class GrantableRoleReferences {

    private static final Logger LOG = Logger.getLogger(GrantableRoleReferences.class);

    private static final String KEPT_REFERENCES_WARNING = "Mapper %s on identity provider %s in realm %s keeps the role"
            + " references %s unchanged, because their renamed spelling names a different role. The mapper no longer"
            + " grants the renamed role. Correct the role references of the mapper by hand.";

    private record RoleGrantingMapper(IdentityProviderMapperModel model, RoleGrantingIdentityProviderMapper type) {
    }

    private record ConfigChange(String key, String previousValue, String newValue) {
    }

    /**
     * A reference left unchanged because {@code wouldBecome}, its spelling after the rename, names a different role.
     */
    private record KeptReference(String reference, String wouldBecome) {
    }

    private GrantableRoleReferences() {
    }

    /**
     * Rewrites the renamed role references of each role-granting mapper of the realm and saves the mappers whose
     * configuration changed; warns about each mapper with a reference that cannot be renamed and leaves that reference
     * unchanged.
     */
    static void rewrite(KeycloakSession session, RealmModel realm, Function<String, RoleReferenceRename> rename,
                        ConfigSynchronizer<?> synchronizer) {
        roleGrantingMappers(session).forEach(mapper -> rewriteMapper(session, realm, mapper, rename, synchronizer));
    }

    private static List<RoleGrantingMapper> roleGrantingMappers(KeycloakSession session) {
        return session.identityProviders().getMappersStream()
                .flatMap(model -> roleGrantingMapper(session, model).stream())
                .toList();
    }

    private static Optional<RoleGrantingMapper> roleGrantingMapper(KeycloakSession session, IdentityProviderMapperModel model) {
        return RoleGrantingIdentityProviderMapper.of(session.getKeycloakSessionFactory(), model)
                .map(type -> new RoleGrantingMapper(model, type));
    }

    private static void rewriteMapper(KeycloakSession session, RealmModel realm, RoleGrantingMapper mapper,
                                      Function<String, RoleReferenceRename> rename, ConfigSynchronizer<?> synchronizer) {
        Map<String, String> config = mapper.model().getConfig();
        Map<String, RoleReferenceRename> renames = renames(mapper, rename);
        List<KeptReference> kept = keptReferences(renames);
        if (!kept.isEmpty()) {
            warnKept(realm, mapper.model(), kept);
        }
        Map<String, String> rewrittenConfig = mapper.type()
                .withRewrittenRoleReferences(config, reference -> renamed(renames, rename, reference));
        List<ConfigChange> changes = configChanges(config, rewrittenConfig);
        if (!changes.isEmpty()) {
            save(session, realm, mapper.model(), changes, synchronizer);
        }
    }

    private static Map<String, RoleReferenceRename> renames(RoleGrantingMapper mapper, Function<String, RoleReferenceRename> rename) {
        return mapper.type().getGrantableRoleReferences(mapper.model().getConfig()).stream()
                .collect(Collectors.toUnmodifiableMap(Function.identity(), rename));
    }

    /**
     * The reference to keep; {@code rename} is applied only to references the mapper does not grant, such as those in
     * rows without a field value.
     */
    private static String renamed(Map<String, RoleReferenceRename> renames, Function<String, RoleReferenceRename> rename,
                                  String reference) {
        return Optional.ofNullable(renames.get(reference)).orElseGet(() -> rename.apply(reference)).applyTo(reference);
    }

    private static List<KeptReference> keptReferences(Map<String, RoleReferenceRename> renames) {
        return renames.entrySet().stream()
                .flatMap(entry -> keptReference(entry.getKey(), entry.getValue()).stream())
                .sorted(Comparator.comparing(KeptReference::reference))
                .toList();
    }

    private static Optional<KeptReference> keptReference(String reference, RoleReferenceRename rename) {
        return rename instanceof RoleReferenceRename.Ambiguous ambiguous
                ? Optional.of(new KeptReference(reference, ambiguous.newReference()))
                : Optional.empty();
    }

    private static void warnKept(RealmModel realm, IdentityProviderMapperModel mapper, List<KeptReference> kept) {
        LOG.warnf(KEPT_REFERENCES_WARNING, quoted(mapper.getName()), quoted(mapper.getIdentityProviderAlias()),
                quoted(realm.getName()), describeKept(kept));
    }

    private static String describeKept(List<KeptReference> kept) {
        return kept.stream().map(GrantableRoleReferences::describeKept).collect(Collectors.joining(", "));
    }

    private static String describeKept(KeptReference kept) {
        return quoted(kept.reference()) + " (would become " + quoted(kept.wouldBecome()) + ")";
    }

    private static String quoted(String value) {
        return "'" + StringUtil.sanitizeSpacesAndQuotes(Optional.ofNullable(value).orElse(""), '\'') + "'";
    }

    private static List<ConfigChange> configChanges(Map<String, String> config, Map<String, String> rewrittenConfig) {
        return rewrittenConfig.entrySet().stream()
                .filter(entry -> !Objects.equals(entry.getValue(), config.get(entry.getKey())))
                .map(entry -> new ConfigChange(entry.getKey(), config.get(entry.getKey()), entry.getValue()))
                .toList();
    }

    private static void save(KeycloakSession session, RealmModel realm, IdentityProviderMapperModel mapper,
                             List<ConfigChange> changes, ConfigSynchronizer<?> synchronizer) {
        changes.forEach(configChange -> apply(realm, mapper, configChange, synchronizer));
        session.identityProviders().updateMapper(mapper);
    }

    private static void apply(RealmModel realm, IdentityProviderMapperModel mapper, ConfigChange configChange,
                              ConfigSynchronizer<?> synchronizer) {
        mapper.getConfig().put(configChange.key(), configChange.newValue());
        synchronizer.logEventProcessed(configChange.key(), configChange.previousValue(), configChange.newValue(), realm.getName(),
                mapper.getName(), mapper.getIdentityProviderAlias());
    }
}
