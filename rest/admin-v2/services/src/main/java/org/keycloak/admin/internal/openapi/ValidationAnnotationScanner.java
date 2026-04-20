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
import org.jboss.jandex.TypeTarget;
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
 * Standard Jakarta and Hibernate Validator constraints are recognized even when their
 * annotation types are not part of the OpenAPI scanner's Jandex index (only application packages are indexed).
 */
public class ValidationAnnotationScanner {

    private static final Logger log = Logger.getLogger(ValidationAnnotationScanner.class);

    // Meta-annotation that marks constraint annotations
    private static final DotName CONSTRAINT = DotName.createSimple(Constraint.class);

    // Hibernate Validator's @URL annotation (for schema property handling - not supported by SmallRye)
    private static final DotName URL = DotName.createSimple(URL.class);

    private static final DotName JAKARTA_NOT_BLANK = DotName.createSimple("jakarta.validation.constraints.NotBlank");
    private static final DotName JAKARTA_PATTERN = DotName.createSimple("jakarta.validation.constraints.Pattern");

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
        if (isStandardBeanValidationOrHibernateConstraint(annotationName)) {
            return true;
        }
        ClassInfo annotationClass = indexView.getClassByName(annotationName);
        return annotationClass != null && annotationClass.hasAnnotation(CONSTRAINT);
    }

    /**
     * True for built-in Jakarta Bean Validation and Hibernate Validator constraint annotations.
     * The OpenAPI Maven plugin typically indexes only application packages, so these types are
     * often absent from {@link IndexView} even though they appear on representation fields.
     */
    private static boolean isStandardBeanValidationOrHibernateConstraint(DotName name) {
        String s = name.toString();
        return s.startsWith("jakarta.validation.constraints.")
                || s.startsWith("org.hibernate.validator.constraints.");
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
     * Applies schema properties for constraints that SmallRye's {@code BeanValidationScanner}
     * misses or applies incompletely: Hibernate Validator's {@code @URL}, and Jakarta
     * {@code @Pattern}/{@code @NotBlank} on collection element types.
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

        // De-duplicate when the compiler emits both declaration and type-use constraint annotations
        Set<DotName> seenFieldConstraintTypes = new HashSet<>();
        for (AnnotationInstance annotation : field.annotations()) {
            if (!isAnnotationOnFieldDeclaration(annotation, field)) {
                continue;
            }

            DotName annotationName = annotation.name();

            // Check if this is a constraint annotation
            if (!isConstraintAnnotation(annotationName)) {
                continue;
            }
            if (!seenFieldConstraintTypes.add(annotationName)) {
                continue;
            }

            String constraintDesc = buildConstraintDescription(annotation);
            if (constraintDesc != null) {
                String context = getGroupContext(annotation);
                constraints.add(context + constraintDesc);
            }
        }

        // Check for type argument annotations (e.g. Set<@URL String>, Set<@NotBlank @URL String>)
        String typeArgConstraints = buildTypeArgumentDescription(field);
        if (typeArgConstraints != null) {
            constraints.add(typeArgConstraints);
        }

        if (constraints.isEmpty()) {
            return null;
        }

        List<String> deduped = new ArrayList<>();
        Set<String> seenPhrases = new HashSet<>();
        for (String phrase : constraints) {
            if (seenPhrases.add(phrase)) {
                deduped.add(phrase);
            }
        }
        StringJoiner joiner = new StringJoiner("; ");
        deduped.forEach(joiner::add);
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
        AnnotationValue defaultFromIndex = getDefault(annotationName, "message");
        if (defaultFromIndex != null) {
            String defaultMessage = defaultFromIndex.asString();
            if (defaultMessage != null) {
                return defaultMessage;
            }
        }
        if (isStandardBeanValidationOrHibernateConstraint(annotationName)) {
            return "{" + annotationName + ".message}";
        }

        throw new IllegalStateException("Missing message value for " + annotationName);
    }

    private AnnotationValue getDefault(DotName annotationName, String fieldName) {
        // Try to get the default value from the annotation class definition
        ClassInfo annotationClass = indexView.getClassByName(annotationName);
        if (annotationClass != null) {
            var messageMethod = annotationClass.method(fieldName);
            if (messageMethod != null && messageMethod.defaultValue() != null) {
                return messageMethod.defaultValue();
            }
        }
        return null;
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
            String message = buildConstraintDescription(annotation);

            AnnotationValue affectedField = annotation.value("affectedFieldNames");
            String[] affectedFields = affectedField != null ? affectedField.asStringArray() : getDefault(annotation.name(), "affectedFieldNames").asStringArray();

            if (affectedFields == null) {
                throw new IllegalStateException("Missing affectedFieldNames value for " + annotation.name());
            }

            for (String affected : affectedFields) {
                fieldDescriptions.put(affected, context + message);
            }
        }

        return fieldDescriptions;
    }

    /**
     * True when the annotation applies to this field, including type-use constraints on the field's type
     * (javac stores {@code @NotBlank String clientId} as a type annotation with enclosing {@code FIELD}).
     */
    private static boolean isAnnotationOnFieldDeclaration(AnnotationInstance annotation, FieldInfo field) {
        AnnotationTarget target = annotation.target();
        if (target == null) {
            return false;
        }
        if (target.kind() == AnnotationTarget.Kind.FIELD) {
            return field.equals(target.asField());
        }
        if (target.kind() == AnnotationTarget.Kind.TYPE) {
            TypeTarget typeTarget = target.asType();
            AnnotationTarget enclosing = typeTarget.enclosingTarget();
            return enclosing != null
                    && enclosing.kind() == AnnotationTarget.Kind.FIELD
                    && field.equals(enclosing.asField());
        }
        return false;
    }

    /**
     * Applies schema hints for type-use constraints on collection/array element types
     * (e.g. {@code Set<@NotBlank @URL String>}) where SmallRye's {@code BeanValidationScanner}
     * often omits {@code items} metadata.
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
            AnnotationInstance patternAnnotation = null;
            boolean notBlank = false;
            boolean url = false;
            for (AnnotationInstance annotation : typeArg.annotations()) {
                DotName name = annotation.name();
                if (JAKARTA_PATTERN.equals(name)) {
                    patternAnnotation = annotation;
                } else if (JAKARTA_NOT_BLANK.equals(name)) {
                    notBlank = true;
                } else if (URL.equals(name)) {
                    url = true;
                }
            }
            if (patternAnnotation != null) {
                AnnotationValue regexp = patternAnnotation.value("regexp");
                if (regexp != null && itemsSchema.getPattern() == null) {
                    itemsSchema.setPattern(regexp.asString());
                }
            } else if (notBlank && itemsSchema.getPattern() == null) {
                itemsSchema.setPattern("\\S");
            }
            if (url && itemsSchema.getFormat() == null) {
                itemsSchema.setFormat("uri");
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
                DotName name = annotation.name();
                // @URL is always handled explicitly: it must appear in OpenAPI prose even if the
                // Jandex index does not list the annotation type as @Constraint (thin classpath).
                if (URL.equals(name)) {
                    String constraintDesc = buildConstraintDescription(annotation);
                    if (constraintDesc != null) {
                        elementConstraints.add(constraintDesc);
                    }
                    continue;
                }
                if (!isConstraintAnnotation(name)) {
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
        AnnotationInstance direct = field.annotation(annotationName);
        if (direct != null && isAnnotationOnFieldDeclaration(direct, field)) {
            return direct;
        }
        for (AnnotationInstance annotation : field.annotations()) {
            if (annotationName.equals(annotation.name()) && isAnnotationOnFieldDeclaration(annotation, field)) {
                return annotation;
            }
        }
        return null;
    }
}
