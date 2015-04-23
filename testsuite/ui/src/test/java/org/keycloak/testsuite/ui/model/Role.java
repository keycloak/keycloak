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
public class Role {

    private String name;
    private boolean composite;
    private String description;

    public Role() {
    }

    public Role(String name) {
        this(name, false, "");
    }
    
    public Role(String name, boolean composite) {
        this(name, composite, "");
    }
    
    public Role(String name, boolean composite, String description) {
        this.name = name;
        this.composite = composite;
        this.description = description;
    }

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isComposite() {
		return composite;
	}

	public void setComposite(boolean composite) {
		this.composite = composite;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}
