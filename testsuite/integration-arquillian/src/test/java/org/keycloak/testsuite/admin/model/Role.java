/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.testsuite.admin.model;

/**
 *
 * @author Petr Mensik
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
