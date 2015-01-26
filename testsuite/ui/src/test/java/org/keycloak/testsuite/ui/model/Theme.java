/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.keycloak.testsuite.ui.model;

/**
 *
 * @author pmensik
 */
public enum Theme {
    
    BASE("base"), KEYCLOAK("keycloak"), PATTERNFLY("patternfly");
    
    private final String name;
    
    private Theme(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    
}
