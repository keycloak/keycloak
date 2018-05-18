/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.adapters.osgi;

import java.security.SecureRandom;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author hmlnarik
 */
public class PaxWebSecurityConstraintMapping {

    private String constraintName = "Constraint-" + new SecureRandom().nextInt(Integer.MAX_VALUE);
    private String mapping;
    private String url;
    private String dataConstraint = "NONE";
    private boolean authentication = true;
    private List<String> roles = new LinkedList<>();

    public String getConstraintName() {
        return constraintName;
    }

    public void setConstraintName(String constraintName) {
        this.constraintName = constraintName;
    }

    public String getMapping() {
        return mapping;
    }

    public void setMapping(String mapping) {
        this.mapping = mapping;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDataConstraint() {
        return dataConstraint;
    }

    public void setDataConstraint(String dataConstraint) {
        this.dataConstraint = dataConstraint;
    }

    public boolean isAuthentication() {
        return authentication;
    }

    public void setAuthentication(boolean authentication) {
        this.authentication = authentication;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

}
