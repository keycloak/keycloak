package org.keycloak.models;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ClaimRequesterModel {
    long getAllowedClaimsMask();

    void setAllowedClaimsMask(long mask);
}
