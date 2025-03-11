package dev.tockl.infinispan.mapper;

import org.infinispan.persistence.keymappers.TwoWayKey2StringMapper;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureKey;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginFailureKey2StringMapper implements TwoWayKey2StringMapper {
    private static final Pattern pattern = Pattern.compile("LoginFailureKey \\[ realmId=(.*)\\. userId=(.*) ]");

    @Override
    public boolean isSupportedType(Class<?> keyType) {
        return keyType == LoginFailureKey.class;
    }

    @Override
    public String getStringMapping(Object key) {
        if (!isSupportedType(key.getClass()))
            throw new IllegalArgumentException("Unsupported key type: " + key.getClass().getName());

        LoginFailureKey object = (LoginFailureKey) key;
        return object.toString();
    }

    @Override
    public Object getKeyMapping(String stringKey) {
        Matcher matcher = pattern.matcher(stringKey);

        if (matcher.find() && matcher.groupCount() == 2) {
            String realmId = matcher.group(1);
            String userId = matcher.group(2);

            return new LoginFailureKey(realmId, userId);
        }

        throw new IllegalArgumentException("Unsupported key type: " + stringKey);
    }
}
