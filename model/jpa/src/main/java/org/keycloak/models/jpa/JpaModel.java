package org.keycloak.models.jpa;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface JpaModel<T> {
    T getEntity();
}
