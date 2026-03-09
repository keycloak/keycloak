package org.keycloak.protocol.ssf.event.token;

import org.keycloak.Token;

import java.util.Map;

public interface SecurityEventToken extends Token {

    String getJti();

    String getIss();

    Integer getIat();

    String[] getAud();

    Map<String, Object> getEvents();
}
