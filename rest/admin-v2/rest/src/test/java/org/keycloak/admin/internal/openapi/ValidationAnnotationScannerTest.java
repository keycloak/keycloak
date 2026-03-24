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
import java.math.BigDecimal;
import java.util.Set;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ValidationAnnotationScannerTest {

    private ValidationAnnotationScanner scanner;
    private Index index;

    @Before
    public void setUp() throws IOException {
        scanner = new ValidationAnnotationScanner();
        index = createIndex(
                TestRepresentation.class,
                TestWithGroups.class,
                CreateTestResource.class,
                PutTestResource.class,
                PatchTestResource.class
        );
    }

    @Test
    public void applySchemaProperties_notBlankSetsMinLength() {
        ClassInfo classInfo = index.getClassByName(TestRepresentation.class);
        Schema schema = OASFactory.createSchema();

        scanner.applySchemaProperties(classInfo, "notBlankField", schema);

        assertEquals(Integer.valueOf(1), schema.getMinLength());
    }

    @Test
    public void applySchemaProperties_notEmptySetsMinItems() {
        ClassInfo classInfo = index.getClassByName(TestRepresentation.class);
        Schema schema = OASFactory.createSchema();

        scanner.applySchemaProperties(classInfo, "notEmptyList", schema);

        assertEquals(Integer.valueOf(1), schema.getMinItems());
    }

    @Test
    public void applySchemaProperties_sizeConstraintsOnString() {
        ClassInfo classInfo = index.getClassByName(TestRepresentation.class);
        Schema schema = OASFactory.createSchema();

        scanner.applySchemaProperties(classInfo, "sizedString", schema);

        assertEquals(Integer.valueOf(5), schema.getMinLength());
        assertEquals(Integer.valueOf(100), schema.getMaxLength());
    }

    @Test
    public void applySchemaProperties_patternSetsRegex() {
        ClassInfo classInfo = index.getClassByName(TestRepresentation.class);
        Schema schema = OASFactory.createSchema();

        scanner.applySchemaProperties(classInfo, "patternField", schema);

        assertEquals("[a-z]+", schema.getPattern());
    }

    @Test
    public void applySchemaProperties_minConstraint() {
        ClassInfo classInfo = index.getClassByName(TestRepresentation.class);
        Schema schema = OASFactory.createSchema();

        scanner.applySchemaProperties(classInfo, "minValue", schema);

        assertEquals(new BigDecimal(10), schema.getMinimum());
    }

    @Test
    public void applySchemaProperties_maxConstraint() {
        ClassInfo classInfo = index.getClassByName(TestRepresentation.class);
        Schema schema = OASFactory.createSchema();

        scanner.applySchemaProperties(classInfo, "maxValue", schema);

        assertEquals(new BigDecimal(1000), schema.getMaximum());
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

        assertTrue(description.contains("required"));
    }

    @Test
    public void buildDescription_sizeConstraint() {
        ClassInfo classInfo = index.getClassByName(TestRepresentation.class);

        String description = scanner.buildDescription(classInfo, "sizedString");

        assertTrue(description.contains("length must be between 5 and 100"));
    }

    @Test
    public void buildDescription_patternConstraint() {
        ClassInfo classInfo = index.getClassByName(TestRepresentation.class);

        String description = scanner.buildDescription(classInfo, "patternField");

        assertTrue(description.contains("must match pattern: [a-z]+"));
    }

    @Test
    public void buildDescription_minMaxConstraints() {
        ClassInfo classInfo = index.getClassByName(TestRepresentation.class);

        String minDescription = scanner.buildDescription(classInfo, "minValue");
        String maxDescription = scanner.buildDescription(classInfo, "maxValue");

        assertTrue(minDescription.contains("minimum value 10"));
        assertTrue(maxDescription.contains("maximum value 1000"));
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
        assertTrue(description.contains("required"));
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

        private String noConstraints;
    }

    @SuppressWarnings("unused")
    public static class TestWithGroups {
        @NotBlank(groups = CreateTestResource.class)
        private String createOnly;

        @NotNull(groups = PutTestResource.class)
        private String updateOnly;

        @NotBlank(groups = PatchTestResource.class)
        private String patchOnly;

        @NotBlank(groups = {CreateTestResource.class, PutTestResource.class})
        private String createAndUpdate;
    }

    public interface CreateTestResource {}
    public interface PutTestResource {}
    public interface PatchTestResource {}
}
