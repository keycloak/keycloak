package org.keycloak.cache;

import java.util.List;
import java.util.Map;

import org.keycloak.models.ClientModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderQuery;
import org.keycloak.models.KeycloakSession;

import com.github.benmanes.caffeine.cache.Cache;

public class DefaultAlternativeLookupProvider implements AlternativeLookupProvider {

    private final Cache<String, String> lookupCache;

    public DefaultAlternativeLookupProvider(Cache<String, String> lookupCache) {
        this.lookupCache = lookupCache;
    }

    public IdentityProviderModel lookupIdentityProviderFromIssuer(KeycloakSession session, String issuerUrl) {
        String alternativeKey = ComputedKey.computeKey(session.getContext().getRealm().getId(), "idp", issuerUrl);

        String cachedIdpAlias = lookupCache.getIfPresent(alternativeKey);
        if (cachedIdpAlias != null) {
            IdentityProviderModel idp = session.identityProviders().getByAlias(cachedIdpAlias);
            if (idp != null && issuerUrl.equals(idp.getConfig().get(IdentityProviderModel.ISSUER))) {
                return idp;
            } else {
                lookupCache.invalidate(alternativeKey);
            }
        }

        IdentityProviderModel idp = session.identityProviders().getAllStream(IdentityProviderQuery.any())
                .filter(i -> issuerUrl.equals(i.getConfig().get(IdentityProviderModel.ISSUER)))
                .findFirst().orElse(null);
        if (idp != null && idp.getAlias() != null) {
            lookupCache.put(alternativeKey, idp.getAlias());
        }
        return idp;
    }

    public ClientModel lookupClientFromClientAttributes(KeycloakSession session, Map<String, String> attributes) {
        String alternativeKey = ComputedKey.computeKey(session.getContext().getRealm().getId(), "client", attributes);

        String cachedClientId = lookupCache.getIfPresent(alternativeKey);
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
