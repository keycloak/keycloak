<!doctype html>
<html>
  <head>
    <meta charset="utf-8">
    <base href="${resourceUrl}/">
    <link rel="icon" type="${properties.favIconType!'image/svg+xml'}" href="${resourceUrl}${properties.favIcon!'/favicon.svg'}">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="description" content="${properties.description!'The Keycloak Administration Console is a web-based interface for managing Keycloak.'}">
    <title>${properties.title!'Keycloak Administration Console'}</title>
    <style>
      body {
        margin: 0;
      }

      body, #app {
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
        background-color: #f0f0f0;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-direction: column;
        margin: 0;
      }

      #loading-text {
        z-index: 1000;
        font-size: 20px;
        font-weight: 600;
        padding-top: 32px;
      }
    </style>
    <script type="importmap">
      {
        "imports": {
          "react": "${resourceCommonUrl}/vendor/react/react.production.min.js",
          "react/jsx-runtime": "${resourceCommonUrl}/vendor/react/react-jsx-runtime.production.min.js",
          "react-dom": "${resourceCommonUrl}/vendor/react-dom/react-dom.production.min.js"
        }
      }
    </script>
    <#if devServerUrl?has_content>
      <script type="module">
        import { injectIntoGlobalHook } from "${devServerUrl}/@react-refresh";

        injectIntoGlobalHook(window);
        window.$RefreshReg$ = () => {};
        window.$RefreshSig$ = () => (type) => type;
      </script>
      <script type="module">
        import { inject } from "${devServerUrl}/@vite-plugin-checker-runtime";

        inject({
          overlayConfig: {},
          base: "/",
        });
      </script>
      <script type="module" src="${devServerUrl}/@vite/client"></script>
      <script type="module" src="${devServerUrl}/src/main.tsx"></script>
    </#if>
    <#if properties.scripts?has_content>
      <#list properties.scripts?split(' ') as script>
        <script type="module" crossorigin src="${resourceUrl}/${script}"></script>
      </#list>
    </#if>
    <#if properties.styles?has_content>
      <#list properties.styles?split(' ') as style>
        <link rel="stylesheet" crossorigin href="${resourceUrl}/${style}">
      </#list>
    </#if>
  </head>
  <body>
    <div id="app">
      <main class="container">
        <div class="keycloak__loading-container">
          <span class="pf-c-spinner pf-m-xl" role="progressbar" aria-valuetext="Loading&hellip;">
            <span class="pf-c-spinner__clipper"></span>
            <span class="pf-c-spinner__lead-ball"></span>
            <span class="pf-c-spinner__tail-ball"></span>
          </span>
          <div>
            <p id="loading-text">Loading the Administration Console</p>
          </div>
        </div>
      </main>
    </div>
    <noscript>JavaScript is required to use the Administration Console.</noscript>
    <script id="environment" type="application/json">
      {
        "loginRealm": "${loginRealm!"master"}",
        "clientId": "${clientId}",
        "authServerUrl": "${authServerUrl}",
        "authUrl": "${authUrl}",
        "consoleBaseUrl": "${consoleBaseUrl}",
        "resourceUrl": "${resourceUrl}",
        "masterRealm": "${masterRealm}",
        "resourceVersion": "${resourceVersion}",
        "logo": "${properties.logo!""}",
        "logoUrl": "${properties.logoUrl!""}"
      }
    </script>
  </body>
</html>
