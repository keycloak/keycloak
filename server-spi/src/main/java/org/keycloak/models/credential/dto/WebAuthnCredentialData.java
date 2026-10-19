/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.keycloak.models.credential.dto;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class WebAuthnCredentialData {

    private final String aaguid;
    private final String credentialId;
    private long counter;
    private String attestationStatement;
    private String credentialPublicKey;
    private String attestationStatementFormat;
    private Set<String> transports;
    // WebAuthn Level 3 backup flags. null for credentials registered before the flags were recorded.
    private Boolean backupEligible;
    private Boolean backupState;

    public WebAuthnCredentialData(String aaguid, String credentialId, long counter, String attestationStatement,
                                  String credentialPublicKey, String attestationStatementFormat, Set<String> transports) {
        this(aaguid, credentialId, counter, attestationStatement, credentialPublicKey, attestationStatementFormat, transports, null, null);
    }

    @JsonCreator
    public WebAuthnCredentialData(@JsonProperty("aaguid") String aaguid,
                                  @JsonProperty("credentialId") String credentialId,
                                  @JsonProperty("counter") long counter,
                                  @JsonProperty("attestationStatement") String attestationStatement,
                                  @JsonProperty("credentialPublicKey") String credentialPublicKey,
                                  @JsonProperty("attestationStatementFormat") String attestationStatementFormat,
                                  @JsonProperty("transports") Set<String> transports,
                                  @JsonProperty("backupEligible") Boolean backupEligible,
                                  @JsonProperty("backupState") Boolean backupState) {
        this.aaguid = aaguid;
        this.credentialId = credentialId;
        this.counter = counter;
        this.attestationStatement = attestationStatement;
        this.credentialPublicKey = credentialPublicKey;
        this.attestationStatementFormat = attestationStatementFormat;
        this.transports = transports;
        this.backupEligible = backupEligible;
        this.backupState = backupState;
    }

    public String getAaguid() {
        return aaguid;
    }

    public String getCredentialId() {
        return credentialId;
    }

    public String getAttestationStatement() {
        return attestationStatement;
    }

    public String getCredentialPublicKey() {
        return credentialPublicKey;
    }

    public long getCounter() {
        return counter;
    }

    public void setCounter(long counter) {
        this.counter = counter;
    }

    public String getAttestationStatementFormat() {
        return attestationStatementFormat;
    }

    public void setAttestationStatementFormat(String attestationStatementFormat) {
        this.attestationStatementFormat = attestationStatementFormat;
    }

    public Set<String> getTransports() {
        return transports != null ? transports : Collections.emptySet();
    }

    public void setTransports(Set<String> transports) {
        this.transports = transports;
    }

    /**
     * Backup Eligible (BE) flag reported by the authenticator. {@code true} means the credential may be synced to
     * other devices (a "synced passkey"), {@code false} means the private key is bound to the authenticator
     * (a "device-bound passkey"). {@code null} when unknown, for credentials registered before the flag was recorded.
     */
    public Boolean getBackupEligible() {
        return backupEligible;
    }

    public void setBackupEligible(Boolean backupEligible) {
        this.backupEligible = backupEligible;
    }

    /**
     * Backup State (BS) flag reported by the authenticator in the last assertion: whether the credential is currently
     * backed up. Only meaningful when {@link #getBackupEligible()} is {@code true}.
     */
    public Boolean getBackupState() {
        return backupState;
    }

    public void setBackupState(Boolean backupState) {
        this.backupState = backupState;
    }

    @Override
    public String toString() {
        return "WebAuthnCredentialData { " +
                "aaguid='" + aaguid + '\'' +
                ", credentialId='" + credentialId + '\'' +
                ", counter=" + counter +
                ", credentialPublicKey=" + credentialPublicKey +
                ", attestationStatement='" + attestationStatement + '\'' +
                ", credentialPublicKey='" + credentialPublicKey + '\'' +
                ", attestationStatementFormat='" + attestationStatementFormat + '\'' +
                ", transports=" + Arrays.toString(getTransports().toArray()) +
                ", backupEligible=" + backupEligible +
                ", backupState=" + backupState +
                " }";
    }
}
