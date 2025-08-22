<!doctype html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="color-scheme" content="light${(properties.darkMode)?boolean?then(' dark', '')}">
    <title>Welcome to ${productName}</title>
    <link rel="shortcut icon" href="${resourcesCommonPath}/img/favicon.ico">
    <#if properties.darkMode?boolean>
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
    <#if properties.stylesCommon?has_content>
      <#list properties.stylesCommon?split(' ') as style>
        <link rel="stylesheet" href="${resourcesCommonPath}/${style}">
      </#list>
    </#if>
    <#if properties.styles?has_content>
      <#list properties.styles?split(' ') as style>
        <link rel="stylesheet" href="${resourcesPath}/${style}">
      </#list>
    </#if>
  </head>
  <body data-page-id="welcome">
    <div class="pf-v5-c-background-image" style="--pf-v5-c-background-image--BackgroundImage: url(${baseUrl}${resourcesPath}/background.svg)"></div>
    <div class="pf-v5-c-login">
      <div class="pf-v5-c-login__container">
        <header class="pf-v5-c-login__header">
          <div class="pf-v5-c-brand">
            <img src="${resourcesPath}/logo.svg" alt="${productName} Logo" class="kc-brand">
          </div>
        </header>
        <#if adminConsoleEnabled && (bootstrap || successMessage?has_content)>
          <main class="pf-v5-c-login__main">
            <header class="pf-v5-c-login__main-header">
              <#if localUser>
                <h1 class="pf-v5-c-title pf-m-2xl">Create an administrative user</h1>
                <#if !successMessage?has_content>
                  <p class="pf-v5-c-login__main-header-desc">To get started with ${productName}, you first create an administrative user.</p>
                </#if>
              <#else>
                <h1 class="pf-v5-c-title pf-m-3xl">Local access required</h1>
                <p class="pf-v5-c-login__main-header-desc">You will need local access to create the administrative user.</p>
              </#if>
            </header>
            <div class="pf-v5-c-login__main-body">
              <#if successMessage?has_content>
                <div class="pf-v5-c-alert pf-m-inline pf-m-success pf-v5-u-mb-xl">
                  <div class="pf-v5-c-alert__icon">
                    <svg class="pf-v5-svg" viewBox="0 0 512 512" fill="currentColor" aria-hidden="true" role="img" width="1em" height="1em">
                      <path d="M504 256c0 136.967-111.033 248-248 248S8 392.967 8 256 119.033 8 256 8s248 111.033 248 248zM227.314 387.314l184-184c6.248-6.248 6.248-16.379 0-22.627l-22.627-22.627c-6.248-6.249-16.379-6.249-22.628 0L216 308.118l-70.059-70.059c-6.248-6.248-16.379-6.248-22.628 0l-22.627 22.627c-6.248 6.248-6.248 16.379 0 22.627l104 104c6.249 6.249 16.379 6.249 22.628.001z"></path>
                    </svg>
                  </div>
                  <h4 class="pf-v5-c-alert__title">
                    <span class="pf-v5-screen-reader">Success alert:</span>${successMessage}
                  </h4>
                </div>
                <a class="pf-v5-c-button pf-m-primary pf-m-block" href="${adminUrl}">Open Administration Console</a>
              </#if>
              <#if bootstrap>
                <#if localUser>
                  <form class="pf-v5-c-form" method="post" novalidate>
                    <#if errorMessage?has_content>
                      <div class="pf-v5-c-form__alert">
                        <div class="pf-v5-c-alert pf-m-inline pf-m-danger">
                          <div class="pf-v5-c-alert__icon">
                            <svg class="pf-v5-svg" viewBox="0 0 512 512" fill="currentColor" aria-hidden="true" role="img" width="1em" height="1em">
                              <path d="M504 256c0 136.997-111.043 248-248 248S8 392.997 8 256C8 119.083 119.043 8 256 8s248 111.083 248 248zm-248 50c-25.405 0-46 20.595-46 46s20.595 46 46 46 46-20.595 46-46-20.595-46-46-46zm-43.673-165.346l7.418 136c.347 6.364 5.609 11.346 11.982 11.346h48.546c6.373 0 11.635-4.982 11.982-11.346l7.418-136c.375-6.874-5.098-12.654-11.982-12.654h-63.383c-6.884 0-12.356 5.78-11.981 12.654z"></path>
                            </svg>
                          </div>
                          <h4 class="pf-v5-c-alert__title">
                            <span class="pf-v5-screen-reader">Danger alert:</span>${errorMessage}
                          </h4>
                        </div>
                      </div>
                    </#if>
                    <div class="pf-v5-c-form__group">
                      <div class="pf-v5-c-form__group-label">
                        <label class="pf-v5-c-form__label" for="username">
                          <span class="pf-v5-c-form__label-text">Username</span>&nbsp;<span class="pf-v5-c-form__label-required" aria-hidden="true">&#42;</span>
                        </label>
                      </div>
                      <div class="pf-v5-c-form__group-control">
                        <span class="pf-v5-c-form-control pf-m-required">
                          <input id="username" type="text" name="username" autocomplete="username" required>
                        </span>
                      </div>
                    </div>
                    <div class="pf-v5-c-form__group">
                      <div class="pf-v5-c-form__group-label">
                        <label class="pf-v5-c-form__label" for="password">
                          <span class="pf-v5-c-form__label-text">Password</span>&nbsp;<span class="pf-v5-c-form__label-required" aria-hidden="true">&#42;</span>
                        </label>
                      </div>
                      <div class="pf-v5-c-form__group-control">
                        <span class="pf-v5-c-form-control pf-m-required">
                          <input id="password" type="password" name="password" autocomplete="new-password" required>
                        </span>
                      </div>
                    </div>
                    <div class="pf-v5-c-form__group">
                      <div class="pf-v5-c-form__group-label">
                        <label class="pf-v5-c-form__label" for="password-confirmation">
                          <span class="pf-v5-c-form__label-text">Password confirmation</span>&nbsp;<span class="pf-v5-c-form__label-required" aria-hidden="true">&#42;</span>
                        </label>
                      </div>
                      <div class="pf-v5-c-form__group-control">
                        <span class="pf-v5-c-form-control pf-m-required">
                          <input id="password-confirmation" type="password" name="passwordConfirmation" autocomplete="new-password" required>
                        </span>
                      </div>
                    </div>
                    <input name="stateChecker" type="hidden" value="${stateChecker}">
                    <div class="pf-v5-c-form__group pf-m-action">
                      <button class="pf-v5-c-button pf-m-primary pf-m-block" type="submit">Create user</button>
                    </div>
                  </form>
                <#else>
                  <p>To create the administrative user, access the Administration Console over localhost, or use a <code>bootstrap-admin</code> command.</p>
                </#if>
              </#if>
            </div>
          </main>
        </#if>
      </div>
    </div>
  </body>
</html>