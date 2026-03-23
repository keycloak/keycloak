package org.keycloak.admin.internal.openapi;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.eclipse.microprofile.openapi.models.media.Schema;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

/**
 * Scans Jakarta Validation annotations on representation classes and applies them to OpenAPI schemas.
 * <p>
 * This scanner provides two types of validation exposure:
 * <ul>
 *   <li><b>Machine-readable</b>: Schema properties like {@code minLength}, {@code format}, {@code pattern}</li>
 *   <li><b>Human-readable</b>: Validation descriptions appended to field descriptions</li>
 * </ul>
 * <p>
 * Supports standard Jakarta Validation annotations, Hibernate Validator annotations,
 * custom Keycloak validation annotations, and validation groups.
 */
public class ValidationAnnotationScanner {

    private static final Logger log = Logger.getLogger(ValidationAnnotationScanner.class);

    // Jakarta Validation annotations
    private static final DotName NOT_BLANK = DotName.createSimple("jakarta.validation.constraints.NotBlank");
    private static final DotName NOT_NULL = DotName.createSimple("jakarta.validation.constraints.NotNull");
    private static final DotName NOT_EMPTY = DotName.createSimple("jakarta.validation.constraints.NotEmpty");
    private static final DotName SIZE = DotName.createSimple("jakarta.validation.constraints.Size");
    private static final DotName PATTERN = DotName.createSimple("jakarta.validation.constraints.Pattern");
    private static final DotName MIN = DotName.createSimple("jakarta.validation.constraints.Min");
    private static final DotName MAX = DotName.createSimple("jakarta.validation.constraints.Max");
    private static final DotName VALID = DotName.createSimple("jakarta.validation.Valid");

    // Hibernate Validator annotations
    private static final DotName URL = DotName.createSimple("org.hibernate.validator.constraints.URL");

    // Custom validation annotations
    private static final DotName UUID_UNMODIFIED = DotName.createSimple("org.keycloak.representations.admin.v2.validation.UuidUnmodified");
    private static final DotName CLIENT_SECRET_NOT_BLANK = DotName.createSimple("org.keycloak.representations.admin.v2.validation.ClientSecretNotBlank");

    // Validation groups
    private static final DotName CREATE_CLIENT = DotName.createSimple("org.keycloak.representations.admin.v2.validation.CreateClient");
    private static final DotName PUT_CLIENT = DotName.createSimple("org.keycloak.representations.admin.v2.validation.PutClient");
    private static final DotName PATCH_CLIENT = DotName.createSimple("org.keycloak.representations.admin.v2.validation.PatchClient");

    /**
     * Applies machine-readable validation schema properties based on validation annotations.
     * These properties can be used by OpenAPI client generators for client-side validation.
     *
     * @param classInfo the class containing the field
     * @param fieldName the name of the field
     * @param propertySchema the OpenAPI schema to modify
     */
    public void applySchemaProperties(ClassInfo classInfo, String fieldName, Schema propertySchema) {
        FieldInfo field = classInfo.field(fieldName);
        if (field == null) {
            return;
        }

        // @NotBlank implies minLength: 1 for strings (only for field-level, not type args)
        AnnotationInstance notBlank = getFieldAnnotation(field, NOT_BLANK);
        if (notBlank != null && propertySchema.getMinLength() == null) {
            propertySchema.setMinLength(1);
        }

        // @NotEmpty implies minItems: 1 for arrays/collections
        AnnotationInstance notEmpty = getFieldAnnotation(field, NOT_EMPTY);
        if (notEmpty != null && propertySchema.getMinItems() == null) {
            propertySchema.setMinItems(1);
        }

        // @URL implies format: uri
        AnnotationInstance url = getFieldAnnotation(field, URL);
        if (url != null && propertySchema.getFormat() == null) {
            propertySchema.setFormat("uri");
        }

        // @Size constraints
        AnnotationInstance size = getFieldAnnotation(field, SIZE);
        if (size != null) {
            applySizeConstraints(field, size, propertySchema);
        }

        // @Pattern constraints
        AnnotationInstance pattern = getFieldAnnotation(field, PATTERN);
        if (pattern != null && propertySchema.getPattern() == null) {
            AnnotationValue regexp = pattern.value("regexp");
            if (regexp != null) {
                propertySchema.setPattern(regexp.asString());
            }
        }

        // @Min constraints
        AnnotationInstance min = getFieldAnnotation(field, MIN);
        if (min != null && propertySchema.getMinimum() == null) {
            AnnotationValue value = min.value();
            if (value != null) {
                propertySchema.setMinimum(new BigDecimal(value.asLong()));
            }
        }

        // @Max constraints
        AnnotationInstance max = getFieldAnnotation(field, MAX);
        if (max != null && propertySchema.getMaximum() == null) {
            AnnotationValue value = max.value();
            if (value != null) {
                propertySchema.setMaximum(new BigDecimal(value.asLong()));
            }
        }

        // Apply type argument constraints to items schema (for arrays)
        applyTypeArgumentSchemaProperties(field, propertySchema);
    }

    /**
     * Builds a human-readable validation description for a field by scanning its validation annotations.
     * Includes validation group context (when the constraint applies: create, update, patch).
     *
     * @param classInfo the class containing the field
     * @param fieldName the name of the field
     * @return validation description or null if no constraints found
     */
    public String buildDescription(ClassInfo classInfo, String fieldName) {
        FieldInfo field = classInfo.field(fieldName);
        if (field == null) {
            return null;
        }

        List<String> constraints = new ArrayList<>();

        // Check for @NotBlank
        AnnotationInstance notBlank = getFieldAnnotation(field, NOT_BLANK);
        if (notBlank != null) {
            String context = getGroupContext(notBlank);
            constraints.add(context + "must not be blank");
        }

        // Check for @NotNull
        AnnotationInstance notNull = getFieldAnnotation(field, NOT_NULL);
        if (notNull != null) {
            String context = getGroupContext(notNull);
            constraints.add(context + "required");
        }

        // Check for @NotEmpty
        AnnotationInstance notEmpty = getFieldAnnotation(field, NOT_EMPTY);
        if (notEmpty != null) {
            String context = getGroupContext(notEmpty);
            constraints.add(context + "must not be empty");
        }

        // Check for @URL
        AnnotationInstance url = getFieldAnnotation(field, URL);
        if (url != null) {
            constraints.add("must be a valid URL");
        }

        // Check for @Size
        AnnotationInstance size = getFieldAnnotation(field, SIZE);
        if (size != null) {
            String sizeDesc = buildSizeDescription(size);
            if (sizeDesc != null) {
                constraints.add(sizeDesc);
            }
        }

        // Check for @Pattern
        AnnotationInstance pattern = getFieldAnnotation(field, PATTERN);
        if (pattern != null) {
            AnnotationValue regexp = pattern.value("regexp");
            if (regexp != null) {
                constraints.add("must match pattern: " + regexp.asString());
            }
        }

        // Check for @Min
        AnnotationInstance min = getFieldAnnotation(field, MIN);
        if (min != null) {
            AnnotationValue value = min.value();
            if (value != null) {
                constraints.add("minimum value " + value.asLong());
            }
        }

        // Check for @Max
        AnnotationInstance max = getFieldAnnotation(field, MAX);
        if (max != null) {
            AnnotationValue value = max.value();
            if (value != null) {
                constraints.add("maximum value " + value.asLong());
            }
        }

        // Check for @Valid (nested validation)
        AnnotationInstance valid = getFieldAnnotation(field, VALID);
        if (valid != null) {
            constraints.add("nested fields are validated");
        }

        // Check for type argument annotations (e.g., Set<@NotBlank @URL String>)
        String typeArgConstraints = buildTypeArgumentDescription(field);
        if (typeArgConstraints != null) {
            constraints.add(typeArgConstraints);
        }

        if (constraints.isEmpty()) {
            return null;
        }

        StringJoiner joiner = new StringJoiner("; ");
        constraints.forEach(joiner::add);
        return joiner.toString();
    }

    /**
     * Builds validation descriptions from class-level validation annotations.
     * Returns a map of field name to validation description for fields affected by class-level constraints.
     *
     * @param classInfo the class to scan
     * @return map of field name to validation description
     */
    public Map<String, String> buildClassLevelDescriptions(ClassInfo classInfo) {
        Map<String, String> fieldDescriptions = new HashMap<>();

        // Check for @UuidUnmodified
        AnnotationInstance uuidUnmodified = classInfo.annotation(UUID_UNMODIFIED);
        if (uuidUnmodified != null) {
            String context = getGroupContext(uuidUnmodified);
            String message = uuidUnmodified.value("message") != null
                    ? uuidUnmodified.value("message").asString()
                    : "server-managed, must not be modified";
            if (!message.startsWith("{")) {
                fieldDescriptions.put("uuid", context + message);
            } else {
                fieldDescriptions.put("uuid", context + "server-managed, must not be modified");
            }
        }

        // Check for @ClientSecretNotBlank
        AnnotationInstance clientSecretNotBlank = classInfo.annotation(CLIENT_SECRET_NOT_BLANK);
        if (clientSecretNotBlank != null) {
            String context = getGroupContext(clientSecretNotBlank);
            fieldDescriptions.put("secret", context + "required when using client secret authentication");
        }

        return fieldDescriptions;
    }

    private void applySizeConstraints(FieldInfo field, AnnotationInstance size, Schema propertySchema) {
        AnnotationValue minValue = size.value("min");
        AnnotationValue maxValue = size.value("max");

        // For strings: minLength/maxLength, for arrays: minItems/maxItems
        Type fieldType = field.type();
        boolean isCollection = fieldType.kind() == Type.Kind.PARAMETERIZED_TYPE ||
                fieldType.kind() == Type.Kind.ARRAY;

        if (isCollection) {
            if (minValue != null && minValue.asInt() > 0 && propertySchema.getMinItems() == null) {
                propertySchema.setMinItems(minValue.asInt());
            }
            if (maxValue != null && maxValue.asInt() < Integer.MAX_VALUE && propertySchema.getMaxItems() == null) {
                propertySchema.setMaxItems(maxValue.asInt());
            }
        } else {
            if (minValue != null && minValue.asInt() > 0 && propertySchema.getMinLength() == null) {
                propertySchema.setMinLength(minValue.asInt());
            }
            if (maxValue != null && maxValue.asInt() < Integer.MAX_VALUE && propertySchema.getMaxLength() == null) {
                propertySchema.setMaxLength(maxValue.asInt());
            }
        }
    }

    private String buildSizeDescription(AnnotationInstance size) {
        AnnotationValue minValue = size.value("min");
        AnnotationValue maxValue = size.value("max");
        int min = minValue != null ? minValue.asInt() : 0;
        int max = maxValue != null ? maxValue.asInt() : Integer.MAX_VALUE;

        if (min > 0 && max < Integer.MAX_VALUE) {
            return "length must be between " + min + " and " + max;
        } else if (min > 0) {
            return "minimum length " + min;
        } else if (max < Integer.MAX_VALUE) {
            return "maximum length " + max;
        }
        return null;
    }

    private void applyTypeArgumentSchemaProperties(FieldInfo field, Schema propertySchema) {
        Type fieldType = field.type();
        if (fieldType.kind() != Type.Kind.PARAMETERIZED_TYPE) {
            return;
        }

        List<Type> typeArgs = fieldType.asParameterizedType().arguments();
        if (typeArgs.isEmpty()) {
            return;
        }

        Schema itemsSchema = propertySchema.getItems();
        if (itemsSchema == null) {
            return;
        }

        for (Type typeArg : typeArgs) {
            for (AnnotationInstance annotation : typeArg.annotations()) {
                DotName annotationName = annotation.name();

                if (NOT_BLANK.equals(annotationName) && itemsSchema.getMinLength() == null) {
                    itemsSchema.setMinLength(1);
                } else if (URL.equals(annotationName) && itemsSchema.getFormat() == null) {
                    itemsSchema.setFormat("uri");
                } else if (SIZE.equals(annotationName)) {
                    AnnotationValue minValue = annotation.value("min");
                    AnnotationValue maxValue = annotation.value("max");
                    if (minValue != null && minValue.asInt() > 0 && itemsSchema.getMinLength() == null) {
                        itemsSchema.setMinLength(minValue.asInt());
                    }
                    if (maxValue != null && maxValue.asInt() < Integer.MAX_VALUE && itemsSchema.getMaxLength() == null) {
                        itemsSchema.setMaxLength(maxValue.asInt());
                    }
                } else if (PATTERN.equals(annotationName) && itemsSchema.getPattern() == null) {
                    AnnotationValue regexp = annotation.value("regexp");
                    if (regexp != null) {
                        itemsSchema.setPattern(regexp.asString());
                    }
                }
            }
        }
    }

    private String buildTypeArgumentDescription(FieldInfo field) {
        Type fieldType = field.type();
        if (fieldType.kind() != Type.Kind.PARAMETERIZED_TYPE) {
            return null;
        }

        List<Type> typeArgs = fieldType.asParameterizedType().arguments();
        if (typeArgs.isEmpty()) {
            return null;
        }

        List<String> elementConstraints = new ArrayList<>();

        for (Type typeArg : typeArgs) {
            List<AnnotationInstance> annotations = typeArg.annotations();
            for (AnnotationInstance annotation : annotations) {
                DotName annotationName = annotation.name();

                if (NOT_BLANK.equals(annotationName)) {
                    elementConstraints.add("must not be blank");
                } else if (NOT_NULL.equals(annotationName)) {
                    elementConstraints.add("required");
                } else if (NOT_EMPTY.equals(annotationName)) {
                    elementConstraints.add("must not be empty");
                } else if (URL.equals(annotationName)) {
                    AnnotationValue message = annotation.value("message");
                    if (message != null && !message.asString().isEmpty() && !message.asString().startsWith("{")) {
                        elementConstraints.add(message.asString());
                    } else {
                        elementConstraints.add("must be a valid URL");
                    }
                } else if (SIZE.equals(annotationName)) {
                    AnnotationValue minValue = annotation.value("min");
                    AnnotationValue maxValue = annotation.value("max");
                    int min = minValue != null ? minValue.asInt() : 0;
                    int max = maxValue != null ? maxValue.asInt() : Integer.MAX_VALUE;
                    if (min > 0 && max < Integer.MAX_VALUE) {
                        elementConstraints.add("length between " + min + " and " + max);
                    } else if (min > 0) {
                        elementConstraints.add("minimum length " + min);
                    } else if (max < Integer.MAX_VALUE) {
                        elementConstraints.add("maximum length " + max);
                    }
                } else if (PATTERN.equals(annotationName)) {
                    AnnotationValue regexp = annotation.value("regexp");
                    if (regexp != null) {
                        elementConstraints.add("must match pattern: " + regexp.asString());
                    }
                }
            }
        }

        if (elementConstraints.isEmpty()) {
            return null;
        }

        StringJoiner joiner = new StringJoiner(", ");
        elementConstraints.forEach(joiner::add);
        return "each element " + joiner;
    }

    private String getGroupContext(AnnotationInstance annotation) {
        AnnotationValue groupsValue = annotation.value("groups");
        if (groupsValue == null) {
            return "";
        }

        Type[] groups = groupsValue.asClassArray();
        if (groups == null || groups.length == 0) {
            return "";
        }

        List<String> contexts = new ArrayList<>();
        for (Type group : groups) {
            DotName groupName = group.name();
            if (CREATE_CLIENT.equals(groupName)) {
                contexts.add("on create");
            } else if (PUT_CLIENT.equals(groupName)) {
                contexts.add("on update");
            } else if (PATCH_CLIENT.equals(groupName)) {
                contexts.add("on patch");
            }
        }

        if (contexts.isEmpty()) {
            return "";
        }

        return String.join("/", contexts) + ": ";
    }

    private AnnotationInstance getFieldAnnotation(FieldInfo field, DotName annotationName) {
        AnnotationInstance annotation = field.annotation(annotationName);
        if (annotation == null) {
            return null;
        }
        // Check if the annotation target is the field itself (not a type use)
        AnnotationTarget target = annotation.target();
        if (target != null && target.kind() == AnnotationTarget.Kind.FIELD) {
            return annotation;
        }
        return null;
    }
}
