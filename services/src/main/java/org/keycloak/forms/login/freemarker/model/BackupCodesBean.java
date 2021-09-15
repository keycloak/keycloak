package org.keycloak.forms.login.freemarker.model;

import org.keycloak.common.util.RandomString;
import org.keycloak.common.util.Time;

import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BackupCodesBean {

    private static final int NUMBER_OF_CODES = 2;

    private final List<String> codes;
    private final long generatedAt;
    private final RandomString randomString = new RandomString(4, new SecureRandom());

    public BackupCodesBean() {
        this.codes = Stream.generate(this::newCode).limit(NUMBER_OF_CODES).collect(Collectors.toList());
        this.generatedAt = Time.currentTimeMillis();
    }

    private String newCode() {
        return String.join("-", randomString.nextString(), randomString.nextString(), randomString.nextString());
    }

    public List<String> getCodes() {
        return codes;
    }

    public String getBackupCodesList() {
        return String.join(",", codes);
    }

    public long getGeneratedAt() {
        return generatedAt;
    }

}
