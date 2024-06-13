const CHECK_INTERVAL_MILLISECS = 2000;
const initialSession = getSession();

export function checkCookiesAndSetTimer(loginRestartUrl) {
  if (initialSession) {
    // We started with a session, so there is nothing to do, exit.
    return;
  }

  const session = getSession();

  if (!session) {
    // The session is not present, check again later.
    setTimeout(
      () => checkCookiesAndSetTimer(loginRestartUrl),
      CHECK_INTERVAL_MILLISECS
    );
  } else {
    // Redirect to the login restart URL. This can typically automatically login user due the SSO
    location.href = loginRestartUrl;
  }
}

function getSession() {
  return getCookieByName("KEYCLOAK_SESSION");
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
