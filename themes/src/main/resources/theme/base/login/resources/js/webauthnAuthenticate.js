import { base64url } from "rfc4648";

export async function authenticateByWebAuthn(input) {
    if (!input.isUserIdentified) {
        try {
            const result = await doAuthenticate([], input.challenge, input.userVerification, input.rpId, input.createTimeout, input.errmsg);
            returnSuccess(result);
        } catch (error) {
            returnFailure(error);
        }
        return;
    }
    checkAllowCredentials(input.challenge, input.userVerification, input.rpId, input.createTimeout, input.errmsg);
}

async function checkAllowCredentials(challenge, userVerification, rpId, createTimeout, errmsg) {
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
    try {
        const result = await doAuthenticate(allowCredentials, challenge, userVerification, rpId, createTimeout, errmsg);
        returnSuccess(result);
    } catch (error) {
        returnFailure(error);
    }
}

function doAuthenticate(allowCredentials, challenge, userVerification, rpId, createTimeout, errmsg) {
    // Check if WebAuthn is supported by this browser
    if (!window.PublicKeyCredential) {
        returnFailure(errmsg);
        return;
    }

    const publicKey = {
        rpId : rpId,
        challenge: base64url.parse(challenge, { loose: true })
    };

    if (createTimeout !== 0) {
        publicKey.timeout = createTimeout * 1000;
    }

    if (allowCredentials.length) {
        publicKey.allowCredentials = allowCredentials;
    }

    if (userVerification !== 'not specified') {
        publicKey.userVerification = userVerification;
    }

    return navigator.credentials.get({publicKey});
}

function returnSuccess(result) {
    document.getElementById("clientDataJSON").value = base64url.stringify(new Uint8Array(result.response.clientDataJSON), { pad: false });
    document.getElementById("authenticatorData").value = base64url.stringify(new Uint8Array(result.response.authenticatorData), { pad: false });
    document.getElementById("signature").value = base64url.stringify(new Uint8Array(result.response.signature), { pad: false });
    document.getElementById("credentialId").value = result.id;
    if (result.response.userHandle) {
        document.getElementById("userHandle").value = base64url.stringify(new Uint8Array(result.response.userHandle), { pad: false });
    }
    document.getElementById("webauth").submit();
}

function returnFailure(err) {
    document.getElementById("error").value = err;
    document.getElementById("webauth").submit();
}