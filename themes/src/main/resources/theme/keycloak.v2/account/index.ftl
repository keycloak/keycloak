<!DOCTYPE html>
<html>
    <head>
        <title>${msg("accountManagementTitle")}</title>

        <meta charset="UTF-8">
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="robots" content="noindex, nofollow">
        <meta name="viewport" content="width=device-width, initial-scale=1">

        <script>
            <#if properties.developmentMode?has_content && properties.developmentMode == "true">
            var developmentMode = true;
            var reactRuntime = 'react.development.js';
            var reactDOMRuntime = 'react-dom.development.js';
            var reactRouterRuntime = 'react-router-dom.js';
            <#else>
            var developmentMode = false;
            var reactRuntime = 'react.production.min.js';
            var reactDOMRuntime = 'react-dom.production.min.js';
            var reactRouterRuntime = 'react-router-dom.min.js';
            </#if>
            var authUrl = '${authUrl}';
            var baseUrl = '${baseUrl}';
            var realm = '${realm.name}';
            var resourceUrl = '${resourceUrl}';
            var isReactLoading = false;

            <#if properties.logo?has_content>
            var brandImg = resourceUrl + '${properties.logo}';
            <#else>
            var brandImg = resourceUrl + '/public/logo.svg';
            </#if>

            <#if properties.logoUrl?has_content>
            var brandUrl = '${properties.logoUrl}';
            <#else>
            var brandUrl = baseUrl;
            </#if>

            var features = {
                isRegistrationEmailAsUsername : ${realm.registrationEmailAsUsername?c},
                isEditUserNameAllowed : ${realm.editUsernameAllowed?c},
                isInternationalizationEnabled : ${realm.isInternationalizationEnabled()?c},
                isLinkedAccountsEnabled : ${realm.identityFederationEnabled?c},
                isEventsEnabled : ${isEventsEnabled?c},
                isMyResourcesEnabled : ${(realm.userManagedAccessAllowed && isAuthorizationEnabled)?c},
                isTotpConfigured : ${isTotpConfigured?c},
                deleteAccountAllowed : ${deleteAccountAllowed?c},
                updateEmailFeatureEnabled: ${updateEmailFeatureEnabled?c},
                updateEmailActionEnabled: ${updateEmailActionEnabled?c}
            }

            var availableLocales = [];
            <#list supportedLocales as locale, label>
                availableLocales.push({locale : '${locale}', label : '${label}'});
            </#list>

            <#if referrer??>
                var referrer = '${referrer}';
                var referrerName = '${referrerName}';
                var referrerUri = '${referrer_uri}'.replace('&amp;', '&');
            </#if>

            <#if msg??>
                var locale = '${locale}';
                <#outputformat "JavaScript">
                var l18nMsg = JSON.parse('${msgJSON?js_string}');
                </#outputformat>
            <#else>
                var locale = 'en';
                var l18Msg = {};
            </#if>
        </script>

        <#if properties.favIcon?has_content>
        <link rel="icon" href="${resourceUrl}${properties.favIcon}" type="image/x-icon"/>
        <#else>
        <link rel="icon" href="${resourceUrl}/public/favicon.ico" type="image/x-icon"/>
        </#if>

        <script src="${authUrl}js/keycloak.js"></script>

        <#if properties.developmentMode?has_content && properties.developmentMode == "true">
        <!-- Don't use this in production: -->
        <script src="${resourceUrl}/node_modules/react/umd/react.development.js" crossorigin></script>
        <script src="${resourceUrl}/node_modules/react-dom/umd/react-dom.development.js" crossorigin></script>
        <script src="https://unpkg.com/babel-standalone@6.26.0/babel.min.js"></script>
        </#if>

        <#if properties.extensions?has_content>
            <#list properties.extensions?split(' ') as script>
                <#if properties.developmentMode?has_content && properties.developmentMode == "true">
        <script type="text/babel" src="${resourceUrl}/${script}"></script>
                <#else>
        <script type="text/javascript" src="${resourceUrl}/${script}"></script>
                </#if>
            </#list>
        </#if>

        <#if properties.scripts?has_content>
            <#list properties.scripts?split(' ') as script>
        <script type="text/javascript" src="${resourceUrl}/${script}"></script>
            </#list>
        </#if>

        <script>
            var content = <#include "resources/content.json"/>
        </script>

        <#if properties.styles?has_content>
            <#list properties.styles?split(' ') as style>
            <link href="${resourceUrl}/${style}" rel="stylesheet"/>
            </#list>
        </#if>

        
        <link rel="stylesheet" type="text/css" href="${resourceCommonUrl}/web_modules/@patternfly/react-core/dist/styles/base.css"/>
        <link rel="stylesheet" type="text/css" href="${resourceCommonUrl}/web_modules/@patternfly/react-core/dist/styles/app.css"/>
        <link rel="stylesheet" type="text/css" href="${resourceCommonUrl}/web_modules/@patternfly/patternfly/patternfly-addons.css"/>
        <link href="${resourceUrl}/public/layout.css" rel="stylesheet"/>
    </head>

    <body>

        <script>
            var keycloak = Keycloak({
                authServerUrl: authUrl,
                realm: realm,
                clientId: 'account-console'
            });
            keycloak.init({onLoad: 'check-sso', pkceMethod: 'S256', promiseType: 'native'}).then((authenticated) => {
                isReactLoading = true;
                toggleReact();
                if (!keycloak.authenticated) {
                    document.getElementById("landingSignInButton").style.display='inline';
                    document.getElementById("landingSignInLink").style.display='inline';
                } else {
                    document.getElementById("landingSignOutButton").style.display='inline';
                    document.getElementById("landingSignOutLink").style.display='inline';
                    document.getElementById("landingLoggedInUser").innerHTML = loggedInUserName('${msg("unknownUser")}', '${msg("fullName")}');
                }

                loadjs("/Main.js");
            }).catch(() => {
                alert('failed to initialize keycloak');
            });
        </script>

<div id="main_react_container" style="display:none;height:100%"></div>

<div id="spinner_screen" style="display:block; height:100%">
    <div style="width: 320px; height: 328px; text-align: center; position: absolute; top:0;	bottom: 0; left: 0;	right: 0; margin: auto;">
        <#if properties.logo?has_content>
        <img src="${resourceUrl}${properties.logo}" alt="Logo" class="brand">
        <#else>
        <img src="${resourceUrl}/public/logo.svg" alt="Logo" class="brand">
        </#if>
        <p>${msg("loadingMessage")}</p>
        <div>
            <svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" style="margin: auto; background: rgb(255, 255, 255); display: block; shape-rendering: auto;" width="200px" height="200px" viewBox="0 0 100 100" preserveAspectRatio="xMidYMid">
                <path d="M10 50A40 40 0 0 0 90 50A40 42 0 0 1 10 50" fill="#5DBCD2" stroke="none" transform="rotate(16.3145 50 51)">
                    <animateTransform attributeName="transform" type="rotate" dur="1s" repeatCount="indefinite" keyTimes="0;1" values="0 50 51;360 50 51"></animateTransform>
                </path>
            </svg>
        </div>
    </div>
</div>

<div id="welcomeScreen" style="display:none;height:100%">
    <div class="pf-c-page" id="page-layout-default-nav">
      <header role="banner" class="pf-c-page__header">
        <div class="pf-c-page__header-brand">
          <#if properties.logoUrl?has_content>
          <a id="landingLogo" class="pf-c-page__header-brand-link" href="${properties.logoUrl}">
          <#else>
          <a id="landingLogo" class="pf-c-page__header-brand-link" href="${baseUrl}">
          </#if>
            <#if properties.logo?has_content>
            <img class="pf-c-brand brand" src="${resourceUrl}${properties.logo}" alt="Logo">
            <#else>
            <img class="pf-c-brand brand" src="${resourceUrl}/public/logo.svg" alt="Logo">
            </#if>
          </a>
        </div>
        <div class="pf-c-page__header-tools">
            <#if referrer?has_content && referrer_uri?has_content>
            <div class="pf-c-page__header-tools-group pf-m-icons pf-u-display-none pf-u-display-flex-on-md">
              <a id="landingReferrerLink" href="${referrer_uri}" class="pf-c-button pf-m-link" tabindex="0">
                  <span class="pf-c-button__icon pf-m-start">
                      <i class="pf-icon pf-icon-arrow" aria-hidden="true"></i>
                  </span>
                  ${msg("backTo",referrerName)}
              </a>
            </div>
            </#if>

            <div class="pf-c-page__header-tools-group pf-m-icons pf-u-display-none pf-u-display-flex-on-md pf-u-mr-md">
              <button id="landingSignInButton" tabindex="0" style="display:none" onclick="keycloak.login();" class="pf-c-button pf-m-primary" type="button">${msg("doSignIn")}</button>
              <button id="landingSignOutButton" tabindex="0" style="display:none" onclick="keycloak.logout();" class="pf-c-button pf-m-primary" type="button">${msg("doSignOut")}</button>
            </div>

            <!-- Kebab for mobile -->
            <div class="pf-c-page__header-tools-group pf-u-display-none-on-md">
                <div id="landingMobileKebab" class="pf-c-dropdown pf-m-mobile" onclick="toggleMobileDropdown();"> <!-- pf-m-expanded -->
                    <button aria-label="Actions" tabindex="0" id="landingMobileKebabButton" class="pf-c-dropdown__toggle pf-m-plain" type="button" aria-expanded="true" aria-haspopup="true">
                        <svg fill="currentColor" height="1em" width="1em" viewBox="0 0 192 512" aria-hidden="true" role="img" style="vertical-align: -0.125em;"><path d="M96 184c39.8 0 72 32.2 72 72s-32.2 72-72 72-72-32.2-72-72 32.2-72 72-72zM24 80c0 39.8 32.2 72 72 72s72-32.2 72-72S135.8 8 96 8 24 40.2 24 80zm0 352c0 39.8 32.2 72 72 72s72-32.2 72-72-32.2-72-72-72-72 32.2-72 72z" transform=""></path></svg>
                    </button>
                    <ul id="landingMobileDropdown" aria-labelledby="landingMobileKebabButton" class="pf-c-dropdown__menu pf-m-align-right" role="menu" style="display:none">
                        <#if referrer?has_content && referrer_uri?has_content>
                        <li role="none">
                            <a id="landingMobileReferrerLink" href="${referrer_uri}" role="menuitem" tabindex="0" aria-disabled="false" class="pf-c-dropdown__menu-item">${msg("backTo",referrerName)}</a>
                        </li>
                        </#if>

                        <li id="landingSignInLink" role="none" style="display:none">
                            <a onclick="keycloak.login();" role="menuitem" tabindex="0" aria-disabled="false" class="pf-c-dropdown__menu-item">${msg("doLogIn")}</a>
                        </li>
                        <li id="landingSignOutLink" role="none" style="display:none">
                            <a onclick="keycloak.logout();" role="menuitem" tabindex="0" aria-disabled="false" class="pf-c-dropdown__menu-item">${msg("doSignOut")}</a>
                        </li>
                    </ul>
                </div>
            </div>

            <span id="landingLoggedInUser"></span>

        </div> <!-- end header tools -->
      </header>

      <main role="main" class="pf-c-page__main">
        <section class="pf-c-page__main-section pf-m-limit-width pf-m-light pf-m-shadow-bottom">
            <div class="pf-c-page__main-body">
                <div class="pf-c-content" id="landingWelcomeMessage">
                    <h1>${msg("accountManagementWelcomeMessage")}</h1>
                </div>
            </div>
        </section>
        <section class="pf-c-page__main-section pf-m-limit-width pf-m-overflow-scroll">
            <div class="pf-c-page__main-body">
                <div class="pf-l-gallery pf-m-gutter">
                    <#assign content=theme.apply("content.json")?eval>
                    <#list content as item>
                        <div class="pf-l-gallery__item" id="landing-${item.id}">
                            <div class="pf-c-card pf-m-full-height">
                                <div>
                                    <div class="pf-c-card__title pf-c-content">
                                        <h2 class="pf-u-display-flex pf-u-w-100 pf-u-flex-direction-column">
                                            <#if item.icon??>
                                                <i class="pf-icon ${item.icon}"></i>
                                            <#elseif item.iconSvg??>
                                                <img src="${item.iconSvg}" alt="icon"/>
                                            </#if>
                                            ${msg(item.label)}
                                        </h2>
                                    </div>
                                    <div class="pf-c-card__body">
                                        <#if item.descriptionLabel??>
                                            <p class="pf-u-mb-md">${msg(item.descriptionLabel)}</p>
                                        </#if>
                                        <#if item.content??>
                                            <#list item.content as sub>
                                                <div id="landing-${sub.id}">
                                                    <a onclick="toggleReact(); window.location.hash='${sub.path}'">${msg(sub.label)}</a>
                                                </div>
                                            </#list>
                                        <#else>
                                            <a id="landing-${item.id}" onclick="toggleReact(); window.location.hash = '${item.path}'">${msg(item.label)}</a>
                                        </#if>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </#list>
                </div>
            </div>
        </section>
      </main>
    </div>
</div>

    <script>
      const removeHidden = (content) => {
        content.forEach(c => {
          if (c.hidden && eval(c.hidden)) {
            document.getElementById('landing-' + c.id).remove();
          }
          if (c.content) removeHidden(c.content);
        });
      }
      removeHidden(content);
    </script>

    </body>
</html>
