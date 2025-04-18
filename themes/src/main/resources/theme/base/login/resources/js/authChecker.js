const SESSION_POLLING_INTERVAL = 2000;
const AUTH_SESSION_TIMEOUT_MILLISECS = 1000;
const initialSession = getSession();
const forms = Array.from(document.forms);
let timeout;

// Stop polling for a session when a form is submitted to prevent unexpected redirects.
// This is required as Safari does not support the 'beforeunload' event properly.
// See: https://bugs.webkit.org/show_bug.cgi?id=219102
forms.forEach((form) =>
  form.addEventListener("submit", () => stopSessionPolling()),
);

// Stop polling for a session when the page is unloaded to prevent unexpected redirects.
globalThis.addEventListener("beforeunload", () => stopSessionPolling());

/**
 * Get all elements specified by the selectors and add the disable onclick.
 * @param {string} selectors The selector to locate the elements to disable
 * @param {string} disableClass The class to assign to the element
 */
export function disableAfterClick(selectors, disableClass) {
  document.querySelectorAll(selectors).forEach((element) =>
    element.addEventListener('click', (event) => checkDisable(event, element, disableClass))
  );
}

/**
 * Disables the element after being called the first time adding
 * attribute 'aria-disabled' to true.
 * @param {Event} event The click event
 * @param {Element} element The element to check
 * @param {string} disableClass The class to assign to the element
 * @returns true if first call, false if already disabled
 */
function checkDisable(event, element, disableClass) {
  if (element.getAttribute('aria-disabled') === 'true') {
      event.preventDefault();
  } else {
      element.setAttribute('aria-disabled', 'true');
      if (disableClass && disableClass.trim().length > 0) {
        element.classList.add(...disableClass.trim().split(/\s+/));
      }
  }
}

/**
 * Starts polling to check if a new session was started in another context (e.g. a tab or window), and redirects to the specified URL if a session is detected.
 * @param {string} redirectUrl - The URL to redirect to if a new session is detected.
 */
export function startSessionPolling(redirectUrl) {
  if (initialSession) {
    // We started with a session, so there is nothing to do, exit.
    return;
  }

  const session = getSession();

  if (!session) {
    // No new session detected, check again later.
    timeout = setTimeout(
      () => startSessionPolling(redirectUrl),
      SESSION_POLLING_INTERVAL,
    );
  } else {
    // A new session was detected, redirect to the specified URL and stop polling.
    location.href = redirectUrl;
    stopSessionPolling();
  }
}

/**
 * Stops polling the session.
 */
function stopSessionPolling() {
  if (timeout) {
    clearTimeout(timeout);
    timeout = undefined;
  }
}

export function checkAuthSession(pageAuthSessionHash) {
  setTimeout(() => {
    const cookieAuthSessionHash = getKcAuthSessionHash();
    if (
      cookieAuthSessionHash &&
      cookieAuthSessionHash !== pageAuthSessionHash
    ) {
      location.reload();
    }
  }, AUTH_SESSION_TIMEOUT_MILLISECS);
}

function getKcAuthSessionHash() {
  return getCookieByName("KC_AUTH_SESSION_HASH");
}

function getSession() {
  return getCookieByName("KEYCLOAK_SESSION");
}

function getCookieByName(name) {
  for (const cookie of document.cookie.split(";")) {
    const [key, value] = cookie.split("=").map((value) => value.trim());
    if (key === name) {
      return value.startsWith('"') && value.endsWith('"')
        ? value.slice(1, -1)
        : value;
    }
  }
  return null;
}
