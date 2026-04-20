/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.admin.internal.openapi;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.keycloak.representations.admin.v2.validation.ClientSecretNotBlank;
import org.keycloak.representations.admin.v2.validation.CreateClient;
import org.keycloak.representations.admin.v2.validation.PatchClient;
import org.keycloak.representations.admin.v2.validation.PutClient;
import org.keycloak.representations.admin.v2.validation.UuidUnmodified;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.hibernate.validator.constraints.URL;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ValidationAnnotationScanner}.
 * <p>
 * Note: Standard Jakarta Validation annotations ({@code @NotBlank}, {@code @Size}, {@code @Pattern}, etc.)
 * are handled by SmallRye OpenAPI's built-in {@code BeanValidationScanner} for schema properties.
 * This scanner only handles:
 * <ul>
 *   <li>Human-readable validation descriptions</li>
 *   <li>{@code @URL} annotation (not supported by SmallRye)</li>
 *   <li>Custom Keycloak validation annotations</li>
 *   <li>Validation group context</li>
 * </ul>
 */
public class ValidationAnnotationScannerTest {

    private ValidationAnnotationScanner scanner;
    private Index index;

    @BeforeEach
    public void setUp() throws IOException {
        index = createIndex(
                TestRepresentation.class,
                TestWithGroups.class,
                TestWithClassLevelConstraint.class,
                TestWithClassLevelConstraintAndGroups.class,
                TestWithClassLevelConstraintAndMessage.class,
                CreateClient.class,
                PutClient.class,
                PatchClient.class,
                NotBlank.class,
                NotNull.class,
                NotEmpty.class,
                Size.class,
                Pattern.class,
                Min.class,
                Max.class,
                URL.class,
                ClientSecretNotBlank.class,
                UuidUnmodified.class
        );
        scanner = new ValidationAnnotationScanner(index);
    }

    @Test
    public void applySchemaProperties_urlSetsFormatUri() {
        ClassInfo classInfo = index.getClassByName(TestRepresentation.class);
        Schema schema = OASFactory.createSchema();

        scanner.applySchemaProperties(classInfo, "urlField", schema);

        assertEquals("uri", schema.getFormat());
    }

    @Test
    public void applySchemaProperties_urlDoesNotOverwriteExistingFormat() {
        ClassInfo classInfo = index.getClassByName(TestRepresentation.class);
        Schema schema = OASFactory.createSchema();
        schema.setFormat("custom-format");

        scanner.applySchemaProperties(classInfo, "urlField", schema);

        assertEquals("custom-format", schema.getFormat());
    }

    @Test
    public void applySchemaProperties_urlOnTypeArgumentSetsItemsFormatUri() {
        ClassInfo classInfo = index.getClassByName(TestRepresentation.class);
        Schema schema = OASFactory.createSchema();
        Schema itemsSchema = OASFactory.createSchema();
        schema.setItems(itemsSchema);

        scanner.applySchemaProperties(classInfo, "urlSet", schema);

        assertEquals("uri", itemsSchema.getFormat());
    }

    @Test
    public void applySchemaProperties_urlOnTypeArgumentDoesNotOverwriteExistingFormat() {
        ClassInfo classInfo = index.getClassByName(TestRepresentation.class);
        Schema schema = OASFactory.createSchema();
        Schema itemsSchema = OASFactory.createSchema();
        itemsSchema.setFormat("custom-format");
        schema.setItems(itemsSchema);

        scanner.applySchemaProperties(classInfo, "urlSet", schema);

        assertEquals("custom-format", itemsSchema.getFormat());
    }

    @Test
    public void buildDescription_notBlankConstraint() {
        ClassInfo classInfo = index.getClassByName(TestRepresentation.class);

        String description = scanner.buildDescription(classInfo, "notBlankField");

        assertTrue(description.contains("must not be blank"));
    }

    @Test
    public void buildDescription_notNullConstraint() {
        ClassInfo classInfo = index.getClassByName(TestRepresentation.class);

        String description = scanner.buildDescription(classInfo, "notNullField");

        assertTrue(description.contains("must not be null"));
    }

    @Test
    public void buildDescription_sizeConstraint() {
        ClassInfo classInfo = index.getClassByName(TestRepresentation.class);

        String description = scanner.buildDescription(classInfo, "sizedString");

        // Standard message: "size must be between {min} and {max}"
        assertTrue(description.contains("size must be between 5 and 100"));
    }

    @Test
    public void buildDescription_patternConstraint() {
        ClassInfo classInfo = index.getClassByName(TestRepresentation.class);

        String description = scanner.buildDescription(classInfo, "patternField");

        // Standard message: "must match \"{regexp}\""
        assertTrue(description.contains("must match \"[a-z]+\""));
    }

    @Test
    public void buildDescription_minMaxConstraints() {
        ClassInfo classInfo = index.getClassByName(TestRepresentation.class);

        String minDescription = scanner.buildDescription(classInfo, "minValue");
        String maxDescription = scanner.buildDescription(classInfo, "maxValue");

        // Standard messages: "must be greater than or equal to {value}" / "must be less than or equal to {value}"
        assertTrue(minDescription.contains("must be greater than or equal to 10"));
        assertTrue(maxDescription.contains("must be less than or equal to 1000"));
    }

    @Test
    public void buildDescription_urlConstraint() {
        ClassInfo classInfo = index.getClassByName(TestRepresentation.class);

        String description = scanner.buildDescription(classInfo, "urlField");

        assertTrue(description.contains("must be a valid URL"));
    }

    @Test
    public void buildDescription_noConstraintsReturnsNull() {
        ClassInfo classInfo = index.getClassByName(TestRepresentation.class);

        String description = scanner.buildDescription(classInfo, "noConstraints");

        assertNull(description);
    }

    @Test
    public void buildDescription_nonExistentFieldReturnsNull() {
        ClassInfo classInfo = index.getClassByName(TestRepresentation.class);

        String description = scanner.buildDescription(classInfo, "nonExistentField");

        assertNull(description);
    }

    @Test
    public void buildDescription_withCreateGroupContext() {
        ClassInfo classInfo = index.getClassByName(TestWithGroups.class);

        String description = scanner.buildDescription(classInfo, "createOnly");

        assertNotNull(description);
        assertTrue(description.contains("on create"));
        assertTrue(description.contains("must not be blank"));
    }

    @Test
    public void buildDescription_withPutGroupContext() {
        ClassInfo classInfo = index.getClassByName(TestWithGroups.class);

        String description = scanner.buildDescription(classInfo, "updateOnly");

        assertNotNull(description);
        assertTrue(description.contains("on update"));
        assertTrue(description.contains("must not be null"));
    }

    @Test
    public void buildDescription_withPatchGroupContext() {
        ClassInfo classInfo = index.getClassByName(TestWithGroups.class);

        String description = scanner.buildDescription(classInfo, "patchOnly");

        assertNotNull(description);
        assertTrue(description.contains("on patch"));
    }

    @Test
    public void buildDescription_multipleGroupsShowMultipleContexts() {
        ClassInfo classInfo = index.getClassByName(TestWithGroups.class);

        String description = scanner.buildDescription(classInfo, "createAndUpdate");

        assertNotNull(description);
        assertTrue(description.contains("on create"));
        assertTrue(description.contains("on update"));
    }

    @Test
    public void buildClassLevelDescriptions_returnsDescriptionForClassLevelConstraint() {
        ClassInfo classInfo = index.getClassByName(TestWithClassLevelConstraint.class);

        Map<String, String> descriptions = scanner.buildClassLevelDescriptions(classInfo);

        assertNotNull(descriptions);
        assertEquals(1, descriptions.size());
        assertTrue(descriptions.containsKey("secret"));
        // Uses the default message from the annotation definition
        assertEquals("Client secret must not be blank", descriptions.get("secret"));
    }

    @Test
    public void buildClassLevelDescriptions_usesExplicitMessage() {
        ClassInfo classInfo = index.getClassByName(TestWithClassLevelConstraintAndMessage.class);

        Map<String, String> descriptions = scanner.buildClassLevelDescriptions(classInfo);

        assertNotNull(descriptions);
        assertTrue(descriptions.containsKey("secret"));
        assertEquals("Custom validation message", descriptions.get("secret"));
    }

    @Test
    public void buildClassLevelDescriptions_includesGroupContext() {
        ClassInfo classInfo = index.getClassByName(TestWithClassLevelConstraintAndGroups.class);

        Map<String, String> descriptions = scanner.buildClassLevelDescriptions(classInfo);

        assertNotNull(descriptions);
        assertTrue(descriptions.containsKey("secret"));
        // Uses the default message from the annotation definition with group context
        assertEquals("on create: Client secret must not be blank", descriptions.get("secret"));
    }

    @Test
    public void buildClassLevelDescriptions_returnsEmptyMapForClassWithoutConstraints() {
        ClassInfo classInfo = index.getClassByName(TestRepresentation.class);

        Map<String, String> descriptions = scanner.buildClassLevelDescriptions(classInfo);

        assertNotNull(descriptions);
        assertTrue(descriptions.isEmpty());
    }

    private Index createIndex(Class<?>... classes) throws IOException {
        Indexer indexer = new Indexer();
        for (Class<?> clazz : classes) {
            String className = clazz.getName().replace('.', '/') + ".class";
            try (InputStream stream = getClass().getClassLoader().getResourceAsStream(className)) {
                if (stream != null) {
                    indexer.index(stream);
                }
            }
        }
        return indexer.complete();
    }

    @SuppressWarnings("unused")
    public static class TestRepresentation {
        @NotBlank
        private String notBlankField;

        @NotNull
        private String notNullField;

        @NotEmpty
        private Set<String> notEmptyList;

        @Size(min = 5, max = 100)
        private String sizedString;

        @Pattern(regexp = "[a-z]+")
        private String patternField;

        @Min(10)
        private Integer minValue;

        @Max(1000)
        private Integer maxValue;

        @URL
        private String urlField;

        private Set<@URL String> urlSet;

        private String noConstraints;
    }

    @SuppressWarnings("unused")
    public static class TestWithGroups {
        @NotBlank(groups = CreateClient.class)
        private String createOnly;

        @NotNull(groups = PutClient.class)
        private String updateOnly;

        @NotBlank(groups = PatchClient.class)
        private String patchOnly;

        @NotBlank(groups = {CreateClient.class, PutClient.class})
        private String createAndUpdate;
    }

    @ClientSecretNotBlank(affectedFieldNames = {"secret"})
    @SuppressWarnings("unused")
    public static class TestWithClassLevelConstraint {
        private String secret;
    }

    @ClientSecretNotBlank(groups = CreateClient.class, affectedFieldNames = {"secret"})
    @SuppressWarnings("unused")
    public static class TestWithClassLevelConstraintAndGroups {
        private String secret;
    }

    @ClientSecretNotBlank(message = "Custom validation message", affectedFieldNames = {"secret"})
    @SuppressWarnings("unused")
    public static class TestWithClassLevelConstraintAndMessage {
        private String secret;
    }
}
