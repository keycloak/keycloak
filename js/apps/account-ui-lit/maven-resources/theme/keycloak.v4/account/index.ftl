<!doctype html>
<html lang="${locale}" dir="${localeDir}">
  <head>
    <meta charset="utf-8">
    <link rel="icon" type="${properties.favIconType!'image/svg+xml'}" href="${resourceUrl}${properties.favIcon!'/favicon.svg'}">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="color-scheme" content="light${darkMode?then(' dark', '')}">
    <meta name="description" content="${properties.description!'The Account Console is a web-based interface for managing your account.'}">
    <title>${properties.title!'Account Management'}</title>
    <style>
      html, body {
        margin: 0;
        height: 100%;
      }

      #app {
        height: 100%;
      }

      .container {
        padding: 0;
        margin: 0;
        width: 100%;
      }

      .keycloak__loading-container {
        height: 100vh;
        width: 100%;
        color: #151515;
        background-color: #fff;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-direction: column;
        margin: 0;
      }

      @media (prefers-color-scheme: dark) {
        .keycloak__loading-container {
          color: #e0e0e0;
          background-color: #1b1d21;
        }
      }

      #loading-text {
        z-index: 1000;
        font-size: 20px;
        font-weight: 600;
        padding-top: 32px;
      }

      .pf-v6-c-spinner {
        --pf-v6-c-spinner--AnimationDuration: 1.5s;
        --pf-v6-c-spinner--diameter: 3.375rem;
        --pf-v6-c-spinner--Width: var(--pf-v6-c-spinner--diameter);
        --pf-v6-c-spinner--Height: var(--pf-v6-c-spinner--diameter);
        width: var(--pf-v6-c-spinner--Width);
        height: var(--pf-v6-c-spinner--Height);
        animation: pf-v6-c-spinner-animation var(--pf-v6-c-spinner--AnimationDuration) linear infinite;
      }

      .pf-v6-c-spinner.pf-m-xl {
        --pf-v6-c-spinner--diameter: 6rem;
      }

      .pf-v6-c-spinner__path {
        stroke: currentColor;
        stroke-width: 8;
        stroke-linecap: round;
        stroke-dasharray: 283;
        stroke-dashoffset: 280;
        animation: pf-v6-c-spinner-path var(--pf-v6-c-spinner--AnimationDuration) ease-in-out infinite;
      }

      @keyframes pf-v6-c-spinner-animation {
        to { transform: rotate(360deg); }
      }

      @keyframes pf-v6-c-spinner-path {
        0% { stroke-dashoffset: 280; }
        50% { stroke-dashoffset: 70; }
        100% { stroke-dashoffset: 280; }
      }
    </style>
    <script type="importmap">
      {
        "imports": {
          "lit": "${resourceCommonUrl}/vendor/lit/lit.js",
          "lit/": "${resourceCommonUrl}/vendor/lit/",
          "lit/decorators.js": "${resourceCommonUrl}/vendor/lit/decorators.js",
          "@lit/context": "${resourceCommonUrl}/vendor/lit-context/context.js",
          "keycloak-js": "${resourceCommonUrl}/vendor/keycloak-js/keycloak.js",
          "i18next": "${resourceCommonUrl}/vendor/i18next/i18next.js"
        }
      }
    </script>
    <link rel="stylesheet" href="${resourceCommonUrl}/vendor/patternfly-v6/patternfly.min.css">
    <#if darkMode>
      <script type="module" async blocking="render">
          const DARK_MODE_CLASS = "${properties.kcDarkModeClass}";
          const mediaQuery = window.matchMedia("(prefers-color-scheme: dark)");

          updateDarkMode(mediaQuery.matches);
          mediaQuery.addEventListener("change", (event) => updateDarkMode(event.matches));

          function updateDarkMode(isEnabled) {
            const { classList } = document.documentElement;

            if (isEnabled) {
              classList.add(DARK_MODE_CLASS);
            } else {
              classList.remove(DARK_MODE_CLASS);
            }
          }
      </script>
    </#if>
    <#if !isSecureContext>
      <script type="module" src="${resourceCommonUrl}/vendor/web-crypto-shim/web-crypto-shim.js"></script>
    </#if>
    <#if devServerUrl?has_content>
      <script type="module" src="${devServerUrl}/@vite/client"></script>
      <script type="module" src="${devServerUrl}/src/main.js"></script>
    <#else>
      <script type="module" src="${resourceUrl}/main.js"></script>
    </#if>
  </head>
  <body data-page-id="account">
    <div id="app">
      <kc-app>
        <main class="container">
          <div class="keycloak__loading-container">
            <svg class="pf-v6-c-spinner pf-m-xl" role="progressbar" aria-valuetext="Loading..." viewBox="0 0 100 100" aria-label="Contents">
              <circle class="pf-v6-c-spinner__path" cx="50" cy="50" r="45" fill="none"></circle>
            </svg>
            <div>
              <p id="loading-text">Loading the Account Console</p>
            </div>
          </div>
        </main>
      </kc-app>
    </div>
    <noscript>JavaScript is required to use the Account Console.</noscript>
    <script>
      window.__env__ = {
        "serverBaseUrl": "${serverBaseUrl}",
        "authUrl": "${authUrl}",
        "authServerUrl": "${authServerUrl}",
        "realm": "${realm.name}",
        "clientId": "${clientId}",
        "resourceUrl": "${resourceUrl}",
        "logo": "${properties.logo!""}",
        "logoUrl": "${properties.logoUrl!""}",
        "baseUrl": "${baseUrl}",
        "locale": "${locale}",
        "referrerName": "${referrerName!""}",
        "referrerUrl": "${referrer_uri!""}",
        "features": {
          "isRegistrationEmailAsUsername": ${realm.registrationEmailAsUsername?c},
          "isEditUserNameAllowed": ${realm.editUsernameAllowed?c},
          "isInternationalizationEnabled": ${realm.isInternationalizationEnabled()?c},
          "isLinkedAccountsEnabled": ${isLinkedAccountsEnabled?c},
          "isMyResourcesEnabled": ${(realm.userManagedAccessAllowed && isAuthorizationEnabled)?c},
          "isViewOrganizationsEnabled": ${isViewOrganizationsEnabled?c},
          "deleteAccountAllowed": ${deleteAccountAllowed?c},
          "updateEmailFeatureEnabled": ${updateEmailFeatureEnabled?c},
          "updateEmailActionEnabled": ${updateEmailActionEnabled?c},
          "isViewGroupsEnabled": ${isViewGroupsEnabled?c},
          "isOid4VciEnabled": ${isOid4VciEnabled?c}
        },
        "scope": "${scope!""}"
      };
    </script>
  </body>
</html>
