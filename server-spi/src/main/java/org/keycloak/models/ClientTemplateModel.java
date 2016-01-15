package org.keycloak.models;

import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ClientTemplateModel extends ProtocolMapperContainerModel, ScopeContainerModel {
    String getId();

    String getName();

    RealmModel getRealm();
    void setName(String name);

    String getDescription();

    void setDescription(String description);

    String getProtocol();
    void setProtocol(String protocol);

    void setAttribute(String name, String value);
    void removeAttribute(String name);
    String getAttribute(String name);
    Map<String, String> getAttributes();

    boolean isFrontchannelLogout();
    void setFrontchannelLogout(boolean flag);

    boolean isBearerOnly();
    void setBearerOnly(boolean only);

    boolean isPublicClient();
    void setPublicClient(boolean flag);

    boolean isConsentRequired();
    void setConsentRequired(boolean consentRequired);

    boolean isStandardFlowEnabled();
    void setStandardFlowEnabled(boolean standardFlowEnabled);

    boolean isImplicitFlowEnabled();
    void setImplicitFlowEnabled(boolean implicitFlowEnabled);

    boolean isDirectAccessGrantsEnabled();
    void setDirectAccessGrantsEnabled(boolean directAccessGrantsEnabled);

    boolean isServiceAccountsEnabled();
    void setServiceAccountsEnabled(boolean serviceAccountsEnabled);



}
