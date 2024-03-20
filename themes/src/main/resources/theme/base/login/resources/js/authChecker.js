const CHECK_INTERVAL_MILLISECS = 2000;
const initialSession = getSession();

export function checkCookiesAndSetTimer(authSessionId, tabId, loginRestartUrl) {
  if (initialSession) {
    // We started with a session, so there is nothing to do, exit.
    return;
  }

  const session = getSession();

  if (!session) {
    // The session is not present, check again later.
    setTimeout(
      () => checkCookiesAndSetTimer(authSessionId, tabId, loginRestartUrl),
      CHECK_INTERVAL_MILLISECS
    );
  } else {
    // The session is present, check the auth state.
    checkAuthState(authSessionId, tabId, loginRestartUrl);
  }
}

function checkAuthState(authSessionId, tabId, loginRestartUrl) {
  const authStateRaw = getAuthState();

  if (!authStateRaw) {
    // The auth state is not present, exit.
    return;
  }

  // Attempt to parse the auth state as JSON.
  let authState;
  try {
    authState = JSON.parse(decodeURIComponent(authStateRaw));
  } catch (error) {
    // The auth state is not valid JSON, exit.
    return;
  }

  if (authState.authSessionId !== authSessionId) {
    // The session ID does not match, exit.
    return;
  }

  if (
    !Array.isArray(authState.remainingTabs) ||
    !authState.remainingTabs.includes(tabId)
  ) {
    // The remaining tabs don't include the provided tab ID, exit.
    return;
  }

  // We made it this far, redirect to the login restart URL.
  location.href = loginRestartUrl;
}

function getSession() {
  return getCookieByName("KEYCLOAK_SESSION");
}

function getAuthState() {
  return getCookieByName("KC_AUTH_STATE");
}

function getCookieByName(name) {
  for (const cookie of document.cookie.split(";")) {
    const [key, value] = cookie.split("=").map((value) => value.trim());
    if (key === name) {
      return value.startsWith('"') && value.endsWith('"') ? value.slice(1, -1) : value;
    }
  }
  return null;
}
