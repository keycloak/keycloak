package org.keycloak.cache;

import java.util.List;
import java.util.Map;

import org.keycloak.models.ClientModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderQuery;
import org.keycloak.models.KeycloakSession;

public class DefaultAlternativeLookupProvider implements AlternativeLookupProvider {

    private final LocalCache<String, String> lookupCache;

    public DefaultAlternativeLookupProvider(LocalCache<String, String> lookupCache) {
        this.lookupCache = lookupCache;
    }

    public IdentityProviderModel lookupIdentityProviderFromIssuer(KeycloakSession session, String issuerUrl) {
        String alternativeKey = ComputedKey.computeKey(session.getContext().getRealm().getId(), "idp", issuerUrl);

        String cachedIdpAlias = lookupCache.get(alternativeKey);
        if (cachedIdpAlias != null) {
            IdentityProviderModel idp = session.identityProviders().getByAlias(cachedIdpAlias);
            if (idp != null && issuerUrl.equals(idp.getConfig().get(IdentityProviderModel.ISSUER))) {
                return idp;
            } else {
                lookupCache.invalidate(alternativeKey);
            }
        }

        List<IdentityProviderModel> idps = session.identityProviders().getAllStream(IdentityProviderQuery.any())
                .filter(i -> issuerUrl.equals(i.getConfig().get(IdentityProviderModel.ISSUER)))
                .limit(2)
                .toList();
        IdentityProviderModel idp = null;
        if (idps.size() == 1) {
            idp = idps.get(0);
            if (idp.getAlias() != null) {
                lookupCache.put(alternativeKey, idp.getAlias());
            }
        } else if (idps.size() > 1) {
            throw new RuntimeException("Multiple IDPs match the same issuer: " + idps.stream().map(IdentityProviderModel::getAlias).toList());
        }

        return idp;
    }

    public ClientModel lookupClientFromClientAttributes(KeycloakSession session, Map<String, String> attributes) {
        String alternativeKey = ComputedKey.computeKey(session.getContext().getRealm().getId(), "client", attributes);

        String cachedClientId = lookupCache.get(alternativeKey);
        if (cachedClientId != null) {
            ClientModel client = session.clients().getClientByClientId(session.getContext().getRealm(), cachedClientId);
            boolean match = client != null;
            if (match) {
                for (Map.Entry<String, String> e : attributes.entrySet()) {
                    if (!e.getValue().equals(client.getAttribute(e.getKey()))) {
                        match = false;
                        break;
                    }
                }
            }
            if (match) {
                return client;
            } else {
                lookupCache.invalidate(alternativeKey);
            }
        }

        ClientModel client = null;
        List<ClientModel> clients = session.clients().searchClientsByAttributes(session.getContext().getRealm(), attributes, 0, 2).toList();
        if (clients.size() == 1) {
            client = clients.get(0);
            lookupCache.put(alternativeKey, client.getClientId());
        } else if (clients.size() > 1) {
            throw new RuntimeException("Multiple clients matches attributes");
        }

        return client;
    }

    @Override
    public void close() {
    }
}
