package org.keycloak.models;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ClientModel {
    long getAllowedClaimsMask();

    void setAllowedClaimsMask(long mask);

    UserModel getAgent();

    String getId();
}
