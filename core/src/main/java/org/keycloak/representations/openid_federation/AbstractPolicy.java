package org.keycloak.representations.openid_federation;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AbstractPolicy <T> {

    @JsonProperty("subset_of")
    protected Set<T> subsetOf;

    @JsonProperty("one_of")
    protected Set<T> oneOf;

    @JsonProperty("superset_of")
    protected Set<T> supersetOf;

    protected Set<T> add;

    protected Boolean essential;

    protected Map<String, T> otherClaims = new HashMap<>();

    protected AbstractPolicy() {

    }

    protected AbstractPolicy<T> combinePolicyCommon(AbstractPolicy<T> inferior) {

        // combine subset_of
        if (inferior.getSubsetOf() != null && this.subsetOf != null) {
            this.subsetOf = this.subsetOf.stream().filter(inferior.getSubsetOf()::contains).collect(Collectors.toSet());
            if (this.subsetOf.isEmpty()) {
                this.subsetOf = null;
            }
        } else if (inferior.getSubsetOf() != null) {
            this.subsetOf = inferior.getSubsetOf();
        }
        // combine one_of
        if (inferior.getOneOf() != null && this.oneOf != null) {
            this.oneOf = this.oneOf.stream().filter(inferior.getOneOf()::contains).collect(Collectors.toSet());
            if (this.oneOf.isEmpty()) {
                this.oneOf = null;
            }
        } else if (inferior.getOneOf() != null) {
            this.oneOf = inferior.getOneOf();
        }
        // combine superset_of
        if (inferior.getSupersetOf() != null && this.supersetOf != null) {
            this.supersetOf = this.supersetOf.stream().filter(inferior.getSupersetOf()::contains).collect(Collectors.toSet());
            if (this.supersetOf.isEmpty()) {
                this.supersetOf = null;
            }
        } else if (inferior.getSupersetOf() != null) {
            this.supersetOf = inferior.getSupersetOf();
        }
        // combine add
        if (this.add == null) {
            this.add = inferior.getAdd();
        } else if (inferior.getAdd() != null) {
            this.add.addAll(inferior.getAdd());
        }
        //combine essential
        if (this.essential != null || inferior.getEssential() != null) {
            this.essential = this.essential == null || inferior.getEssential() == null || this.essential || inferior.getEssential();
        }
        return this;
    }

    protected boolean isNotAcceptedCombination(Object defaultValue, Object value) {
        return (this.add != null && (defaultValue != null || value != null || this.oneOf != null || this.subsetOf != null || this.supersetOf != null)) || (defaultValue != null && value != null) || (this.oneOf != null && (this.subsetOf != null || this.supersetOf != null)) || (this.subsetOf != null && this.supersetOf != null && !this.subsetOf.containsAll(this.supersetOf));
    }

    public Set<T> getSubsetOf() {
        return subsetOf;
    }

    public void setSubsetOf(Set<T> subsetOf) {
        this.subsetOf = subsetOf;
    }

    public Set<T> getOneOf() {
        return oneOf;
    }

    public void setOneOf(Set<T> oneOf) {
        this.oneOf = oneOf;
    }

    public Set<T> getSupersetOf() {
        return supersetOf;
    }

    public void setSupersetOf(Set<T> supersetOf) {
        this.supersetOf = supersetOf;
    }

    public Set<T> getAdd() {
        return add;
    }

    public void setAdd(Set<T> add) {
        this.add = add;
    }

    public Boolean getEssential() {
        return essential;
    }

    public void setEssential(Boolean essential) {
        this.essential = essential;
    }

    @JsonAnyGetter
    public Map<String, T> getOtherClaims() {
        return otherClaims;
    }

    @JsonAnySetter
    public void setOtherClaims(Map<String, T> otherClaims) {
        this.otherClaims = otherClaims;
    }

}

