package com.dell.software.ce.dib.claims;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

public class ClaimsTransformationFactory implements ClaimsManipulationFactory<ClaimsManipulation> {
    public static final String PROVIDER_ID = "claims_transformation";
    private static JedisPool jedisPool = null;

    public ClaimsTransformationFactory() {
        jedisPool = new JedisPool(new JedisPoolConfig(), "pub-redis-11078.us-east-1-2.3.ec2.garantiadata.com", 11078, Protocol.DEFAULT_TIMEOUT, "Lab4dev1" );
    }

    @Override
    public String getName() {
        return "Claims Transformation";
    }

    @Override
    public ClaimsManipulation create() {
        return new ClaimsTransformation(jedisPool);
    }

    @Override
    public ClaimsManipulation create(KeycloakSession session) {
        return create();
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void close() {
        jedisPool.close();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
