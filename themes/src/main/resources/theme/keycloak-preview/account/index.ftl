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
                
            var features = {
                isRegistrationEmailAsUsername : ${realm.registrationEmailAsUsername?c},
                isEditUserNameAllowed : ${realm.editUsernameAllowed?c},
                isInternationalizationEnabled : ${realm.internationalizationEnabled?c},
                isLinkedAccountsEnabled : ${realm.identityFederationEnabled?c},
                isEventsEnabled : ${isEventsEnabled?c},
                isMyResourcesEnabled : ${(realm.userManagedAccessAllowed && isAuthorizationEnabled)?c}
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
        
        <link rel="icon" href="${resourceUrl}/app/assets/img/favicon.ico" type="image/x-icon"/>
        <link rel="stylesheet" href="${resourceUrl}/node_modules/@patternfly/patternfly/patternfly.min.css">

        <script src="${authUrl}/js/keycloak.js"></script>
        
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
    </head>

    <body>

        <script>
            var keycloak = Keycloak('${authUrl}/realms/${realm.name}/account/keycloak.json');
            keycloak.init({onLoad: 'check-sso'}).success(function(authenticated) {
                toggleReact();
                if (!keycloak.authenticated) {
                    document.getElementById("signInButton").style.display='inline';
                    document.getElementById("signInLink").style.display='inline';
                } else {
                    document.getElementById("signOutButton").style.display='inline';
                    document.getElementById("signOutLink").style.display='inline';
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

<div id="main_react_container"></div>

<div id="welcomeScreen" style="display:none">
    <#if properties.styles?has_content>
        <#list properties.styles?split(' ') as style>
        <link href="${resourceUrl}/${style}" rel="stylesheet"/>
        </#list>
    </#if>
    <style>
        .pf-c-background-image {
            --pf-c-background-image--BackgroundImage: url('${resourceUrl}/node_modules/@patternfly/patternfly/assets/images/pfbg_576.jpg');
            --pf-c-background-image--BackgroundImage-2x: url('${resourceUrl}/node_modules/@patternfly/patternfly/assets/images/pfbg_576@2x.jpg');
            --pf-c-background-image--BackgroundImage--sm: url('${resourceUrl}/node_modules/@patternfly/patternfly/assets/images/pfbg_768.jpg');
            --pf-c-background-image--BackgroundImage--sm-2x: url('${resourceUrl}/node_modules/@patternfly/patternfly/assets/images/pfbg_768@2x.jpg');
            --pf-c-background-image--BackgroundImage--lg: url('${resourceUrl}/node_modules/@patternfly/patternfly/assets/images/pfbg_1200.jpg');
            --pf-c-background-image--Filter: url('${resourceUrl}/node_modules/@patternfly/patternfly/assets/images/background-filter.svg#image_overlay');
        }
    </style>
        
<div class="pf-c-background-image">
  <svg xmlns="http://www.w3.org/2000/svg" class="pf-c-background-image__filter" width="0" height="0">
    <filter id="image_overlay">
      <feColorMatrix type="matrix" values="1 0 0 0 0
              1 0 0 0 0
              1 0 0 0 0
              0 0 0 1 0" />
      <feComponentTransfer color-interpolation-filters="sRGB" result="duotone">
        <feFuncR type="table" tableValues="0.086274509803922 0.43921568627451"></feFuncR>
        <feFuncG type="table" tableValues="0.086274509803922 0.43921568627451"></feFuncG>
        <feFuncB type="table" tableValues="0.086274509803922 0.43921568627451"></feFuncB>
        <feFuncA type="table" tableValues="0 1"></feFuncA>
      </feComponentTransfer>
    </filter>
  </svg>
</div>
    <div class="pf-c-page" id="page-layout-default-nav">
      <header role="banner" class="pf-c-page__header">
        <div class="pf-c-page__header-brand">
          <a class="pf-c-page__header-brand-link">
            <img class="pf-c-brand" src="${resourceUrl}/app/assets/img/keycloak-logo-min.png" alt="Keycloak Logo">
          </a>
        </div>
        <div class="pf-c-page__header-tools">
            <#if referrer?has_content && referrer_uri?has_content>
            <div class="pf-c-page__header-tools-group pf-m-icons pf-screen-reader">
              <a href="${referrer_uri}" id="referrer" tabindex="0"><span class="pf-icon pf-icon-arrow"></span>${msg("backTo",referrerName)}</a>
            </div>
            </#if>
            
            <#if realm.internationalizationEnabled  && supportedLocales?size gt 1>
            <div class="pf-c-page__header-tools-group pf-screen-reader pf-m-icons">
              <div id="landing-locale-dropdown" class="pf-c-dropdown">
                <button onclick="toggleLocaleDropdown();" class="pf-c-dropdown__toggle pf-m-plain" id="landing-locale-dropdown-button" aria-expanded="false" aria-haspopup="true">
                    <span class="pf-c-dropdown__toggle-text">
                          ${msg("locale_" + locale)}
                    </span>
                    <i class="fas fa-caret-down pf-c-dropdown__toggle-icon" aria-hidden="true"></i>
                </button>
                <ul id="landing-locale-dropdown-list" class="pf-c-dropdown__menu" aria-labeledby="pf-toggle-id-20" role="menu" hidden>
                    <#list supportedLocales as locale, label>
                        <#if referrer?has_content && referrer_uri?has_content>
                        <li role="none"><a href="${baseUrl}/?kc_locale=${locale}&referrer=${referrer}&referrer_uri=${referrer_uri}" role="menuitem" tabindex="0" aria-disabled="false" class="pf-c-dropdown__menu-item">${label}</a></li>
                        <#else>
                        <li role="none"><a href="${baseUrl}/?kc_locale=${locale}" role="menuitem" tabindex="0" aria-disabled="false" class="pf-c-dropdown__menu-item">${label}</a></li>
                        </#if>
                    </#list>
                </ul>
              </div>
            </div>
            </#if>
            
            <div class="pf-c-page__header-tools-group pf-m-icons pf-screen-reader">
              <button id="signInButton" tabindex="0" style="display:none" onclick="keycloak.login();" class="pf-c-button pf-m-primary" type="button">${msg("doLogIn")}</button>
              <button id="signOutButton" tabindex="0" style="display:none" onclick="keycloak.logout();" class="pf-c-button pf-m-primary" type="button">${msg("doSignOut")}</button>
            </div>
            
            <!-- Kebab for mobile -->
            <div class="pf-c-page__header-tools-group">
                <div id="mobileKebab" class="pf-c-dropdown pf-m-mobile" onclick="toggleMobileDropdown();"> <!-- pf-m-expanded -->
                    <button aria-label="Actions" tabindex="0" id="mobileKebabButton" class="pf-c-dropdown__toggle pf-m-plain" type="button" aria-expanded="true" aria-haspopup="true">
                        <i class="fas fa-ellipsis-v" aria-hidden="false"></i>
                    </button>
                    <ul id="mobileDropdown" aria-labelledby="pf-toggle-id-2" class="pf-c-dropdown__menu pf-m-align-right" role="menu" style="display:none">
                        <#if referrer?has_content && referrer_uri?has_content>
                        <li role="none">
                            <a href="${referrer_uri}" role="menuitem" tabindex="0" aria-disabled="false" class="pf-c-dropdown__menu-item">${msg("backTo",referrerName)}</a>
                        </li>
                        </#if>
                        
                        <!-- locale selector for mobile -->
                        <#if realm.internationalizationEnabled  && supportedLocales?size gt 1>
                            <li role="none" aria-expanded="false" onclick="toggleMobileChooseLocale();"><a href="#" class="pf-c-dropdown__menu-item">${msg("locale_" + locale)} <i id="mobileLocaleSelectedIcon" class="fas fa-angle-right pf-c-options-menu__menu-item-icon" aria-hidden="true"></i></a></li>
                            <#list supportedLocales as locale, label>
                                <#if referrer?has_content && referrer_uri?has_content>
                                <li role="none" id="mobile-locale-${locale}" style="display:none"><a href="${baseUrl}/?kc_locale=${locale}&referrer=${referrer}&referrer_uri=${referrer_uri}" role="menuitem" tabindex="0" aria-disabled="false" class="pf-c-dropdown__menu-item">${label}</a></li>
                                <#else>
                                <li role="none" id="mobile-locale-${locale}" style="display:none"><a href="${baseUrl}/?kc_locale=${locale}" role="menuitem" tabindex="0" aria-disabled="false" class="pf-c-dropdown__menu-item">${label}</a></li>
                                </#if>
                            </#list>
                            <li id="mobileLocaleSeperator" class="pf-c-dropdown__separator" role="separator" style="display:none"></li>
                        </#if>
                        <!-- end locale selector for mobile -->

                        <li id="signInLink" role="none" style="display:none">
                            <a href="#" onclick="keycloak.login();" role="menuitem" tabindex="0" aria-disabled="false" class="pf-c-dropdown__menu-item">${msg("doLogIn")}</a>
                        </li>
                        <li id="signOutLink" role="none" style="display:none">
                            <a href="#" onclick="keycloak.logout();" role="menuitem" tabindex="0" aria-disabled="false" class="pf-c-dropdown__menu-item">${msg("doSignOut")}</a>
                        </li>
                    </ul>
                </div>
            </div>
            
        </div> <!-- end header tools -->
      </header>

      <main role="main" class="pf-c-page__main">
        <section class="pf-c-page__main-section pf-m-light">
          <div class="pf-c-content">
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
                    <h5 id="personalInfoLink"><a href="#/app/account">${msg("personalInfoHtmlTitle")}</a></h5>
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
                    <h5 id="changePasswordLink"><a href="#/app/password">${msg("changePasswordHtmlTitle")}</a></h5>
                    <h5 id="authenticatorLink"><a href="#/app/authenticator">${msg("authenticatorTitle")}</a></h5>
                    <h5 id="deviceActivityLink"><a href="#/app/device-activity">${msg("deviceActivityHtmlTitle")}</a></h5>
                    <h5 id="linkedAccountsLink" style="display:none"><a href="#/app/linked-accounts">${msg("linkedAccountsHtmlTitle")}</a></h5>
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
                    <h5 id="applicationsLink"><a href="#/app/applications">${msg("applicationsHtmlTitle")}</a></h5>
                </div>
              </div>
            </div>
            <div class="pf-l-gallery__item" style="display:none" id="myResourcesCard">
              <div class="pf-c-card">
                <div class="pf-c-card__header pf-c-content">
                    <h2><i class="pf-icon pf-icon-repository"></i>&nbsp${msg("myResources")}</h2>
                    <h6>${msg("resourceIntroMessage")}</h6>
                </div>
                <div class="pf-c-card__body pf-c-content">
                    <h5 id="myResourcesLink"><a href="#/app/my-resources">${msg("myResources")}</a></h5>
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
                document.getElementById("linkedAccountsLink").style.display='block';
            };
                
            if (features.isMyResourcesEnabled) {
                document.getElementById("myResourcesCard").style.display='block';
            };
        </script>

    </body>
</html>
