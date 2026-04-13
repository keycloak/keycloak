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

    // Hibernate Validator's @URL annotation (for schema property handling - not supported by SmallRye)
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

    private String resolveMessage(String messageTemplate, AnnotationInstance annotation) {
        if (messageTemplate == null || messageTemplate.isEmpty()) {
            return null;
        }

        String resolved = messageTemplate;

        // Resolve resource bundle key if message is a template like {jakarta.validation.constraints.NotNull.message}
        if (resolved.startsWith("{") && resolved.endsWith("}")) {
            String key = resolved.substring(1, resolved.length() - 1);
            if (validationMessages != null) {
                try {
                    resolved = validationMessages.getString(key);
                } catch (MissingResourceException e) {
                    log.debugf("Message key not found in resource bundle: %s", key);
                    return null;
                }
            } else {
                return null;
            }
        }

        // Interpolate annotation parameters into the message (e.g., {min}, {max}, {value}, {regexp})
        if (annotation != null) {
            for (AnnotationValue value : annotation.values()) {
                String placeholder = "{" + value.name() + "}";
                if (resolved.contains(placeholder)) {
                    resolved = resolved.replace(placeholder, formatAnnotationValue(value));
                }
            }
        }

        return resolved;
    }

    private String formatAnnotationValue(AnnotationValue value) {
        if (value == null) {
            return "";
        }
        Object val = value.value();
        if (val instanceof String) {
            return (String) val;
        }
        return String.valueOf(val);
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

        // Resolve the message from annotation or resource bundle
        AnnotationValue messageValue = annotation.value("message");
        String messageTemplate = messageValue != null ? messageValue.asString() : getDefaultMessage(annotationName);
        String resolved = resolveMessage(messageTemplate, annotation);

        if (resolved != null) {
            return resolved;
        }

        return annotationName.withoutPackagePrefix();
    }

    private String getDefaultMessage(DotName annotationName) {
        return "{" + annotationName.toString() + ".message}";
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
            String message = resolveMessage(messageTemplate, annotation);

            if (message == null) {
                message = annotation.name().withoutPackagePrefix();
            }

            AnnotationValue affectedField = annotation.value("affectedFieldNames");
            String affectedFieldName = annotation.name().withoutPackagePrefix();
            if (affectedField != null) {
                for (String affected : affectedField.asStringArray()) {
                    fieldDescriptions.put(affected, context + message);
                }
            } else {
                fieldDescriptions.put(affectedFieldName, context + message);
            }
        }

        return fieldDescriptions;
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
