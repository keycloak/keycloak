package org.keycloak.scim.resource.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnterpriseUser {

    @JsonProperty("employeeNumber")
    private String employeeNumber;

    @JsonProperty("costCenter")
    private String costCenter;

    @JsonProperty("organization")
    private String organization;

    @JsonProperty("division")
    private String division;

    @JsonProperty("department")
    private String department;

    @JsonProperty("manager")
    private Manager manager;

    public String getEmployeeNumber() {
        return employeeNumber;
    }

    public void setEmployeeNumber(String employeeNumber) {
        this.employeeNumber = employeeNumber;
    }

    public String getCostCenter() {
        return costCenter;
    }

    public void setCostCenter(String costCenter) {
        this.costCenter = costCenter;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getDivision() {
        return division;
    }

    public void setDivision(String division) {
        this.division = division;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Manager getManager() {
        return manager;
    }

    public void setManager(Manager manager) {
        this.manager = manager;
    }

    /**
     * Represents the manager of the user as defined in RFC 7643 Section 4.3
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Manager {

        @JsonProperty("value")
        private String value;

        @JsonProperty("$ref")
        private String ref;

        @JsonProperty("displayName")
        private String displayName;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getRef() {
            return ref;
        }

        public void setRef(String ref) {
            this.ref = ref;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
    }
}
