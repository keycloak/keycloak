import { doAuthenticate, returnSuccess } from "./webauthnAuthenticate.js";

/**
 * Entry point: replaces initAuthenticate + autoTriggerPasskey as a separate call.
 *
 * 1. Tries mediation "optional" → immediate native dialog without a click
 * 2. Falls back to mediation "conditional" on failure → autofill in the username field
 */
export async function initAuthenticate(input, availableCallback = () => {}) {
    // Check if WebAuthn is supported by this browser
    if (!window.PublicKeyCredential) {
        // Fail silently as WebAuthn Conditional UI is not required
        return;
    }
    if (input.isUserIdentified || typeof PublicKeyCredential.isConditionalMediationAvailable === "undefined") {
        availableCallback(false);
        return;
    }

    const isAvailable = await PublicKeyCredential.isConditionalMediationAvailable();
    if (!isAvailable) {
        availableCallback(false);
        return;
    }

    availableCallback(true);
    await tryOptionalThenConditional(input);
}

async function tryOptionalThenConditional(input) {
    // Step 1: Immediate modal dialog without user interaction
    try {
        const result = await doAuthenticate({
            ...input,
            allowCredentials: [],
            additionalOptions: { mediation: 'optional' },
        });
        if (result) {
            returnSuccess(result);
            return;
        }
    } catch (err) {
        if (err.name !== 'NotAllowedError' && err.name !== 'AbortError') {
            console.warn('WebAuthn optional trigger failed:', err);
        }
    }

    // Step 2: Fall back to Conditional UI (autofill in the username field)
    try {
        const result = await doAuthenticate({
            ...input,
            allowCredentials: [],
            additionalOptions: { mediation: 'conditional' },
        });
        if (result) returnSuccess(result);
    } catch {
        // Fail silently as WebAuthn Conditional UI is not required
    }
}
