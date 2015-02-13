package com.dell.software.ce.dib.claims;

import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.util.MultivaluedHashMap;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ClaimsTransformation implements ClaimsManipulation {
    private static final String defaultRedisNamespace = "kc:%s:claims:transforms";
    private static JedisPool jedisPool = null;
    Jedis jedis = null;

    public ClaimsTransformation(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    @Override
    public void initClaims(MultivaluedHashMap<String, String> claims, UserSessionModel userSession, ClientModel model, UserModel user) {
        if(jedis == null) {
            jedis = jedisPool.getResource();
        }

        List<ClaimMapping<String>> mappings = new ArrayList<>();

        //Realm Mappings
        mappings.addAll(getClaimMappings(getRealmKey(model)));

        //IDP Mappings
        //SAML IDP Claim
        String idp = claims.getFirst("http://schemas.software.dell.com/DellIdentityBroker/claims/authenticatingIdp");
        mappings.addAll(getClaimMappings(getIdpKey(model, idp)));

        //OIDC User Profile
        idp = claims.getFirst("authenticatingIdp");
        mappings.addAll(getClaimMappings(getIdpKey(model, idp)));

        //Application Mappings
        mappings.addAll(getClaimMappings(getApplicationKey(model)));

        //User Mappings
        mappings.addAll(getClaimMappings(getUserKey(model, user)));

        TransformableMultiMap<String, String> transformedClaims = new TransformableMultiMap<>(mappings);
        transformedClaims.mergeMap(claims);
        transformedClaims.transformMap(true);

        addClaims(claims, transformedClaims);
    }

    private void addClaims(MultivaluedHashMap<String, String> claims, TransformableMultiMap<String, String> transformedClaims) {
        if(claims == null || transformedClaims == null) {
            return;
        }

        claims.clear(); //Clear all current claims and use our transformed ones

        for (final Map.Entry<String, Object> entry : transformedClaims.entrySet()) {
            if(entry.getKey().isEmpty() || entry.getValue() == null) {
                continue;
            }

            List<String> values = (List<String>) entry.getValue();

            for(String item : values) {
                claims.add(entry.getKey(), item);
            }
        }
    }

    @Override
    public void close() {
        if(jedis != null) {
            jedis.close();
        }
    }

    private List<ClaimMapping<String>> getClaimMappings(String key) {
        List<ClaimMapping<String>> claimMappings = new ArrayList<>();
        Map<String, String> mappings = jedis.hgetAll(key);
        for(Map.Entry<String, String> entry : mappings.entrySet()) {
            claimMappings.add(new ClaimMapping<>(entry.getKey(), entry.getValue(), false));
        }

        return claimMappings;
    }

    private String getRealmKey(ClientModel model) {
        return getRedisKey(model.getRealm().getName());
    }

    private String getApplicationKey(ClientModel model) {
        return getRedisKey(String.format("%s:%s", model.getRealm().getName(), ((ApplicationModel) model).getName()));
    }

    private String getUserKey(ClientModel model, UserModel user) {
        return getRedisKey(String.format("%s:%s", model.getRealm().getName(), user.getEmail()));
    }

    private String getIdpKey(ClientModel model, String idp) {
        return getRedisKey(String.format("%s:%s", model.getRealm().getName(), idp));
    }

    private String getRedisKey(String relKey) {
        return String.format(defaultRedisNamespace, relKey);
    }
}
