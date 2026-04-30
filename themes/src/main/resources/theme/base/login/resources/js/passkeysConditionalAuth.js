import { doAuthenticate, returnSuccess } from "./webauthnAuthenticate.js";

const PASSKEY_MODAL_DISMISSED = 'kc_passkey_modal_dismissed';

/**
 * Returns the current cookie KC_AUTH_SESSION_HASH value if present.
 * Undefined if not present.
 */
function getModalDismissedHash() {
    for (const cookie of document.cookie.split(';')) {
        const [key, value] = cookie.trim().split('=');
        if (key === 'KC_AUTH_SESSION_HASH' && value) {
            return value;
        }
    }
    return undefined;
}

/**
 * Entry point for passkey authentication on page load.
 *
 * Calls navigator.credentials.get() once with the mediation value configured
 * in the WebAuthn Passwordless Policy (conditional/none/optional/required/silent).
 * For "none", unsupported browsers, or an already-identified user, nothing is
 * attempted automatically — the user can always initiate via the button.
 *
 * For modal mediations (optional/required), the dialog is shown at most once
 * per authentication session: if the user dismisses it, it will not reappear
 * on subsequent page loads (e.g. after a failed password attempt).
 */
export async function initAuthenticate(input, availableCallback = () => {}) {
    // Check if WebAuthn is supported by this browser
    if (!window.PublicKeyCredential) {
        // Fail silently as WebAuthn Conditional UI is not required
        return;
    }

    const mediation = input.mediation ?? 'conditional';

    if (input.isUserIdentified || mediation === 'none') {
        availableCallback(false);
        return;
    }

    if (input.authenticatorAttachment === 'platform'
            && !await PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable()) {
        availableCallback(false);
        return;
    }

    // The isConditionalMediationAvailable() check is only relevant for
    // conditional (autofill) mediation — other modes do not depend on it.
    if (mediation === 'conditional') {
        if (typeof PublicKeyCredential.isConditionalMediationAvailable === 'undefined') {
            availableCallback(false);
            return;
        }
        const isAvailable = await PublicKeyCredential.isConditionalMediationAvailable();
        if (!isAvailable) {
            // Treat unavailable conditional UI the same as 'none'
            availableCallback(false);
            return;
        }
        availableCallback(true);
    } else {
        availableCallback(false);
    }

    // For modal mediations, skip if the user already dismissed the dialog in
    // this authentication session — avoids re-interrupting on every page load.
    const modalDismissedHash = getModalDismissedHash();
    if ((!modalDismissedHash || modalDismissedHash === sessionStorage.getItem(PASSKEY_MODAL_DISMISSED)) &&
            (mediation === 'optional' || mediation === 'required')) {
        return;
    }

    try {
        const result = await doAuthenticate({
            ...input,
            allowCredentials: [],
            additionalOptions: { mediation },
        });
        if (result) returnSuccess(result);
    } catch (err) {
        // If the user explicitly dismissed the modal, remember it so it is not
        // shown again during the same authentication session.
        if ((mediation === 'optional' || mediation === 'required') &&
                (err?.name === 'NotAllowedError' || err?.name === 'AbortError')) {
            sessionStorage.setItem(PASSKEY_MODAL_DISMISSED, modalDismissedHash);
        }
    }
}
