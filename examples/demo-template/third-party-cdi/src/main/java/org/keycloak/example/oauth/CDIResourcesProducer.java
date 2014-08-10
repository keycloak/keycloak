package org.keycloak.example.oauth;

import org.keycloak.servlet.ServletOAuthClient;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CDIResourcesProducer {

    @Produces
    @RequestScoped
    public FacesContext produceFacesContext() {
        return FacesContext.getCurrentInstance();
    }

    @Produces
    @RequestScoped
    @ServletRequestQualifier
    public HttpServletRequest produceServletRequest() {
        return (HttpServletRequest)FacesContext.getCurrentInstance().getExternalContext().getRequest();
    }

    @Produces
    @RequestScoped
    public HttpServletResponse produceServletResponse() {
        return (HttpServletResponse)FacesContext.getCurrentInstance().getExternalContext().getResponse();
    }

    @Produces
    @ApplicationScoped
    public ServletOAuthClient produceOAuthClient() {
        return new ServletOAuthClient();
    }
}
