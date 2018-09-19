<!DOCTYPE html>
<html class="layout-pf-alt layout-pf-alt-fixed">
    <head>
        <title>${msg("accountManagementTitle")}</title>

        <meta charset="UTF-8">
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="robots" content="noindex, nofollow">
        <meta name="viewport" content="width=device-width, initial-scale=1">

        <script>
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
                var referrer_uri = '${referrer_uri}';
            </#if>

            <#if msg??>
                var locale = '${locale}';
                var l18n_msg = JSON.parse('${msgJSON?no_esc}');
            <#else>
                var locale = 'en';
                var l18n_msg = {};
            </#if>
        </script>

        <base href="${baseUrl}/">

        <link rel="icon" href="${resourceUrl}/app/assets/img/favicon.ico" type="image/x-icon"/>

        <!-- PatternFly -->
        <!-- iPad retina icon -->
        <link rel="apple-touch-icon-precomposed" sizes="152x152"
              href="${resourceUrl}/node_modules/patternfly/dist/img/apple-touch-icon-precomposed-152.png">
        <!-- iPad retina icon (iOS < 7) -->
        <link rel="apple-touch-icon-precomposed" sizes="144x144"
              href="${resourceUrl}/node_modules/patternfly/dist/img/apple-touch-icon-precomposed-144.png">
        <!-- iPad non-retina icon -->
        <link rel="apple-touch-icon-precomposed" sizes="76x76"
              href="${resourceUrl}/node_modules/patternfly/dist/img/apple-touch-icon-precomposed-76.png">
        <!-- iPad non-retina icon (iOS < 7) -->
        <link rel="apple-touch-icon-precomposed" sizes="72x72"
              href="${resourceUrl}/node_modules/patternfly/dist/img/apple-touch-icon-precomposed-72.png">
        <!-- iPhone 6 Plus icon -->
        <link rel="apple-touch-icon-precomposed" sizes="120x120"
              href="${resourceUrl}/node_modules/patternfly/dist/img/apple-touch-icon-precomposed-180.png">
        <!-- iPhone retina icon (iOS < 7) -->
        <link rel="apple-touch-icon-precomposed" sizes="114x114"
              href="${resourceUrl}/node_modules/patternfly/dist/img/apple-touch-icon-precomposed-114.png">
        <!-- iPhone non-retina icon (iOS < 7) -->
        <link rel="apple-touch-icon-precomposed" sizes="57x57"
              href="${resourceUrl}/node_modules/patternfly/dist/img/apple-touch-icon-precomposed-57.png">
        <link href="${resourceUrl}/node_modules/patternfly/dist/css/patternfly.min.css" rel="stylesheet"
              media="screen, print">
        <link href="${resourceUrl}/node_modules/patternfly/dist/css/patternfly-additions.min.css" rel="stylesheet"
              media="screen, print">
        <link rel="stylesheet" href="${resourceUrl}/node_modules/patternfly-ng/dist/css/patternfly-ng.min.css" media="screen, print">

        <script src="${resourceUrl}/node_modules/jquery/dist/jquery.min.js"></script>
        <script src="${resourceUrl}/node_modules/bootstrap/dist/js/bootstrap.min.js"></script>
        <script src="${resourceUrl}/node_modules/patternfly/dist/js/patternfly.min.js"></script>
        <script src="${authUrl}/js/keycloak.js"></script>

   <!-- TODO: We should save these css and js into variables and then load in
        main.ts for better performance.  These might be loaded twice.
        -->
        <#if properties.styles?has_content>
            <#list properties.styles?split(' ') as style>
            <link href="${resourceUrl}/${style}" rel="stylesheet"/>
            </#list>
            <a href="../../../../../../../../keycloak-quickstarts/app-profile-jee-html5/src/main/webapp/index.html"></a>
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
            var loadjs = function (url,loadListener) {
                    const script = document.createElement("script");
                    script.src = resourceUrl + url;
                    if (loadListener) script.addEventListener("load", loadListener);
                    document.head.appendChild(script);
                };
            keycloak.init({onLoad: 'check-sso'}).success(function(authenticated) {
                loadjs("/node_modules/core-js/client/shim.min.js", function() {
                    loadjs("/node_modules/zone.js/dist/zone.min.js", function() {
                        loadjs("/node_modules/systemjs/dist/system.src.js", function() {
                            loadjs("/systemjs.config.js", function() {
                                System.import('${resourceUrl}/main.js').catch(function (err) {
                                    console.error(err);
                                });
                                if (!keycloak.authenticated) document.getElementById("signInButton").style.visibility='visible';
                            });
                        });
                    });
                });
            }).error(function() {
                alert('failed to initialize keycloak');
            });
        </script>


<!-- Top Navigation -->
        <nav class="navbar navbar-pf-alt">

            <div class="navbar-header">
                <a href="http://www.keycloak.org" class="navbar-brand">
                    <img class="navbar-brand-icon" type="image/svg+xml" src="${resourceUrl}/app/assets/img/keycloak-logo-min.png" alt="" width="auto" height="30px"/>
                </a>
            </div>
            <nav class="collapse navbar-collapse">
                <ul class="nav navbar-nav">
                </ul>

                <!-- This sign in button is only displayed in the rare case where we go directly to this page and we aren't logged in.
                     Note javascript code above that changes its visibility for that case.  Also, because we are not logged in
                     we are unable to localize the button's message.  Not sure what to do about that yet.
                -->
                <ul class="nav navbar-nav navbar-right navbar-iconic">
                    <#if referrer?has_content && referrer_uri?has_content>
                        <li><a class="nav-item-iconic" href="${referrer_uri}" id="referrer"><span class="pficon-arrow"></span>${msg("backTo",referrer)}</a></li>
                    </#if>
                    <li><button id="signInButton" style="visibility:hidden" onclick="keycloak.login();" class="btn btn-primary btn-lg btn-sign" type="button">${msg("doLogIn")}</button></li>
                    <#if realm.internationalizationEnabled  && supportedLocales?size gt 1>
                        <li class="dropdown">
                          <a href="#0" class="dropdown-toggle nav-item-iconic" id="localeDropdownBtn" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                            ${msg("locale_" + locale)} <span class="caret"></span>
                          </a>
                          <ul class="dropdown-menu" aria-labelledby="localeDropdownBtn" id="localeDropdownMenu">
                          <#list supportedLocales as locale, label>
                            <li><a href="${baseUrl}/?kc_locale=${locale}">${label}</a></li>
                          </#list>
                          </ul>
                        </li>
                    </#if>
                </ul>
            </nav>
        </nav>

<!--Top Nav -->

<!-- Home Page -->

    <div class="cards-pf" id="welcomeScreen">
        <div class="text-center" id="welcomeMsg">
          <h1>${msg("accountManagementWelcomeMessage")}</h1>
        </div>
        <div class="container-fluid container-cards-pf">
            <div class="row row-cards-pf">
                <div class="col-xs-12 col-sm-4 col-md-4 col-lg-3" id="personalInfoCard">
                    <div class="card-pf card-pf-view card-pf-view-select card-pf-view-single-select">
                        <div class="card-pf-body text-center row">
                            <div class="card-pf-top-element col-xs-2 col-sm-12 col-md-12 col-lg-12">
                                <span class="fa pficon-user card-pf-icon-circle"></span>
                            </div>
                            <div class="card-pf-content col-xs-10 col-sm-12 col-md-12 col-lg-12">
                              <h2>${msg("personalInfoHtmlTitle")}</h2>
                              <p class="card-pf-content-intro">${msg("personalInfoIntroMessage")}</p>
                              <h3 id="personalInfoLink"><a href="${baseUrl}/#/account">${msg("personalInfoHtmlTitle")}</a></h3>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="col-xs-12 col-sm-4 col-md-4 col-lg-3" id="accountSecurityCard">
                    <div class="card-pf card-pf-view card-pf-view-select card-pf-view-single-select">
                        <div class="card-pf-body text-center row">
                            <div class="card-pf-top-element col-xs-2 col-sm-12 col-md-12 col-lg-12">
                                <span class="fa fa-shield card-pf-icon-circle"></span>
                            </div>
                            <div class="card-pf-content col-xs-10 col-sm-12 col-md-12 col-lg-12">
                              <h2>${msg("accountSecurityTitle")}</h2>
                              <p class="card-pf-content-intro">${msg("accountSecurityIntroMessage")}</p>
                              <h3 id="changePasswordLink"><a href="${baseUrl}/#/password">${msg("changePasswordHtmlTitle")}</a></h3>
                              <h3 id="authenticatorLink"><a href="${baseUrl}/#/authenticator">${msg("authenticatorTitle")}</a></h3>
                              <h3 id="deviceActivityLink"><a href="${baseUrl}/#/device-activity">${msg("deviceActivityHtmlTitle")}</a></h3>
                              <h3 id="linkedAccountsLink"><a href="${baseUrl}/#/linked-accounts">${msg("linkedAccountsHtmlTitle")}</a></h3>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="col-xs-12 col-sm-4 col-md-4 col-lg-3" id="applicationsCard">
                    <div class="card-pf card-pf-view card-pf-view-select card-pf-view-single-select">
                        <div class="card-pf-body text-center row">
                            <div class="card-pf-top-element col-xs-2 col-sm-12 col-md-12 col-lg-12">
                                <span class="fa fa-th card-pf-icon-circle"></span>
                            </div>
                            <div class="card-pf-content col-xs-10 col-sm-12 col-md-12 col-lg-12">
                              <h2>${msg("applicationsHtmlTitle")}</h2>
                              <p class="card-pf-content-intro">${msg("applicationsIntroMessage")}</p>
                              <h3 id="applicationsLink"><a href="${baseUrl}/#/applications">${msg("applicationsHtmlTitle")}</a></h3>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="col-xs-12 col-sm-4 col-md-4 col-lg-3" id="myResourcesCard">
                    <div class="card-pf card-pf-view card-pf-view-select card-pf-view-single-select">
                        <div class="card-pf-body text-center row">
                            <div class="card-pf-top-element col-xs-2 col-sm-12 col-md-12 col-lg-12">
                                <span class="fa pficon-repository card-pf-icon-circle"></span>
                            </div>
                            <div class="card-pf-content col-xs-10 col-sm-12 col-md-12 col-lg-12">
                              <h2>${msg("myResources")}</h2>
                              <p class="card-pf-content-intro">${msg("resourceIntroMessage")}</p>
                              <h3 id="myResourcesLink"><a href="${baseUrl}/#/my-resources">${msg("myResources")}</a></h3>
                            </div>
                        </div>
                    </div>
                </div>

            </div>
        </div>
    </div>

        <script>
            if (!features.isLinkedAccountsEnabled) {
                document.getElementById("linkedAccountsLink").style.display='none';
            }
                
            if (!features.isMyResourcesEnabled) {
                document.getElementById("myResourcesCard").style.display='none';
            }
                
            var winHash = window.location.hash;
            if ((winHash.indexOf('#/') == 0) && (!winHash.indexOf('#/&state') == 0)) {
                document.getElementById("welcomeScreen").style.display='none';
            }
        </script>

        <app-root></app-root>
    </body>
</html>
