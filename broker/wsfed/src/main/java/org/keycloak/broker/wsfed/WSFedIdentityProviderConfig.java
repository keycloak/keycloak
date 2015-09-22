/*
 * Copyright (C) 2015 Dell, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.broker.wsfed;

import org.keycloak.wsfed.common.WSFedConstants;
import org.keycloak.models.IdentityProviderModel;

public class WSFedIdentityProviderConfig extends IdentityProviderModel {

    public WSFedIdentityProviderConfig() {
    }

    public WSFedIdentityProviderConfig(IdentityProviderModel identityProviderModel) {
        super(identityProviderModel);
    }

    public String getWsFedUrl() {
        return getConfig().get("wsfedUrl");
    }

    public void setWsFedUrl(String wsFedUrl) {
        getConfig().put("wsfedUrl", wsFedUrl);
    }

    public String getSingleLogoutServiceUrl() {
        return getConfig().get("singleLogoutServiceUrl");
    }

    public void setSingleLogoutServiceUrl(String singleLogoutServiceUrl) {
        getConfig().put("singleLogoutServiceUrl", singleLogoutServiceUrl);
    }

    public boolean isValidateSignature() {
        return Boolean.valueOf(getConfig().get("validateSignature"));
    }

    public void setValidateSignature(boolean validateSignature) {
        getConfig().put("validateSignature", String.valueOf(validateSignature));
    }

    public int getFreshness() {
        return Integer.parseInt(getConfig().get(WSFedConstants.WSFED_FRESHNESS));
    }

    public void setFreshness(boolean freshess) {
        getConfig().put(WSFedConstants.WSFED_FRESHNESS, String.valueOf(freshess));
    }

    public String getSigningCertificate() {
        return getConfig().get("signingCertificate");
    }

    public void setSigningCertificate(String signingCertificate) {
        getConfig().put("signingCertificate", signingCertificate);
    }

    public String getEncryptionPublicKey() {
        return getConfig().get("encryptionPublicKey");
    }

    public void setEncryptionPublicKey(String encryptionPublicKey) {
        getConfig().put("encryptionPublicKey", encryptionPublicKey);
    }

    public String getWsFedRealm() {
        return getConfig().get("wsfedRealm");
    }

    public void setWsFedRealm(String wsfedRealm) {
        getConfig().put("wsfedRealm", wsfedRealm);
    }

    public boolean isBackchannelSupported() {
        return Boolean.valueOf(getConfig().get("backchannelSupported"));
    }

    public void setBackchannelSupported(boolean backchannel) {
        getConfig().put("backchannelSupported", String.valueOf(backchannel));
    }

    public boolean handleEmptyActionAsLogout() {
        return Boolean.valueOf(getConfig().get("emptyActionHandledAsLogout"));
    }

    public void setHandleEmptyActionAsLogout(boolean handleAsLogout) {
        getConfig().put("emptyActionHandledAsLogout", String.valueOf(handleAsLogout));
    }
}
