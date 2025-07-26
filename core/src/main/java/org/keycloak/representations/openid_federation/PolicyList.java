package org.keycloak.representations.openid_federation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.exceptions.MetadataPolicyCombinationException;
import org.keycloak.exceptions.MetadataPolicyException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class PolicyList<T> extends AbstractPolicy<T> {

    private static final String COMMA = ",";

    private Set<T> value;

    @JsonProperty("default")
    private Set<T> defaultValue;

    public PolicyList() {

    }

    public PolicyList<T> combinePolicy(PolicyList<T> inferior) throws MetadataPolicyCombinationException {

        if (inferior == null) {
            return this;
        }

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
            if (inferior.getValue() != null && ((this.oneOf != null && !this.oneOf.containsAll(inferior.getValue())) || (this.subsetOf != null && !this.subsetOf.containsAll(inferior.getValue())) || (this.supersetOf != null && !this.supersetOf.containsAll(inferior.getValue()))))
                throw new MetadataPolicyCombinationException("Inferior value must be subset of one_of,subset_of and superset_of, if one of these exist ");
            this.value = inferior.getValue();
        }

        if (notNullnotEqual(this.defaultValue, inferior.getDefaultValue())) {
            throw new MetadataPolicyCombinationException("Could not construct two different values");
        } else if (this.defaultValue == null) {
            this.defaultValue = inferior.getDefaultValue();
        }

        if (illegalDefaultValueCombination())
            throw new MetadataPolicyCombinationException("Not null default value must be subset of one_of,subset_of and superset of superset_of, if one of these exist ");

        if (isNotAcceptedCombination(this.defaultValue, this.value))
            throw new MetadataPolicyCombinationException("False Policy Type Combination exists");

        return this;
    }

    public PolicyList<T> policyTypeCombination() throws MetadataPolicyCombinationException {
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
        return this.defaultValue != null && ((this.oneOf != null && !this.oneOf.containsAll(this.defaultValue)) || (this.subsetOf != null && !this.subsetOf.containsAll(this.defaultValue)) || (this.supersetOf != null && !this.defaultValue.containsAll(this.supersetOf)));
    }

    private boolean notNullnotEqual(Set<T> superiorValue, Set<T> inferiorValue) {
        return superiorValue != null && inferiorValue != null && !(superiorValue.size() == inferiorValue.size() && superiorValue.containsAll(inferiorValue));
    }

    public List<T> enforcePolicy(List<T> t, String name) throws MetadataPolicyException {

        if (t == null && this.essential != null && this.essential)
            throw new MetadataPolicyException(name + " must exist in rp");

        //add can only exist alone
        if (this.add != null) {
            if (t == null) t = new ArrayList<>();
            for (T val : this.add) {
                if (!t.contains(val)) t.add(val);
            }
            return t;
        }

        if (this.value != null) {
            return new ArrayList<>(this.value);
        }

        if (this.defaultValue != null && t == null) {
            return new ArrayList<>(this.defaultValue);
        }

        if (this.oneOf != null && (t == null || !this.oneOf.containsAll(t)))
            throw new MetadataPolicyException(name + " must have one values of " + this.oneOf.stream().map(String::valueOf).collect(Collectors.joining(COMMA)));
        if (this.supersetOf != null && (t == null || !t.containsAll(this.supersetOf)))
            throw new MetadataPolicyException(name + " values must be superset of " + this.supersetOf.stream().map(String::valueOf).collect(Collectors.joining(COMMA)));


        if (this.subsetOf != null && t != null) {
            return t.stream().filter(e -> this.subsetOf.contains(e)).collect(Collectors.toList());
        }

        return t;
    }

    public Set<T> getValue() {
        return value;
    }

    public void setValue(Set<T> value) {
        this.value = value;
    }

    public Set<T> getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Set<T> defaultValue) {
        this.defaultValue = defaultValue;
    }

}
