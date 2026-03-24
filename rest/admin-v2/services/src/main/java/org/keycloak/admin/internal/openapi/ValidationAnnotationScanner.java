package org.keycloak.admin.internal.openapi;

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
 * Builds human-readable validation descriptions for OpenAPI schemas based on validation annotations.
 * <p>
 * SmallRye OpenAPI's built-in {@code BeanValidationScanner} handles machine-readable schema properties
 * (like {@code minLength}, {@code pattern}, {@code minimum}) for standard Jakarta Validation annotations.
 * This scanner complements it by:
 * <ul>
 *   <li>Building human-readable validation descriptions for field documentation</li>
 *   <li>Handling Hibernate Validator's {@code @URL} annotation (not supported by SmallRye)</li>
 *   <li>Handling custom Keycloak validation annotations</li>
 *   <li>Adding validation group context (e.g., "on create:", "on update:")</li>
 * </ul>
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

    // Hibernate Validator annotations (not supported by SmallRye's BeanValidationScanner)
    private static final DotName URL = DotName.createSimple("org.hibernate.validator.constraints.URL");

    // Custom validation annotations
    private static final DotName UUID_UNMODIFIED = DotName.createSimple("org.keycloak.representations.admin.v2.validation.UuidUnmodified");
    private static final DotName CLIENT_SECRET_NOT_BLANK = DotName.createSimple("org.keycloak.representations.admin.v2.validation.ClientSecretNotBlank");

    // Validation group operation prefixes mapped to their human-readable context
    private static final Map<String, String> OPERATION_PREFIXES = Map.of(
            "Create", "on create",
            "Put", "on update",
            "Patch", "on patch"
    );

    /**
     * Applies schema properties for annotations not handled by SmallRye's BeanValidationScanner.
     * Currently only handles Hibernate Validator's {@code @URL} annotation.
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

        // @URL implies format: uri (not handled by SmallRye)
        AnnotationInstance url = getFieldAnnotation(field, URL);
        if (url != null && propertySchema.getFormat() == null) {
            propertySchema.setFormat("uri");
        }

        // Apply @URL to type arguments (e.g., Set<@URL String>)
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

    /**
     * Applies @URL format to items schema for parameterized types (e.g., Set&lt;@URL String&gt;).
     * Other type argument annotations are handled by SmallRye's BeanValidationScanner.
     */
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
                // Only handle @URL - SmallRye handles the rest
                if (URL.equals(annotation.name()) && itemsSchema.getFormat() == null) {
                    itemsSchema.setFormat("uri");
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
            String simpleName = group.name().local();
            String context = getOperationContext(simpleName);
            if (context != null) {
                contexts.add(context);
            }
        }

        if (contexts.isEmpty()) {
            return "";
        }

        return String.join("/", contexts) + ": ";
    }

    private String getOperationContext(String groupSimpleName) {
        for (Map.Entry<String, String> entry : OPERATION_PREFIXES.entrySet()) {
            if (groupSimpleName.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
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
