package org.keycloak.services.error;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Problem details object as defined by RFC 9457, returned with the media type {@code application/problem+json} for every
 * error of the Admin API v2.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProblemDetail {

    public static final String MEDIA_TYPE = "application/problem+json";

    private final String type;
    private final String title;
    private final int status;
    private final String detail;
    private final List<Violation> violations;

    /**
     * @param type       identifies the problem, one of {@link ProblemType}, or {@link ProblemType#fallbackType(int)}
     * @param title      short summary, fixed per type
     * @param status     the HTTP status code
     * @param detail     explanation specific to this occurrence
     * @param violations extension member, only for {@link ProblemType#VALIDATION_FAILED}
     */
    @JsonCreator
    public ProblemDetail(@JsonProperty("type") String type,
                         @JsonProperty("title") String title,
                         @JsonProperty("status") int status,
                         @JsonProperty("detail") String detail,
                         @JsonProperty("violations") List<Violation> violations) {
        this.type = type;
        this.title = title;
        this.status = status;
        this.detail = detail;
        this.violations = violations;
    }

    @JsonProperty("type")
    public String type() {
        return type;
    }

    @JsonProperty("title")
    public String title() {
        return title;
    }

    @JsonProperty("status")
    public int status() {
        return status;
    }

    @JsonProperty("detail")
    public String detail() {
        return detail;
    }

    @JsonProperty("violations")
    public List<Violation> violations() {
        return violations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProblemDetail)) {
            return false;
        }
        ProblemDetail that = (ProblemDetail) o;
        return status == that.status
                && Objects.equals(type, that.type)
                && Objects.equals(title, that.title)
                && Objects.equals(detail, that.detail)
                && Objects.equals(violations, that.violations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, title, status, detail, violations);
    }

    @Override
    public String toString() {
        return "ProblemDetail[type=" + type + ", title=" + title + ", status=" + status + ", detail=" + detail
                + ", violations=" + violations + "]";
    }

    /**
     * A single constraint violation.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Violation {

        private final String field;
        private final String message;

        /**
         * @param field   the path of the invalid field in the request body, or the name of the invalid query
         *                parameter; absent when the violation does not belong to a single field
         * @param message why the value is invalid
         */
        @JsonCreator
        public Violation(@JsonProperty("field") String field, @JsonProperty("message") String message) {
            this.field = field;
            this.message = message;
        }

        @JsonProperty("field")
        public String field() {
            return field;
        }

        @JsonProperty("message")
        public String message() {
            return message;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Violation)) {
                return false;
            }
            Violation that = (Violation) o;
            return Objects.equals(field, that.field) && Objects.equals(message, that.message);
        }

        @Override
        public int hashCode() {
            return Objects.hash(field, message);
        }

        @Override
        public String toString() {
            return "Violation[field=" + field + ", message=" + message + "]";
        }
    }

}
