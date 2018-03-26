<!DOCTYPE html>
<html class="layout-pf-alt layout-pf-alt-fixed">
    <head>
        <title>Keycloak Account</title>

        <script>
            var authUrl = '${authUrl}';
            var baseUrl = '${baseUrl}';
            var realm = '${realm}';
            var resourceUrl = '${resourceUrl}';
                
            <#if referrer??>
                var referrer = '${referrer}';
                var referrer_uri = '${referrer_uri}';
            </#if>
        
            <#if msg??>
                var locale = '${locale}';
                var l18n_msg = JSON.parse('${msg?no_esc}');
            <#else>
                var locale = 'en';
                var l18n_msg = {};
            </#if>
        </script>

        <base href="${baseUrl}/">

        <meta charset="UTF-8">
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="robots" content="noindex, nofollow">
        <meta name="viewport" content="width=device-width, initial-scale=1">

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

        <script src="${resourceUrl}/node_modules/bootstrap/dist/js/bootstrap.min.js"></script>
        <script src="${resourceUrl}/node_modules/patternfly/dist/js/patternfly.min.js"></script>
        <script src="${authUrl}/js/keycloak.js"></script>

<!--
  This somewhat complicated script is a performance enahancement. We
  don't want to load the systemjs and angular stuff until Keycloak has
  checked with the server to see if we are logged in.  So, the js below
  is only loaded after the redirect.  This was made more complex by the
  fact that to do it this way we needed to make sure that some scripts
  are not loaded until others are finished.

  It's possible that this enhancement could cause slower performance in
  some cases because of the aformentioned serial loading.  So I've left
  the old code commented out.
        -->
        <script>
            var keycloak = Keycloak('${authUrl}/realms/${realm}/account/keycloak.json');
            keycloak.init({onLoad: 'check-sso'}).success(function(authenticated) {
                var loadjs = function (url,loadListener) {
                    const script = document.createElement("script");
                    script.src = resourceUrl + url;
                    if (loadListener) script.addEventListener("load", loadListener);
                    document.head.appendChild(script); 
                };
                loadjs("/node_modules/core-js/client/shim.min.js", function(){
                    loadjs("/node_modules/zone.js/dist/zone.min.js");
                    loadjs("/node_modules/systemjs/dist/system.src.js", function() {
                        loadjs("/systemjs.config.js");
                        System.import('${resourceUrl}/main.js').catch(function (err) {
                            console.error(err);
                        });
                        if (!keycloak.authenticated) document.getElementById("signInButton").style.visibility='visible';
                    });
                });
            }).error(function() {
                alert('failed to initialize keycloak');
            });
        </script>


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
                    <li><button id="signInButton" style="visibility:hidden" onclick="keycloak.login();" class="btn btn-primary btn-lg" type="button">Sign In</button></li>
                </ul>
            </nav>
        </nav>
        
<!--Top Nav -->
        
<!-- Home Page --->
    <div class="cards-pf" id="welcomeScreen">
        <div><h1 class="text-center">Welcome to Keycloak Account Management</h1></div>
        <div class="container-fluid container-cards-pf">
            <div class="row row-cards-pf">
                <div class="col-xs-12 col-sm-6 col-md-4 col-lg-3">
                    <div class="card-pf card-pf-view card-pf-view-select card-pf-view-single-select">
                        <div class="card-pf-body">
                            <div class="card-pf-top-element">
                                <span class="fa pficon-user card-pf-icon-circle"></span>
                            </div>
                            <h2 class="card-pf-title text-center">
                                Personal Info
                            </h2>
                            <h3 class="card-pf-info text-center">
                                <a href="${baseUrl}/#/account">Account</a>
                            </h3>
                        </div>
                    </div>
                </div>
                <div class="col-xs-12 col-sm-6 col-md-4 col-lg-3">
                    <div class="card-pf card-pf-view card-pf-view-select card-pf-view-single-select">
                        <div class="card-pf-body">
                            <div class="card-pf-top-element">
                                <span class="fa fa-shield card-pf-icon-circle"></span>
                            </div>
                            <h2 class="card-pf-title text-center">
                                Account Security
                            </h2>
                            <h3 class="card-pf-info text-center">
                                More stuff goes here
                            </h3>
                        </div>
                    </div>
                </div>
                <div class="col-xs-12 col-sm-6 col-md-4 col-lg-3">
                    <div class="card-pf card-pf-view card-pf-view-select card-pf-view-single-select">
                        <div class="card-pf-body">
                            <div class="card-pf-top-element">
                                <span class="fa fa-th card-pf-icon-circle"></span>
                            </div>
                            <h2 class="card-pf-title text-center">
                                Applications
                            </h2>
                            <h3 class="card-pf-info text-center">
                                More stuff goes here
                            </h3>
                        </div>
                    </div>
                </div>
                <div class="col-xs-12 col-sm-6 col-md-4 col-lg-3">
                    <div class="card-pf card-pf-view card-pf-view-select card-pf-view-single-select">
                        <div class="card-pf-body">
                            <div class="card-pf-top-element">
                                <span class="fa pficon-repository card-pf-icon-circle"></span>
                            </div>
                            <h2 class="card-pf-title text-center">
                                My Resources
                            </h2>
                            <h3 class="card-pf-info text-center">
                                More stuff goes here
                            </h3>
                        </div>
                    </div>
                </div>

            </div>
        </div>
    </div>

        <app-root></app-root>
    </body>
</html>
