package org.keycloak.services.filters;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakTransaction;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakSessionServletFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest)servletRequest;

        KeycloakSessionFactory sessionFactory = (KeycloakSessionFactory) servletRequest.getServletContext().getAttribute(KeycloakSessionFactory.class.getName());
        KeycloakSession session = sessionFactory.create();
        ResteasyProviderFactory.pushContext(KeycloakSession.class, session);

        KeycloakTransaction tx = session.getTransaction();
        ResteasyProviderFactory.pushContext(KeycloakTransaction.class, tx);
        tx.begin();

        try {
            filterChain.doFilter(servletRequest, servletResponse);
            if (tx.isActive()) {
                if (tx.getRollbackOnly()) tx.rollback();
                else tx.commit();
            }
        } catch (IOException ex) {
            if (tx.isActive()) tx.rollback();
            throw ex;
        } catch (ServletException ex) {
            if (tx.isActive()) tx.rollback();
            throw ex;
        }
        catch (RuntimeException ex) {
            if (tx.isActive()) tx.rollback();
            throw new RuntimeException("request path: " + request.getRequestURI(), ex);
        } finally {
            session.close();
            ResteasyProviderFactory.clearContextData();
        }

    }

    @Override
    public void destroy() {
    }
}
