package org.keycloak.testsuite.broker.util;

import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpRequest;

import org.apache.http.client.HttpClient;

public final class SimpleHttpDefault {

    private SimpleHttpDefault() {
    }

    public static SimpleHttpRequest doDelete(String url, HttpClient client) {
        return SimpleHttp.create(client).doDelete(url);
    }

    public static SimpleHttpRequest doPost(String url, HttpClient client) {
        return SimpleHttp.create(client).doPost(url);
    }

    public static SimpleHttpRequest doPut(String url, HttpClient client) {
        return SimpleHttp.create(client).doPut(url);
    }

    public static SimpleHttpRequest doGet(String url, HttpClient client) {
        return SimpleHttp.create(client).doGet(url);
    }

    public static SimpleHttpRequest doHead(String url, HttpClient client) {
        return SimpleHttp.create(client).doHead(url);
    }
}
