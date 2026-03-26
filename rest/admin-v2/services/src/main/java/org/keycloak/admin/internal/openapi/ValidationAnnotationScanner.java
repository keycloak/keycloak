package org.keycloak.admin.internal.openapi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringJoiner;

import jakarta.validation.Constraint;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.keycloak.representations.admin.v2.validation.CreateClient;
import org.keycloak.representations.admin.v2.validation.PatchClient;
import org.keycloak.representations.admin.v2.validation.PutClient;

import org.eclipse.microprofile.openapi.models.media.Schema;
import org.hibernate.validator.constraints.URL;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
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

    // Meta-annotation that marks constraint annotations
    private static final DotName CONSTRAINT = DotName.createSimple(Constraint.class);

    // Annotations that need special handling for parameter formatting
    private static final DotName SIZE = DotName.createSimple(Size.class);
    private static final DotName PATTERN = DotName.createSimple(Pattern.class);
    private static final DotName MIN = DotName.createSimple(Min.class);
    private static final DotName MAX = DotName.createSimple(Max.class);
    private static final DotName VALID = DotName.createSimple(Valid.class);

    // Hibernate Validator annotations (not supported by SmallRye's BeanValidationScanner)
    private static final DotName URL = DotName.createSimple(URL.class);

    // Validation group package prefix for detecting unknown groups
    private static final String VALIDATION_PACKAGE = "org.keycloak.representations.admin.v2.validation";

    // Validation groups mapped to their human-readable context
    // When adding a new validation group, add it here to provide a description for the OpenAPI docs.
    private static final Map<DotName, String> VALIDATION_GROUPS = Map.of(
            DotName.createSimple(CreateClient.class), "on create",
            DotName.createSimple(PutClient.class), "on update",
            DotName.createSimple(PatchClient.class), "on patch"
    );

    private final IndexView indexView;
    private final ResourceBundle validationMessages;
    private final Set<DotName> constraintAnnotations;

    public ValidationAnnotationScanner(IndexView indexView) {
        this.indexView = indexView;
        this.validationMessages = loadValidationMessages();
        this.constraintAnnotations = discoverConstraintAnnotations();
    }

    private ResourceBundle loadValidationMessages() {
        try {
            return ResourceBundle.getBundle("org.hibernate.validator.ValidationMessages");
        } catch (MissingResourceException e) {
            log.warn("Could not load ValidationMessages resource bundle", e);
            return null;
        }
    }

    private Set<DotName> discoverConstraintAnnotations() {
        Set<DotName> constraints = new HashSet<>();
        for (AnnotationInstance annotation : indexView.getAnnotations(CONSTRAINT)) {
            if (annotation.target().kind() == AnnotationTarget.Kind.CLASS) {
                constraints.add(annotation.target().asClass().name());
            }
        }
        log.debugf("Discovered %d constraint annotations: %s", constraints.size(), constraints);
        return constraints;
    }

    private boolean isConstraintAnnotation(DotName annotationName) {
        if (constraintAnnotations.contains(annotationName)) {
            return true;
        }
        ClassInfo annotationClass = indexView.getClassByName(annotationName);
        return annotationClass != null && annotationClass.hasAnnotation(CONSTRAINT);
    }

    private String resolveMessage(String messageTemplate) {
        if (messageTemplate == null || messageTemplate.isEmpty()) {
            return null;
        }
        if (messageTemplate.startsWith("{") && messageTemplate.endsWith("}")) {
            String key = messageTemplate.substring(1, messageTemplate.length() - 1);
            if (validationMessages != null) {
                try {
                    return validationMessages.getString(key);
                } catch (MissingResourceException e) {
                    log.debugf("Message key not found in resource bundle: %s", key);
                }
            }
            return null;
        }
        return messageTemplate;
    }

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

        for (AnnotationInstance annotation : field.annotations()) {
            if (annotation.target().kind() != AnnotationTarget.Kind.FIELD) {
                continue;
            }

            DotName annotationName = annotation.name();

            // Handle @Valid (nested validation) - not a constraint but useful to document
            if (VALID.equals(annotationName)) {
                constraints.add("nested fields are validated");
                continue;
            }

            // Check if this is a constraint annotation
            if (!isConstraintAnnotation(annotationName)) {
                continue;
            }

            String constraintDesc = buildConstraintDescription(annotation);
            if (constraintDesc != null) {
                String context = getGroupContext(annotation);
                constraints.add(context + constraintDesc);
            }
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

    private String buildConstraintDescription(AnnotationInstance annotation) {
        DotName annotationName = annotation.name();

        // Handle annotations that need special parameter formatting
        if (SIZE.equals(annotationName)) {
            return buildSizeDescription(annotation);
        }
        if (PATTERN.equals(annotationName)) {
            AnnotationValue regexp = annotation.value("regexp");
            if (regexp != null) {
                return "must match pattern: " + regexp.asString();
            }
            return null;
        }
        if (MIN.equals(annotationName)) {
            AnnotationValue value = annotation.value();
            if (value != null) {
                return "minimum value " + value.asLong();
            }
            return null;
        }
        if (MAX.equals(annotationName)) {
            AnnotationValue value = annotation.value();
            if (value != null) {
                return "maximum value " + value.asLong();
            }
            return null;
        }
        if (URL.equals(annotationName)) {
            return "must be a valid URL";
        }

        // For all other constraint annotations, resolve the message
        AnnotationValue messageValue = annotation.value("message");
        String messageTemplate = messageValue != null ? messageValue.asString() : getDefaultMessage(annotationName);
        String resolved = resolveMessage(messageTemplate);

        if (resolved != null) {
            return resolved;
        }

        // Fallback: derive description from annotation name
        String simpleName = annotationName.withoutPackagePrefix();
        return humanize(simpleName);
    }

    private String getDefaultMessage(DotName annotationName) {
        return "{" + annotationName.toString() + ".message}";
    }

    private String humanize(String annotationName) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < annotationName.length(); i++) {
            char c = annotationName.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                result.append(' ');
            }
            result.append(Character.toLowerCase(c));
        }
        return result.toString();
    }

    /**
     * Builds validation descriptions from class-level validation annotations.
     * Returns a map of field name to validation description for fields affected by class-level constraints.
     * <p>
     * Note: Class-level constraints often affect multiple fields or cross-field validation.
     * The mapping to specific fields is derived from the constraint's documented behavior.
     *
     * @param classInfo the class to scan
     * @return map of field name to validation description
     */
    public Map<String, String> buildClassLevelDescriptions(ClassInfo classInfo) {
        Map<String, String> fieldDescriptions = new HashMap<>();

        for (AnnotationInstance annotation : classInfo.annotations()) {
            if (annotation.target().kind() != AnnotationTarget.Kind.CLASS) {
                continue;
            }

            if (!isConstraintAnnotation(annotation.name())) {
                continue;
            }

            String context = getGroupContext(annotation);
            AnnotationValue messageValue = annotation.value("message");
            String messageTemplate = messageValue != null ? messageValue.asString() : getDefaultMessage(annotation.name());
            String message = resolveMessage(messageTemplate);

            if (message == null) {
                message = humanize(annotation.name().withoutPackagePrefix());
            }

            // Try to determine which field(s) this class-level constraint affects
            String affectedField = getAffectedField(annotation);
            if (affectedField != null) {
                fieldDescriptions.put(affectedField, context + message);
            }
        }

        return fieldDescriptions;
    }

    private String getAffectedField(AnnotationInstance classLevelConstraint) {
        String simpleName = classLevelConstraint.name().withoutPackagePrefix().toLowerCase();
        if (simpleName.contains("uuid")) {
            return "uuid";
        }
        if (simpleName.contains("secret")) {
            return "secret";
        }
        return null;
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
            for (AnnotationInstance annotation : typeArg.annotations()) {
                if (!isConstraintAnnotation(annotation.name())) {
                    continue;
                }

                String constraintDesc = buildConstraintDescription(annotation);
                if (constraintDesc != null) {
                    elementConstraints.add(constraintDesc);
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
            String context = VALIDATION_GROUPS.get(groupName);
            if (context != null) {
                contexts.add(context);
            } else if (groupName.toString().startsWith(VALIDATION_PACKAGE)) {
                throw new IllegalStateException(
                        "Unknown validation group: " + groupName + ". " +
                        "Please add it to VALIDATION_GROUPS map in " + ValidationAnnotationScanner.class.getSimpleName() +
                        " with an appropriate description (e.g., \"on create\", \"on update\").");
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
