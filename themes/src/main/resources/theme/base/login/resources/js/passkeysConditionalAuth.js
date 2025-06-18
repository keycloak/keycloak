import { base64url } from "rfc4648";
import { returnSuccess, signal } from "./webauthnAuthenticate.js";

export function initAuthenticate(input, availableCallback = (available) => {}) {
    // Check if WebAuthn is supported by this browser
    if (!window.PublicKeyCredential) {
        // Fail silently as WebAuthn Conditional UI is not required
        return;
    }
    if (input.isUserIdentified || typeof PublicKeyCredential.isConditionalMediationAvailable === "undefined") {
        availableCallback(false);
    } else {
        tryAutoFillUI(input, availableCallback);
    }
}

function doAuthenticate(input) {
    // Check if WebAuthn is supported by this browser
    if (!window.PublicKeyCredential) {
        // Fail silently as WebAuthn Conditional UI is not required
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
        signal: signal(),
        ...input.additionalOptions
    });
}

async function tryAutoFillUI(input, availableCallback = (available) => {}) {
    const isConditionalMediationAvailable = await PublicKeyCredential.isConditionalMediationAvailable();
    if (isConditionalMediationAvailable) {
        availableCallback(true);
        input.additionalOptions = { mediation: 'conditional'};
        try {
            const result = await doAuthenticate(input);
            returnSuccess(result);
        } catch {
            // Fail silently as WebAuthn Conditional UI is not required
        }
    } else {
        availableCallback(false);
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
