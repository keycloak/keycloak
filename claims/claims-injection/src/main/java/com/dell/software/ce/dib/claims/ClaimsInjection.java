package com.dell.software.ce.dib.claims;

import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.util.MultivaluedHashMap;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Map;

public class ClaimsInjection implements ClaimsManipulation {
    private static final String defaultRedisNamespace = "kc:%s:claims";
    private static JedisPool jedisPool = null;
    Jedis jedis = null;

    public ClaimsInjection(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    @Override
    public void initClaims(MultivaluedHashMap<String, String> claims, UserSessionModel userSession, ClientModel model, UserModel user) {
        if(jedis == null) {
            jedis = jedisPool.getResource();
        }

        //User Claims
        addClaims(claims, jedis.hgetAll(getUserKey(model, user)));

        //Application Claims
        addClaims(claims, jedis.hgetAll(getApplicationKey(model)));

        //IDP Claims
        //SAML IDP Claim
        String idp = claims.getFirst("http://schemas.software.dell.com/DellIdentityBroker/claims/authenticatingIdp");
        addClaims(claims, jedis.hgetAll(getIdpKey(model, idp)));

        //OIDC User Profile
        idp = claims.getFirst("authenticatingIdp");
        addClaims(claims, jedis.hgetAll(getIdpKey(model, idp)));

        //Realm Claims
        addClaims(claims, jedis.hgetAll(getRealmKey(model)));
    }

    private void addClaims(MultivaluedHashMap<String, String> claims, Map<String, String> injectedClaims) {
        if(claims == null || injectedClaims == null) {
            return;
        }

        for (Map.Entry<String, String> claim : injectedClaims.entrySet()) {
            claims.add(claim.getKey(), claim.getValue());
        }
    }

    @Override
    public void close() {
        if(jedis != null) {
            jedis.close();
        }
    }

    private String getRealmKey(ClientModel model) {
        return getRedisKey(model.getRealm().getName());
    }

    private String getApplicationKey(ClientModel model) {
        return getRedisKey(String.format("%s:%s", model.getRealm().getName(),((ApplicationModel) model).getName()));
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
