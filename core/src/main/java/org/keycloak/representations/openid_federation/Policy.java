package org.keycloak.representations.openid_federation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.exceptions.MetadataPolicyCombinationException;
import org.keycloak.exceptions.MetadataPolicyException;

import java.util.HashSet;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Policy <T> extends AbstractPolicy<T> {

    private static final String COMMA = ",";

    private T value;

    @JsonProperty("default")
    private T defaultValue;

    public static <T> PolicyBuilder<T> builder() {
        return new PolicyBuilder<T>();
    }

    public Policy() {

    }

    protected Policy(PolicyBuilder<T> builder) {
        this.subsetOf = builder.subset_of.isEmpty() ? null : builder.subset_of;
        this.oneOf = builder.one_of.isEmpty() ? null : builder.one_of;
        this.supersetOf = builder.superset_of.isEmpty() ? null : builder.superset_of;
        this.add = builder.add.isEmpty() ? null : builder.add;
        this.value = builder.value;
        this.defaultValue = builder.defaultValue;
        this.essential = builder.essential;
    }

    public Policy<T> combinePolicy(Policy<T> inferior) throws MetadataPolicyCombinationException {

        if (inferior == null) return this;
        // first check combination value with one_of, subset_of , superset_of
        if (notNullnotEqual(this.value, inferior.getValue())) {
            throw new MetadataPolicyCombinationException("Could not combine two different values");
        }
        if (this.value != null) {
            this.oneOf = null;
            this.subsetOf = null;
            this.supersetOf = null;
            inferior.setOneOf(null);
            inferior.setSubsetOf(null);
            inferior.setSupersetOf(null);
        }

        this.combinePolicyCommon(inferior);

        if (this.value == null) {
            if (inferior.getValue() != null && ((this.oneOf != null && !this.oneOf.contains(inferior.getValue())) || (this.subsetOf != null && !this.subsetOf.contains(inferior.getValue())) || (this.supersetOf != null && !this.supersetOf.contains(inferior.getValue()))))
                throw new MetadataPolicyCombinationException("Not null inferior value must be containing in one_of,subset_of and superset_of, if one of these exist ");
            this.value = inferior.getValue();
        }
        if (notNullnotEqual(this.defaultValue, inferior.getDefaultValue())) {
            throw new MetadataPolicyCombinationException("Could not construct two different values");
        } else if (this.defaultValue == null) {
            this.defaultValue = inferior.getDefaultValue();
        }
        if (illegalDefaultValueCombination())
            throw new MetadataPolicyCombinationException("Not null default value must be containing in one_of,subset_of and superset_of, if one of these exist ");

        if (isNotAcceptedCombination(this.defaultValue, this.value))
            throw new MetadataPolicyCombinationException("False Policy Type Combination exists");

        return this;
    }

    public Policy<T> policyTypeCombination() throws MetadataPolicyCombinationException {
        // same rules as for combination
        if (illegalDefaultValueCombination())
            throw new MetadataPolicyCombinationException("Not null default value must be subset of one_of,subset_of and superset of superset_of, if one of these exist ");

        if (isNotAcceptedCombination(this.defaultValue, this.value))
            throw new MetadataPolicyCombinationException("False Policy Type Combination exists");

        if (this.value != null) {
            this.oneOf = null;
            this.subsetOf = null;
            this.supersetOf = null;
        }


        return this;
    }

    private boolean illegalDefaultValueCombination() {
        return this.defaultValue != null && ((this.oneOf != null && !this.oneOf.contains(this.defaultValue)) || (this.subsetOf != null && !this.subsetOf.contains(this.defaultValue)) || (this.supersetOf != null && !this.supersetOf.contains(this.defaultValue)));
    }

    private boolean notNullnotEqual(T superiorValue, T inferiorValue) {
        return superiorValue != null && inferiorValue != null && !superiorValue.equals(inferiorValue);
    }

    public T enforcePolicy(T t, String name) throws MetadataPolicyException {

        if (t == null && this.essential != null && this.essential)
            throw new MetadataPolicyException(name + " must exist in rp");

        // add ???
        if (this.add != null && t != null) {
            throw new MetadataPolicyException(name + " can not add extra value to an already existing single value");
        } else if (this.add != null) {
            t = this.add.iterator().next();
        }

        if (this.value != null) {
            return this.value;
        }

        if (this.defaultValue != null && t == null) {
            return this.defaultValue;
        }

        if (this.oneOf != null && ((t != null && !this.oneOf.contains(t)) || t == null))
            throw new MetadataPolicyException(name + " must have one values of " + String.join(COMMA, oneOf.toString()));
        if (this.supersetOf != null && (this.supersetOf.size() > 1 || (t != null && !this.supersetOf.contains(t))))
            throw new MetadataPolicyException(name + " value must be superset of " + String.join(COMMA, supersetOf.toString()));

        if (this.subsetOf != null && t != null && !this.subsetOf.contains(t)) t = null;

        return t;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(T defaultValue) {
        this.defaultValue = defaultValue;
    }

    public static class PolicyBuilder<T> {
        private Set<T> subset_of = new HashSet<>();
        private Set<T> one_of = new HashSet<>();
        private Set<T> superset_of = new HashSet<>();
        private Set<T> add = new HashSet<>();
        private T value;
        private T defaultValue;
        private Boolean essential;

        public PolicyBuilder<T> subsetOf(Set<T> subsetOfSet) {
            this.subset_of = subsetOfSet;
            return this;
        }

        public PolicyBuilder<T> addSubsetOf(T subsetOf) {
            this.subset_of.add(subsetOf);
            return this;
        }

        public PolicyBuilder<T> addOneOf(T oneOf) {
            this.one_of.add(oneOf);
            return this;
        }

        public PolicyBuilder<T> addSupersetOf(T supersetOf) {
            this.superset_of.add(supersetOf);
            return this;
        }

        public PolicyBuilder<T> addAdd(T add) {
            this.add.add(add);
            return this;
        }

        public PolicyBuilder<T> value(T value) {
            this.value = value;
            return this;
        }

        public PolicyBuilder<T> defaultValue(T defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public PolicyBuilder<T> essential(Boolean essential) {
            this.essential = essential;
            return this;
        }

        public Policy<T> build() {
            Policy<T> policy = new Policy<T>(this);
            return policy;
        }
    }
}

