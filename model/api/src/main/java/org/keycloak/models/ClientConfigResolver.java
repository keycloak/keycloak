package org.keycloak.models;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClientConfigResolver {
    protected ClientModel client;
    protected ClientTemplateModel clientTemplate;

    public ClientConfigResolver(ClientModel client) {
        this.client = client;
        this.clientTemplate = client.getClientTemplate();
    }

    public String resolveAttribute(String name) {
        if (clientTemplate != null && client.useTemplateConfig()) {
            return clientTemplate.getAttribute(name);
        } else {
            return client.getAttribute(name);
        }
    }

    public boolean isFrontchannelLogout() {
        if (clientTemplate != null && client.useTemplateConfig()) {
            return clientTemplate.isFrontchannelLogout();
        }

        return client.isFrontchannelLogout();
    }

    boolean isConsentRequired() {
        if (clientTemplate != null && client.useTemplateConfig()) {
            return clientTemplate.isConsentRequired();
        }

        return client.isConsentRequired();
    }

    boolean isStandardFlowEnabled() {
        if (clientTemplate != null && client.useTemplateConfig()) {
            return clientTemplate.isStandardFlowEnabled();
        }

        return client.isStandardFlowEnabled();
    }

    boolean isServiceAccountsEnabled() {
        if (clientTemplate != null && client.useTemplateConfig()) {
            return clientTemplate.isServiceAccountsEnabled();
        }

        return client.isServiceAccountsEnabled();
    }
}
