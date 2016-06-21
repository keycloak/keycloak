package org.keycloak.examples.domainextension.rest.model;

import org.keycloak.examples.domainextension.entities.Company;

public class CompanyView {

    private String id;
    private String name;

    public CompanyView() {
    }

    public CompanyView(Company company) {
        id = company.getId();
        name = company.getName();
    }
    
    public String getId() {
		return id;
	}
    
    public String getName() {
		return name;
	}

}
