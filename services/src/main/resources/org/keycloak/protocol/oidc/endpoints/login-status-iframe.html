<!doctype html>
<html>
  <head>
    <meta charset="utf-8">
  </head>
  <body>
    <script type="module">
      window.addEventListener("message", onMessage);

      async function onMessage(event) {
        // Filter out any events that do not match the expected format of a 2-part string split by a space.
        if (typeof event.data !== "string") {
          return;
        }

        const data = event.data.split(" ");

        if (data.length !== 2) {
          return;
        }

        // Extract data from event and verify status of session.
        const [clientId, sessionState] = data;
        const sessionStatus = await checkState(clientId, event.origin, sessionState);

        // Signal session status to the page embedding this iframe.
        event.source.postMessage(sessionStatus, event.origin);
      }

      let init;

      async function checkState(clientId, origin, sessionState) {
        // Check if the browser has granted us access to 3rd-party storage (such as cookies).
        const hasAccess = await hasStorageAccess();

        // If we don't have access, signal an error.
        // As we cannot read cookies, we cannot verify the session state.
        if (!hasAccess) {
          return "error";
        }

        // If not initialized, verify this client is allowed access with a call to the server.
        if (!init) {
          const url = new URL(`${location.origin}${location.pathname}/init`);

          url.searchParams.set("client_id", clientId);
          url.searchParams.set("origin", origin);

          const response = await fetch(url);

          if (!response.ok) {
            return "error";
          }

          init = { clientId, origin };
        }

        const cookie = getSessionCookie();

        // Signal a change in state if there is no cookie, and the session state is not empty.
        if (!cookie) {
          return sessionState !== "" ? "changed" : "unchanged";
        }

        // If the client and origin from the event match the verified ones from the server, signal if the cookie has changed.
        if (clientId === init.clientId && origin === init.origin) {
          const [, , cookieSessionState] = cookie.split("/");
          return sessionState === cookieSessionState ? "unchanged" : "changed";
        }

        // Otherwise, if there is no match, then signal an error.
        return "error";
      }

      async function hasStorageAccess() {
        if (!("hasStorageAccess" in document)) {
          return true;
        }

        return document.hasStorageAccess();
      }

      function getSessionCookie() {
        const cookie = getCookieByName("KEYCLOAK_SESSION");

        if (cookie !== null) {
          return cookie;
        }

        return getCookieByName("KEYCLOAK_SESSION_LEGACY");
      }

      function getCookieByName(name) {
        const cookies = new Map();

        for (const cookie of document.cookie.split(";")) {
          const [key, value] = cookie.split("=").map((value) => value.trim());
          cookies.set(key, value);
        }

        return cookies.get(name) ?? null;
      }
    </script>
  </body>
</html>
