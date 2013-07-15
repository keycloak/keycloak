package org.keycloak.services.filters;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.picketlink.idm.IdentitySession;
import org.picketlink.idm.IdentitySessionFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;
import java.io.IOException;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@PreMatching
public class IdentitySessionFilter implements ContainerRequestFilter, ContainerResponseFilter {
    protected IdentitySessionFactory factory;

    public IdentitySessionFilter(IdentitySessionFactory factory) {
        this.factory = factory;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        IdentitySession ctx = factory.createIdentitySession();
        requestContext.setProperty(IdentitySession.class.getName(), ctx);
        ResteasyProviderFactory.pushContext(IdentitySession.class, ctx);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        IdentitySession ctx = (IdentitySession)requestContext.getProperty(IdentitySession.class.getName());
        if (ctx != null) ctx.close();
    }
}
