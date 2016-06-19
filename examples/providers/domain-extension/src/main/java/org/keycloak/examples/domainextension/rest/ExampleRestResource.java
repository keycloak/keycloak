package org.keycloak.examples.domainextension.rest;

import javax.ws.rs.Path;

import org.keycloak.models.KeycloakSession;

public class ExampleRestResource {

	private KeycloakSession session;
	
	public ExampleRestResource(KeycloakSession session) {
		this.session = session;
	}
	
    @Path("companies")
    public CompanyResource getCompanyResource() {
        return new CompanyResource(session);
    }

}
