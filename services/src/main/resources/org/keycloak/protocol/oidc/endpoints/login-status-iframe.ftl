<!doctype html>
<html>
  <head>
    <meta charset="utf-8">
  </head>
  <body>
    <#if !isSecureContext>
      <script type="module" src="${resourceCommonUrl}/vendor/web-crypto-shim/web-crypto-shim.js"></script>
    </#if>
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
      let preventAdditionalRequests = false;

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
          // Prevent additional requests to the server to avoid potential DoS attacks.
          if (preventAdditionalRequests) {
            return "error";
          } else {
            preventAdditionalRequests = true;
          }

          const url = new URL(location.origin + location.pathname + "/init");

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
          const hashedSessionId = await hashString(sessionState);
          if (hashedSessionId === cookie) return "unchanged";

          // Backwards compatibility with versions older than 26.1
          const [, , cookieSessionState] = cookie.split("/");
          return sessionState === cookieSessionState ? "unchanged" : "changed";
        }

        // Otherwise, if there is no match, then signal an error.
        return "error";
      }

      // See https://developer.mozilla.org/en-US/docs/Web/API/Storage_Access_API/Using#checking_and_requesting_storage_access
      async function hasStorageAccess() {
        // Check if the Storage Access API is supported, if not, pretend we have access.
        // This is for older browsers, where support can be determined using the test cookie.
        if (!("hasStorageAccess" in document)) {
          return true;
        }

        // Check if we already have been granted storage access, if so, signal access.
        if (await document.hasStorageAccess()) {
          return true;
        }

        try {
          // Attempt to request storage access without a user interaction.
          // This might fail, and if it does an exception will be thrown.
          await document.requestStorageAccess();

          // If no exceptions are thrown, then signal access.
          return true;
        } catch (error) {
          // If an exception is thrown, then signal no access.
          return false;
        }
      }

      function getSessionCookie() {
        const cookie = getCookieByName("KEYCLOAK_SESSION");
        return cookie;
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

      /**
       * @param {ArrayBuffer} bytes
       * @see https://developer.mozilla.org/en-US/docs/Glossary/Base64#the_unicode_problem
       */
      function bytesToBase64(bytes) {
        const binString = String.fromCodePoint(...bytes);
        return btoa(binString);
      }

      /**
        * @param {string} message
        * @see https://developer.mozilla.org/en-US/docs/Web/API/SubtleCrypto/digest#basic_example
        */
      async function sha256Digest(message) {
        const encoder = new TextEncoder();
        const data = encoder.encode(message);

        if (typeof crypto === "undefined" || typeof crypto.subtle === "undefined") {
          throw new Error("Web Crypto API is not available.");
        }

        return await crypto.subtle.digest("SHA-256", data);
      }

      async function hashString(str) {
        // hash codeVerifier, then encode as url-safe base64 without padding
        const hashBytes = new Uint8Array(await sha256Digest(str));
        const encodedHash = bytesToBase64(hashBytes)
            .replace(/\+/g, '-')
            .replace(/\//g, '_')
            .replace(/\=/g, '');

        return encodedHash;
      }

    </script>
  </body>
</html>
