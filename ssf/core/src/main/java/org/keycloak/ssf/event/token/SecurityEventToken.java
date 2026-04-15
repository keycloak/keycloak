package org.keycloak.ssf.event.token;

import java.util.Map;

import org.keycloak.Token;

public interface SecurityEventToken extends Token {

    String getJti();

    String getIss();

    Integer getIat();

    String[] getAud();

    Map<String, Object> getEvents();
}
