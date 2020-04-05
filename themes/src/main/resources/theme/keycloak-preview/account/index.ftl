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

            var features = {
                isRegistrationEmailAsUsername : ${realm.registrationEmailAsUsername?c},
                isEditUserNameAllowed : ${realm.editUsernameAllowed?c},
                isInternationalizationEnabled : false,
                isLinkedAccountsEnabled : ${realm.identityFederationEnabled?c},
                isEventsEnabled : ${isEventsEnabled?c},
                isMyResourcesEnabled : ${(realm.userManagedAccessAllowed && isAuthorizationEnabled)?c},
                isTotpConfigured : ${isTotpConfigured?c}
            }

            var availableLocales = [];
            <#list supportedLocales as locale, label>
                availableLocales.push({locale : '${locale}', label : '${label}'});
            </#list>

            <#if referrer??>
                var referrer = '${referrer}';
                var referrerName = '${referrerName}';
                var referrerUri = '${referrer_uri}';
            </#if>

            <#if msg??>
                var locale = '${locale}';
                var l18nMsg = JSON.parse('${msgJSON?no_esc}');
            <#else>
                var locale = 'en';
                var l18Msg = {};
            </#if>
        </script>

        <link rel="icon" href="${resourceUrl}/public/favicon.ico" type="image/x-icon"/>

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

        <#if properties.styles?has_content>
            <#list properties.styles?split(' ') as style>
            <link href="${resourceUrl}/${style}" rel="stylesheet"/>
            </#list>
        </#if>

        <link href="${resourceUrl}/public/layout.css" rel="stylesheet"/>
    </head>

    <body>

        <script>
            var keycloak = Keycloak({
                authServerUrl: authUrl,
                realm: realm,
                clientId: 'account-console'
            });
            keycloak.init({onLoad: 'check-sso', pkceMethod: 'S256'}).success(function(authenticated) {
                isReactLoading = true;
                toggleReact();
                if (!keycloak.authenticated) {
                    document.getElementById("landingSignInButton").style.display='inline';
                    document.getElementById("landingSignInLink").style.display='inline';
                } else {
                    document.getElementById("landingSignOutButton").style.display='inline';
                    document.getElementById("landingSignOutLink").style.display='inline';
                }

                loadjs("/node_modules/systemjs/dist/system.src.js", function() {
                    loadjs("/systemjs.config.js", function() {
                        System.import('${resourceUrl}/Main.js').catch(function (err) {
                            console.error(err);
                        });
                    });
                });
            }).error(function() {
                alert('failed to initialize keycloak');
            });
        </script>

<div id="main_react_container" style="display:none;height:100%"></div>

<div id="spinner_screen" style="display:block; height:100%">
    <div style="width: 320px; height: 328px; text-align: center; position: absolute; top:0;	bottom: 0; left: 0;	right: 0; margin: auto;">
                <img src="${resourceUrl}/public/logo.svg" alt="Logo" class="brand">
                <p>${msg("loadingMessage")}</p>
                <div >
                    <svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" style="margin: auto; background: rgb(255, 255, 255); display: block; shape-rendering: auto;" width="200px" height="200px" viewBox="0 0 100 100" preserveAspectRatio="xMidYMid">
                    <path d="M10 50A40 40 0 0 0 90 50A40 42 0 0 1 10 50" fill="#5DBCD2" stroke="none" transform="rotate(16.3145 50 51)">
                        <animateTransform attributeName="transform" type="rotate" dur="1s" repeatCount="indefinite" keyTimes="0;1" values="0 50 51;360 50 51"></animateTransform>
                    </path>
                </div>
            </div>
        </div>
    </div>
</div>

<div id="welcomeScreen" style="display:none;height:100%">
    <div class="pf-c-page" id="page-layout-default-nav">
      <header role="banner" class="pf-c-page__header">
        <div class="pf-c-page__header-brand">
          <a class="pf-c-page__header-brand-link">
            <img class="pf-c-brand brand" src="${resourceUrl}/public/logo.svg" alt="Logo">
          </a>
        </div>
        <div class="pf-c-page__header-tools">
            <#if referrer?has_content && referrer_uri?has_content>
            <div class="pf-c-page__header-tools-group pf-m-icons">
              <a id="landingReferrerLink" href="${referrer_uri}" id="referrer" tabindex="0"><span class="pf-icon pf-icon-arrow"></span>${msg("backTo",referrerName)}</a>
            </div>
            </#if>

            <div class="pf-c-page__header-tools-group pf-m-icons">
              <button id="landingSignInButton" tabindex="0" style="display:none" onclick="keycloak.login();" class="pf-c-button pf-m-primary" type="button">${msg("doLogIn")}</button>
              <button id="landingSignOutButton" tabindex="0" style="display:none" onclick="keycloak.logout();" class="pf-c-button pf-m-primary" type="button">${msg("doSignOut")}</button>
            </div>

            <!-- Kebab for mobile -->
            <div class="pf-c-page__header-tools-group">
                <div id="landingMobileKebab" class="pf-c-dropdown pf-m-mobile" onclick="toggleMobileDropdown();"> <!-- pf-m-expanded -->
                    <button aria-label="Actions" tabindex="0" id="landingMobileKebabButton" class="pf-c-dropdown__toggle pf-m-plain" type="button" aria-expanded="true" aria-haspopup="true">
                        <i class="fas fa-ellipsis-v" aria-hidden="false"></i>
                    </button>
                    <ul id="landingMobileDropdown" aria-labelledby="landingMobileKebabButton" class="pf-c-dropdown__menu pf-m-align-right" role="menu" style="display:none">
                        <#if referrer?has_content && referrer_uri?has_content>
                        <li role="none">
                            <a id="landingMobileReferrerLink" href="${referrer_uri}" role="menuitem" tabindex="0" aria-disabled="false" class="pf-c-dropdown__menu-item">${msg("backTo",referrerName)}</a>
                        </li>
                        </#if>

                        <li id="landingSignInLink" role="none" style="display:none">
                            <a href="#" onclick="keycloak.login();" role="menuitem" tabindex="0" aria-disabled="false" class="pf-c-dropdown__menu-item">${msg("doLogIn")}</a>
                        </li>
                        <li id="landingSignOutLink" role="none" style="display:none">
                            <a href="#" onclick="keycloak.logout();" role="menuitem" tabindex="0" aria-disabled="false" class="pf-c-dropdown__menu-item">${msg("doSignOut")}</a>
                        </li>
                    </ul>
                </div>
            </div>

        </div> <!-- end header tools -->
      </header>

      <main role="main" class="pf-c-page__main">
        <section class="pf-c-page__main-section pf-m-light">
          <div class="pf-c-content" id="landingWelcomeMessage">
            <h1>${msg("accountManagementWelcomeMessage")}</h1>
          </div>
        </section>
        <section class="pf-c-page__main-section">
          <div class="pf-l-gallery pf-m-gutter">
            <div class="pf-l-gallery__item">
              <div class="pf-c-card">
                <div class="pf-c-card__header pf-c-content">
                    <h2><i class="pf-icon pf-icon-user"></i>&nbsp${msg("personalInfoHtmlTitle")}</h2>
                    <h6>${msg("personalInfoIntroMessage")}</h6>
                </div>
                <div class="pf-c-card__body pf-c-content">
                    <h5 id="landingPersonalInfoLink" onclick="toggleReact()"><a href="#/app/personal-info">${msg("personalInfoHtmlTitle")}</a></h5>
                </div>
              </div>
            </div>
            <div class="pf-l-gallery__item">
              <div class="pf-c-card">
                <div class="pf-c-card__header pf-c-content">
                    <h2><i class="pf-icon pf-icon-security"></i>&nbsp${msg("accountSecurityTitle")}</h2>
                    <h6>${msg("accountSecurityIntroMessage")}</h6>
                </div>
                <div class="pf-c-card__body pf-c-content">
                    <h5 id="landingSigningInLink" onclick="toggleReact()"><a href="#/app/security/signingin">${msg("signingIn")}</a></h5>
                    <h5 id="landingDeviceActivityLink" onclick="toggleReact()"><a href="#/app/security/device-activity">${msg("deviceActivityHtmlTitle")}</a></h5>
                    <h5 id="landingLinkedAccountsLink" style="display:none" onclick="toggleReact()"><a href="#/app/security/linked-accounts">${msg("linkedAccountsHtmlTitle")}</a></h5>
                </div>
              </div>
            </div>
            <div class="pf-l-gallery__item">
              <div class="pf-c-card">
                <div class="pf-c-card__header pf-c-content">
                    <h2><i class="pf-icon pf-icon-applications"></i>&nbsp${msg("applicationsHtmlTitle")}</h2>
                    <h6>${msg("applicationsIntroMessage")}</h6>
                </div>
                <div class="pf-c-card__body pf-c-content">
                    <h5 id="landingApplicationsLink" onclick="toggleReact()"><a href="#/app/applications">${msg("applicationsHtmlTitle")}</a></h5>
                </div>
              </div>
            </div>
            <div class="pf-l-gallery__item" style="display:none" id="landingMyResourcesCard">
              <div class="pf-c-card">
                <div class="pf-c-card__header pf-c-content">
                    <h2><i class="pf-icon pf-icon-repository"></i>&nbsp${msg("myResources")}</h2>
                    <h6>${msg("resourceIntroMessage")}</h6>
                </div>
                <div class="pf-c-card__body pf-c-content">
                    <h5 id="landingMyResourcesLink" onclick="toggleReact()"><a href="#/app/resources">${msg("myResources")}</a></h5>
                </div>
              </div>
            </div>
          </div>
        </section>
      </main>
    </div>
</div>

        <script>
            if (features.isLinkedAccountsEnabled) {
                document.getElementById("landingLinkedAccountsLink").style.display='block';
            };

            // Hidden until feature is complete.
            //if (features.isMyResourcesEnabled) {
            //    document.getElementById("landingMyResourcesCard").style.display='block';
            //};
        </script>

    </body>
</html>
