package org.keycloak.examples.domainextension.rest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.keycloak.examples.domainextension.entities.Company;
import org.keycloak.examples.domainextension.rest.model.CompanyView;
import org.keycloak.examples.domainextension.services.ExampleService;
import org.keycloak.models.KeycloakSession;

public class CompanyResource {

	private KeycloakSession session;
	
	public CompanyResource(KeycloakSession session) {
		this.session = session;
	}

    @GET
    @Path("")
    public Set<CompanyView> getMasterAccounts() {
        List<Company> companies = session.getProvider(ExampleService.class).listCompanies();
        Set<CompanyView> companyViews = new HashSet<>();
        for (Company company : companies) {
        	companyViews.add(new CompanyView(company));
        }
        return companyViews;
    }

    @GET
    @Path("{id}")
    public CompanyView getCompany(@PathParam("id") final String id) {
        return new CompanyView(session.getProvider(ExampleService.class).findCompany(id));
    }

}