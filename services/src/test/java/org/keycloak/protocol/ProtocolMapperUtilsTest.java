/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
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

package org.keycloak.protocol;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.models.utils.UserModelDelegate;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Alexander Schwartz
 */
public class ProtocolMapperUtilsTest {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Builds a persisted {@link ProtocolMapperModel} (has an id). */
    private static ProtocolMapperModel existingModel(String id, String protocol, String name,
                                                      String mapperType, Map<String, String> config) {
        ProtocolMapperRepresentation rep = incomingRep(protocol, name, mapperType, config);
        rep.setId(id);
        return RepresentationToModel.toModel(rep);
    }

    /** Builds an incoming {@link ProtocolMapperRepresentation} (no id — as sent by a client). */
    private static ProtocolMapperRepresentation incomingRep(String protocol, String name,
                                                              String mapperType, Map<String, String> config) {
        ProtocolMapperRepresentation r = new ProtocolMapperRepresentation();
        r.setProtocol(protocol);
        r.setName(name);
        r.setProtocolMapper(mapperType);
        r.setConfig(config);
        return r;
    }

    private static Set<String> changed(Stream<ProtocolMapperModel> existing,
                                        List<ProtocolMapperRepresentation> incoming) {
        return ProtocolMapperUtils.getChangedOrDeletedMapperTypes(existing, incoming);
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    public void noExistingMappersNewMappersProvided() {
        // No mappers in DB, two brand-new ones sent → both types must be checked.
        List<ProtocolMapperRepresentation> incoming = List.of(
                incomingRep("oidc", "email", "oidc-usermodel-property-mapper", Map.of("user.attribute", "email")),
                incomingRep("oidc", "roles", "oidc-role-name-mapper", Map.of())
        );

        Set<String> result = changed(Stream.empty(), incoming);

        Assert.assertEquals(Set.of("oidc-usermodel-property-mapper", "oidc-role-name-mapper"), result);
    }

    @Test
    public void noChangedMappers() {
        // All incoming mappers are byte-for-byte identical to what is in the DB → nothing to check.
        // The incoming representation carries the stored id, as a real client does after round-tripping.
        ProtocolMapperModel existing = existingModel("id-1", "oidc", "email",
                "oidc-usermodel-property-mapper", Map.of("user.attribute", "email"));

        ProtocolMapperRepresentation incoming = incomingRep("oidc", "email",
                "oidc-usermodel-property-mapper", Map.of("user.attribute", "email"));
        incoming.setId("id-1");

        Set<String> result = changed(Stream.of(existing), List.of(incoming));

        Assert.assertTrue("Expected no changed mapper types", result.isEmpty());
    }

    @Test
    public void mixOfChangedAndUnchangedMappers() {
        // Two existing mappers; one is sent back unchanged (with its stored id), one has a config value modified.
        ProtocolMapperModel existingEmail = existingModel("id-1", "oidc", "email",
                "oidc-usermodel-property-mapper", Map.of("user.attribute", "email"));
        ProtocolMapperModel existingRoles = existingModel("id-2", "oidc", "roles",
                "oidc-role-name-mapper", Map.of("role", "admin"));

        ProtocolMapperRepresentation incomingEmail = incomingRep("oidc", "email",
                "oidc-usermodel-property-mapper", Map.of("user.attribute", "email"));
        incomingEmail.setId("id-1"); // unchanged — carries stored id

        List<ProtocolMapperRepresentation> incoming = List.of(
                incomingEmail,
                // changed: config value differs
                incomingRep("oidc", "roles", "oidc-role-name-mapper", Map.of("role", "superadmin"))
        );

        Set<String> result = changed(Stream.of(existingEmail, existingRoles), incoming);

        Assert.assertEquals(Set.of("oidc-role-name-mapper"), result);
    }

    @Test
    public void mixOfDeletedChangedUnchangedAndNew() {
        // Existing: mapper-A (unchanged), mapper-B (to be changed), mapper-C (to be deleted).
        // Incoming: mapper-A (unchanged, carries stored id), mapper-B (modified config), mapper-D (new).
        ProtocolMapperModel existingA = existingModel("id-a", "oidc", "mapper-a", "type-a", Map.of("k", "v"));
        ProtocolMapperModel existingB = existingModel("id-b", "oidc", "mapper-b", "type-b", Map.of("k", "old"));
        ProtocolMapperModel existingC = existingModel("id-c", "oidc", "mapper-c", "type-c", Map.of());

        ProtocolMapperRepresentation incomingA = incomingRep("oidc", "mapper-a", "type-a", Map.of("k", "v"));
        incomingA.setId("id-a"); // unchanged — carries stored id

        List<ProtocolMapperRepresentation> incoming = List.of(
                incomingA,
                incomingRep("oidc", "mapper-b", "type-b", Map.of("k", "new")),    // changed (no id — treated as change)
                incomingRep("oidc", "mapper-d", "type-d", Map.of())               // new
        );

        Set<String> result = changed(Stream.of(existingA, existingB, existingC), incoming);

        // type-a is unchanged → not included; type-b changed, type-c deleted, type-d new → all included
        Assert.assertEquals(Set.of("type-b", "type-c", "type-d"), result);
    }

    @Test
    public void allMappersDeleted() {
        // Two existing mappers, empty incoming list → both deletions must be checked.
        ProtocolMapperModel existing1 = existingModel("id-1", "oidc", "mapper-1", "type-x", Map.of());
        ProtocolMapperModel existing2 = existingModel("id-2", "oidc", "mapper-2", "type-y", Map.of());

        Set<String> result = changed(Stream.of(existing1, existing2), Collections.emptyList());

        Assert.assertEquals(Set.of("type-x", "type-y"), result);
    }

    @Test
    public void twoMappersWithSameProviderIdBothUnchanged() {
        // Two mappers share the same providerID but have different names; neither is changed.
        // Both carry their stored ids as a real client would after round-tripping.
        ProtocolMapperModel existing1 = existingModel("id-1", "oidc", "attr-email",
                "oidc-usermodel-property-mapper", Map.of("user.attribute", "email"));
        ProtocolMapperModel existing2 = existingModel("id-2", "oidc", "attr-phone",
                "oidc-usermodel-property-mapper", Map.of("user.attribute", "phone"));

        ProtocolMapperRepresentation incomingEmail = incomingRep("oidc", "attr-email",
                "oidc-usermodel-property-mapper", Map.of("user.attribute", "email"));
        incomingEmail.setId("id-1");
        ProtocolMapperRepresentation incomingPhone = incomingRep("oidc", "attr-phone",
                "oidc-usermodel-property-mapper", Map.of("user.attribute", "phone"));
        incomingPhone.setId("id-2");

        Set<String> result = changed(Stream.of(existing1, existing2), List.of(incomingEmail, incomingPhone));

        Assert.assertTrue("Expected no changed mapper types when both are unchanged", result.isEmpty());
    }

    @Test
    public void twoMappersWithSameProviderIdOneChangedOneUnchanged() {
        // Two mappers share the same providerID; one has a modified config (no id — treated as change),
        // one is unchanged and carries its stored id.
        ProtocolMapperModel existing1 = existingModel("id-1", "oidc", "attr-email",
                "oidc-usermodel-property-mapper", Map.of("user.attribute", "email"));
        ProtocolMapperModel existing2 = existingModel("id-2", "oidc", "attr-phone",
                "oidc-usermodel-property-mapper", Map.of("user.attribute", "phone"));

        ProtocolMapperRepresentation incomingPhone = incomingRep("oidc", "attr-phone",
                "oidc-usermodel-property-mapper", Map.of("user.attribute", "phone"));
        incomingPhone.setId("id-2"); // unchanged — carries stored id

        List<ProtocolMapperRepresentation> incoming = List.of(
                incomingRep("oidc", "attr-email", "oidc-usermodel-property-mapper", Map.of("user.attribute", "EMAIL")), // changed
                incomingPhone
        );

        Set<String> result = changed(Stream.of(existing1, existing2), incoming);

        Assert.assertEquals(Set.of("oidc-usermodel-property-mapper"), result);
    }

    @Test
    public void twoMappersWithSameProviderIdBothChanged() {
        // Two mappers share the same providerID and both have modified configs (neither carries an id).
        ProtocolMapperModel existing1 = existingModel("id-1", "oidc", "attr-email",
                "oidc-usermodel-property-mapper", Map.of("user.attribute", "email"));
        ProtocolMapperModel existing2 = existingModel("id-2", "oidc", "attr-phone",
                "oidc-usermodel-property-mapper", Map.of("user.attribute", "phone"));

        List<ProtocolMapperRepresentation> incoming = List.of(
                incomingRep("oidc", "attr-email", "oidc-usermodel-property-mapper", Map.of("user.attribute", "EMAIL")), // changed
                incomingRep("oidc", "attr-phone", "oidc-usermodel-property-mapper", Map.of("user.attribute", "PHONE"))  // changed
        );

        Set<String> result = changed(Stream.of(existing1, existing2), incoming);

        Assert.assertEquals(Set.of("oidc-usermodel-property-mapper"), result);
    }

    @Test
    public void incomingMapperWithNullIdMatchingByNameIsTreatedAsChanged() {
        // Existing mapper has a stored id. Incoming representation has the same protocol/name/type/config
        // but carries no id (null). The id mismatch must be treated as a change so that the type is
        // checked — this prevents a second mapper added by name alone from slipping through unchecked.
        ProtocolMapperModel existing = existingModel("id-1", "oidc", "email",
                "oidc-usermodel-property-mapper", Map.of("user.attribute", "email"));

        // incomingRep produces a representation with no id set
        ProtocolMapperRepresentation incoming = incomingRep("oidc", "email",
                "oidc-usermodel-property-mapper", Map.of("user.attribute", "email"));

        Set<String> result = changed(Stream.of(existing), List.of(incoming));

        Assert.assertEquals(Set.of("oidc-usermodel-property-mapper"), result);
    }

    @Test
    public void incomingMapperWithMismatchedIdIsTreatedAsChanged() {
        // Incoming representation carries an id that differs from the stored one. This is also a change.
        ProtocolMapperModel existing = existingModel("id-original", "oidc", "email",
                "oidc-usermodel-property-mapper", Map.of("user.attribute", "email"));

        ProtocolMapperRepresentation incoming = incomingRep("oidc", "email",
                "oidc-usermodel-property-mapper", Map.of("user.attribute", "email"));
        incoming.setId("id-different");

        Set<String> result = changed(Stream.of(existing), List.of(incoming));

        Assert.assertEquals(Set.of("oidc-usermodel-property-mapper"), result);
    }

    @Test
    public void incomingMapperWithCorrectIdAndNoOtherChangesIsNotTreatedAsChanged() {
        // Incoming representation carries the same id as stored and all fields are identical — no change.
        ProtocolMapperModel existing = existingModel("id-1", "oidc", "email",
                "oidc-usermodel-property-mapper", Map.of("user.attribute", "email"));

        ProtocolMapperRepresentation incoming = incomingRep("oidc", "email",
                "oidc-usermodel-property-mapper", Map.of("user.attribute", "email"));
        incoming.setId("id-1");

        Set<String> result = changed(Stream.of(existing), List.of(incoming));

        Assert.assertTrue("Expected no changed mapper types when id and all fields match", result.isEmpty());
    }

    @Test
    public void providerTypeChangedIncludesBothOldAndNewTypes() {
        // Existing mapper has provider "type-old"; incoming request swaps it to "type-new"
        // (same protocol/name key). Both types must appear in the result so that the
        // delegated-admin permission check covers the removal of the old type as well.
        ProtocolMapperModel existing = existingModel("id-1", "oidc", "my-mapper", "type-old", Map.of());

        ProtocolMapperRepresentation incoming = incomingRep("oidc", "my-mapper", "type-new", Map.of());

        Set<String> result = changed(Stream.of(existing), List.of(incoming));

        Assert.assertEquals(Set.of("type-old", "type-new"), result);
    }

    // -------------------------------------------------------------------------
    // Property-by-property change detection
    // -------------------------------------------------------------------------

    private static final String TYPE = "oidc-usermodel-property-mapper";

    private static ProtocolMapperModel baseExisting() {
        return existingModel("id-1", "oidc", "email", TYPE, Map.of("a", "1", "b", "2"));
    }

    private static ProtocolMapperRepresentation baseIncoming() {
        ProtocolMapperRepresentation r = incomingRep("oidc", "email", TYPE, Map.of("a", "1", "b", "2"));
        r.setId("id-1");
        return r;
    }

    /** Sets a field via reflection; needed as ProtocolMapperModel has no setters for the consent fields. */
    private static void setField(Object target, Class<?> declaringClass, String field, Object value) {
        try {
            java.lang.reflect.Field f = declaringClass.getDeclaredField(field);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    public void baselineWithMultipleConfigEntriesIsUnchanged() {
        Set<String> result = changed(Stream.of(baseExisting()), List.of(baseIncoming()));

        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void idChangedIsDetected() {
        ProtocolMapperRepresentation incoming = baseIncoming();
        incoming.setId("id-other");

        Assert.assertEquals(Set.of(TYPE), changed(Stream.of(baseExisting()), List.of(incoming)));
    }

    @Test
    public void nameChangedIsDetectedAsDeletedAndNew() {
        // The name is part of the identity key: the old mapper is deleted and a new one is added.
        ProtocolMapperRepresentation incoming = baseIncoming();
        incoming.setName("email-renamed");

        Assert.assertEquals(Set.of(TYPE), changed(Stream.of(baseExisting()), List.of(incoming)));
    }

    @Test
    public void nameChangedWithDifferentTypeIncludesOldAndNewTypes() {
        ProtocolMapperRepresentation incoming = baseIncoming();
        incoming.setName("email-renamed");
        incoming.setProtocolMapper("type-new");

        Assert.assertEquals(Set.of(TYPE, "type-new"), changed(Stream.of(baseExisting()), List.of(incoming)));
    }

    @Test
    public void protocolChangedIsDetectedAsDeletedAndNew() {
        // The protocol is part of the identity key: the old mapper is deleted and a new one is added.
        ProtocolMapperRepresentation incoming = baseIncoming();
        incoming.setProtocol("saml");

        Assert.assertEquals(Set.of(TYPE), changed(Stream.of(baseExisting()), List.of(incoming)));
    }

    @Test
    public void protocolChangedWithDifferentTypeIncludesOldAndNewTypes() {
        ProtocolMapperRepresentation incoming = baseIncoming();
        incoming.setProtocol("saml");
        incoming.setProtocolMapper("type-new");

        Assert.assertEquals(Set.of(TYPE, "type-new"), changed(Stream.of(baseExisting()), List.of(incoming)));
    }

    @Test
    public void protocolMapperChangedIsDetected() {
        ProtocolMapperRepresentation incoming = baseIncoming();
        incoming.setProtocolMapper("type-new");

        Assert.assertEquals(Set.of(TYPE, "type-new"), changed(Stream.of(baseExisting()), List.of(incoming)));
    }

    @Test
    public void consentRequiredChangedIsDetected() {
        // The stored mapper has consentRequired=true, the incoming representation does not.
        ProtocolMapperModel existing = baseExisting();
        setField(existing, ProtocolMapperModel.class, "consentRequired", true);

        Assert.assertEquals(Set.of(TYPE), changed(Stream.of(existing), List.of(baseIncoming())));
    }

    @Test
    public void consentTextChangedIsDetected() {
        // The stored mapper has a consentText, the incoming representation does not.
        ProtocolMapperModel existing = baseExisting();
        setField(existing, ProtocolMapperModel.class, "consentText", "some text");

        Assert.assertEquals(Set.of(TYPE), changed(Stream.of(existing), List.of(baseIncoming())));
    }

    @Test
    public void consentFieldsOnIncomingRepresentationAreIgnored() {
        // The consent fields on the representation are deprecated and not copied by
        // RepresentationToModel.toModel(), so they do not influence change detection.
        ProtocolMapperRepresentation incoming = baseIncoming();
        setField(incoming, ProtocolMapperRepresentation.class, "consentRequired", true);
        setField(incoming, ProtocolMapperRepresentation.class, "consentText", "some text");

        Assert.assertTrue(changed(Stream.of(baseExisting()), List.of(incoming)).isEmpty());
    }

    @Test
    public void configEntryAddedIsDetected() {
        ProtocolMapperRepresentation incoming = baseIncoming();
        incoming.setConfig(Map.of("a", "1", "b", "2", "c", "3"));

        Assert.assertEquals(Set.of(TYPE), changed(Stream.of(baseExisting()), List.of(incoming)));
    }

    @Test
    public void configEntryRemovedIsDetected() {
        ProtocolMapperRepresentation incoming = baseIncoming();
        incoming.setConfig(Map.of("a", "1"));

        Assert.assertEquals(Set.of(TYPE), changed(Stream.of(baseExisting()), List.of(incoming)));
    }

    @Test
    public void configEntryValueUpdatedIsDetected() {
        ProtocolMapperRepresentation incoming = baseIncoming();
        incoming.setConfig(Map.of("a", "1", "b", "changed"));

        Assert.assertEquals(Set.of(TYPE), changed(Stream.of(baseExisting()), List.of(incoming)));
    }

    @Test
    public void configEntryKeyReplacedIsDetected() {
        // Same number of entries and same values, but one key is different.
        ProtocolMapperRepresentation incoming = baseIncoming();
        incoming.setConfig(Map.of("a", "1", "other", "2"));

        Assert.assertEquals(Set.of(TYPE), changed(Stream.of(baseExisting()), List.of(incoming)));
    }

    @Test
    public void allConfigEntriesRemovedIsDetected() {
        ProtocolMapperRepresentation incoming = baseIncoming();
        incoming.setConfig(Map.of());

        Assert.assertEquals(Set.of(TYPE), changed(Stream.of(baseExisting()), List.of(incoming)));
    }

    @Test
    public void configEntryAddedToEmptyConfigIsDetected() {
        ProtocolMapperModel existing = existingModel("id-1", "oidc", "email", TYPE, Map.of());
        ProtocolMapperRepresentation incoming = incomingRep("oidc", "email", TYPE, Map.of("a", "1"));
        incoming.setId("id-1");

        Assert.assertEquals(Set.of(TYPE), changed(Stream.of(existing), List.of(incoming)));
    }

    @Test
    public void configEntryWithEmptyOrNullValueIsIgnored() {
        // RepresentationToModel.toModel() drops empty/null config values, so adding such an entry is not a change.
        Map<String, String> config = new HashMap<>();
        config.put("a", "1");
        config.put("b", "2");
        config.put("empty", "");
        config.put("nullValue", null);
        ProtocolMapperRepresentation incoming = baseIncoming();
        incoming.setConfig(config);

        Assert.assertTrue(changed(Stream.of(baseExisting()), List.of(incoming)).isEmpty());
    }

    @Test
    public void configEntryValueChangedToEmptyIsDetected() {
        // Setting an existing value to "" effectively removes the entry, which is a change.
        ProtocolMapperRepresentation incoming = baseIncoming();
        incoming.setConfig(Map.of("a", "1", "b", ""));

        Assert.assertEquals(Set.of(TYPE), changed(Stream.of(baseExisting()), List.of(incoming)));
    }

    @Test
    public void nullIncomingConfigIsDetectedAsChange() {
        ProtocolMapperRepresentation incoming = baseIncoming();
        incoming.setConfig(null);

        Assert.assertEquals(Set.of(TYPE), changed(Stream.of(baseExisting()), List.of(incoming)));
    }

    @Test
    public void getUserModelValue() {
        UserModel useModel = new UserModelDelegate(null) {
            @Override
            public String getUsername() {
                return "theo";
            }

            @Override
            public boolean isEmailVerified() {
                return true;
            }
        };

        Assert.assertEquals("theo", ProtocolMapperUtils.getUserModelValue(useModel, "username"));
        Assert.assertEquals("theo", ProtocolMapperUtils.getUserModelValue(useModel, "Username"));
        Assert.assertEquals("true", ProtocolMapperUtils.getUserModelValue(useModel, "emailVerified"));
        Assert.assertNull(ProtocolMapperUtils.getUserModelValue(useModel, "emailverified"));
        Assert.assertNull(ProtocolMapperUtils.getUserModelValue(useModel, "unknown"));
    }
}
