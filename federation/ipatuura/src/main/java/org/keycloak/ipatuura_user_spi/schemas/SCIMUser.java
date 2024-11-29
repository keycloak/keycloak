/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.ipatuura_user_spi.schemas;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "Resources", "itemsPerPage", "schemas", "startIndex", "totalResults" })
@Generated("jsonschema2pojo")
public class SCIMUser {

    @JsonProperty("Resources")
    private List<Resource> resources = null;
    @JsonProperty("itemsPerPage")
    private Integer itemsPerPage;
    @JsonProperty("schemas")
    private List<String> schemas = null;
    @JsonProperty("startIndex")
    private Integer startIndex;
    @JsonProperty("totalResults")
    private Integer totalResults;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("Resources")
    public List<Resource> getResources() {
        return resources;
    }

    @JsonProperty("Resources")
    public void setResources(List<Resource> resources) {
        this.resources = resources;
    }

    @JsonProperty("itemsPerPage")
    public Integer getItemsPerPage() {
        return itemsPerPage;
    }

    @JsonProperty("itemsPerPage")
    public void setItemsPerPage(Integer itemsPerPage) {
        this.itemsPerPage = itemsPerPage;
    }

    @JsonProperty("schemas")
    public List<String> getSchemas() {
        return schemas;
    }

    @JsonProperty("schemas")
    public void setSchemas(List<String> schemas) {
        this.schemas = schemas;
    }

    @JsonProperty("startIndex")
    public Integer getStartIndex() {
        return startIndex;
    }

    @JsonProperty("startIndex")
    public void setStartIndex(Integer startIndex) {
        this.startIndex = startIndex;
    }

    @JsonProperty("totalResults")
    public Integer getTotalResults() {
        return totalResults;
    }

    @JsonProperty("totalResults")
    public void setTotalResults(Integer totalResults) {
        this.totalResults = totalResults;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({ "active", "emails", "groups", "id", "meta", "name", "schemas", "userName" })
    @Generated("jsonschema2pojo")
    public static class Resource {

        @JsonProperty("active")
        private Boolean active;
        @JsonProperty("emails")
        private List<Email> emails = null;
        @JsonProperty("groups")
        private List<Group> groups = null;
        @JsonProperty("id")
        private String id;
        @JsonProperty("meta")
        private Meta meta;
        @JsonProperty("name")
        private Name name;
        @JsonProperty("schemas")
        private List<String> schemas = null;
        @JsonProperty("userName")
        private String userName;
        @JsonIgnore
        private Map<String, Object> additionalProperties = new HashMap<String, Object>();

        @JsonProperty("active")
        public Boolean getActive() {
            return active;
        }

        @JsonProperty("active")
        public void setActive(boolean b) {
            this.active = b;
        }

        @JsonProperty("emails")
        public List<Email> getEmails() {
            return emails;
        }

        @JsonProperty("emails")
        public void setEmails(List<Email> emails) {
            this.emails = emails;
        }

        @JsonProperty("groups")
        public List<Group> getGroups() {
            return groups;
        }

        @JsonProperty("groups")
        public void setGroups(List<Group> groups) {
            this.groups = groups;
        }

        @JsonProperty("id")
        public String getId() {
            return id;
        }

        @JsonProperty("id")
        public void setId(String id) {
            this.id = id;
        }

        @JsonProperty("meta")
        public Meta getMeta() {
            return meta;
        }

        @JsonProperty("meta")
        public void setMeta(Meta meta) {
            this.meta = meta;
        }

        @JsonProperty("name")
        public Name getName() {
            return name;
        }

        @JsonProperty("name")
        public void setName(Name name) {
            this.name = name;
        }

        @JsonProperty("schemas")
        public List<String> getSchemas() {
            return schemas;
        }

        @JsonProperty("schemas")
        public void setSchemas(List<String> schemas) {
            this.schemas = schemas;
        }

        @JsonProperty("userName")
        public String getUserName() {
            return userName;
        }

        @JsonProperty("userName")
        public void setUserName(String userName) {
            this.userName = userName;
        }

        @JsonAnyGetter
        public Map<String, Object> getAdditionalProperties() {
            return this.additionalProperties;
        }

        @JsonAnySetter
        public void setAdditionalProperty(String name, Object value) {
            this.additionalProperties.put(name, value);
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonPropertyOrder({ "familyName", "givenName", "middleName" })
        @Generated("jsonschema2pojo")
        public static class Name {

            @JsonProperty("familyName")
            private String familyName;
            @JsonProperty("givenName")
            private String givenName;
            @JsonProperty("middleName")
            private String middleName;
            @JsonIgnore
            private Map<String, Object> additionalProperties = new HashMap<String, Object>();

            @JsonProperty("familyName")
            public String getFamilyName() {
                return familyName;
            }

            @JsonProperty("familyName")
            public void setFamilyName(String familyName) {
                this.familyName = familyName;
            }

            @JsonProperty("givenName")
            public String getGivenName() {
                return givenName;
            }

            @JsonProperty("givenName")
            public void setGivenName(String givenName) {
                this.givenName = givenName;
            }

            @JsonProperty("middleName")
            public String getMiddleName() {
                return middleName;
            }

            @JsonProperty("middleName")
            public void setMiddleName(String middleName) {
                this.middleName = middleName;
            }

            @JsonAnyGetter
            public Map<String, Object> getAdditionalProperties() {
                return this.additionalProperties;
            }

            @JsonAnySetter
            public void setAdditionalProperty(String name, Object value) {
                this.additionalProperties.put(name, value);
            }

        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonPropertyOrder({ "location", "resourceType" })
        @Generated("jsonschema2pojo")
        public static class Meta {

            @JsonProperty("location")
            private String location;
            @JsonProperty("resourceType")
            private String resourceType;
            @JsonIgnore
            private Map<String, Object> additionalProperties = new HashMap<String, Object>();

            @JsonProperty("location")
            public String getLocation() {
                return location;
            }

            @JsonProperty("location")
            public void setLocation(String location) {
                this.location = location;
            }

            @JsonProperty("resourceType")
            public String getResourceType() {
                return resourceType;
            }

            @JsonProperty("resourceType")
            public void setResourceType(String resourceType) {
                this.resourceType = resourceType;
            }

            @JsonAnyGetter
            public Map<String, Object> getAdditionalProperties() {
                return this.additionalProperties;
            }

            @JsonAnySetter
            public void setAdditionalProperty(String name, Object value) {
                this.additionalProperties.put(name, value);
            }
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonPropertyOrder({ "primary", "type", "value" })
        @Generated("jsonschema2pojo")
        public static class Email {

            @JsonProperty("primary")
            private Boolean primary;
            @JsonProperty("type")
            private String type;
            @JsonProperty("value")
            private String value;
            @JsonIgnore
            private Map<String, Object> additionalProperties = new HashMap<String, Object>();

            @JsonProperty("primary")
            public Boolean getPrimary() {
                return primary;
            }

            @JsonProperty("primary")
            public void setPrimary(Boolean primary) {
                this.primary = primary;
            }

            @JsonProperty("type")
            public String getType() {
                return type;
            }

            @JsonProperty("type")
            public void setType(String type) {
                this.type = type;
            }

            @JsonProperty("value")
            public String getValue() {
                return value;
            }

            @JsonProperty("value")
            public void setValue(String value) {
                this.value = value;
            }

            @JsonAnyGetter
            public Map<String, Object> getAdditionalProperties() {
                return this.additionalProperties;
            }

            @JsonAnySetter
            public void setAdditionalProperty(String name, Object value) {
                this.additionalProperties.put(name, value);
            }
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonPropertyOrder({ "$ref", "display", "value" })
        @Generated("jsonschema2pojo")
        public static class Group {

            @JsonProperty("$ref")
            private String $ref;
            @JsonProperty("display")
            private String display;
            @JsonProperty("value")
            private String value;
            @JsonIgnore
            private Map<String, Object> additionalProperties = new HashMap<String, Object>();

            @JsonProperty("$ref")
            public String get$ref() {
                return $ref;
            }

            @JsonProperty("$ref")
            public void set$ref(String $ref) {
                this.$ref = $ref;
            }

            @JsonProperty("display")
            public String getDisplay() {
                return display;
            }

            @JsonProperty("display")
            public void setDisplay(String display) {
                this.display = display;
            }

            @JsonProperty("value")
            public String getValue() {
                return value;
            }

            @JsonProperty("value")
            public void setValue(String value) {
                this.value = value;
            }

            @JsonAnyGetter
            public Map<String, Object> getAdditionalProperties() {
                return this.additionalProperties;
            }

            @JsonAnySetter
            public void setAdditionalProperty(String name, Object value) {
                this.additionalProperties.put(name, value);
            }

        }
    }
}
