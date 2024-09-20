import { base64url } from "rfc4648";
import { returnSuccess, returnFailure } from "./webauthnAuthenticate.js";

export function initAuthenticate(input) {
    // Check if WebAuthn is supported by this browser
    if (!window.PublicKeyCredential) {
        returnFailure(input.errmsg);
        return;
    }
    if (input.isUserIdentified || typeof PublicKeyCredential.isConditionalMediationAvailable === "undefined") {
        document.getElementById("kc-form-passkey-button").style.display = 'block';
    } else {
        tryAutoFillUI(input);
    }
}

function doAuthenticate(input) {
    // Check if WebAuthn is supported by this browser
    if (!window.PublicKeyCredential) {
        returnFailure(input.errmsg);
        return;
    }

    const publicKey = {
        rpId : input.rpId,
        challenge: base64url.parse(input.challenge, { loose: true })
    };

    publicKey.allowCredentials = !input.isUserIdentified ? [] : getAllowCredentials();

    if (input.createTimeout !== 0) {
        publicKey.timeout = input.createTimeout * 1000;
    }

    if (input.userVerification !== 'not specified') {
        publicKey.userVerification = input.userVerification;
    }

    return navigator.credentials.get({
        publicKey: publicKey,
        ...input.additionalOptions
    });
}

async function tryAutoFillUI(input) {
    const isConditionalMediationAvailable = await PublicKeyCredential.isConditionalMediationAvailable();
    if (isConditionalMediationAvailable) {
        document.getElementById("kc-form-login").style.display = "block";
        input.additionalOptions = { mediation: 'conditional'};
        try {
            const result = await doAuthenticate(input);
            returnSuccess(result);
        } catch (error) {
            returnFailure(error);
        }
    } else {
        document.getElementById("kc-form-passkey-button").style.display = 'block';
    }
}

function getAllowCredentials() {
    const allowCredentials = [];
    const authnUse = document.forms['authn_select'].authn_use_chk;
    if (authnUse !== undefined) {
        if (authnUse.length === undefined) {
            allowCredentials.push({
                id: base64url.parse(authnUse.value, {loose: true}),
                type: 'public-key',
            });
        } else {
            authnUse.forEach((entry) =>
                allowCredentials.push({
                    id: base64url.parse(entry.value, {loose: true}),
                    type: 'public-key',
                }));
        }
    }
    return allowCredentials;
}
