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
            keycloak.init({onLoad: 'login-required'}).success(function(authenticated) {
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
                    });
                });
            }).error(function() {
                alert('failed to initialize keycloak');
            });
        </script>
        
    <!-- Old code to kick off angular ---------------------
        // Polyfill(s) for older browsers
        <script src="${resourceUrl}/node_modules/core-js/client/shim.min.js"></script>
        
        <script src="${resourceUrl}/node_modules/zone.js/dist/zone.min.js"></script>
        <script src="${resourceUrl}/node_modules/systemjs/dist/system.src.js"></script>

        <script src="${resourceUrl}/systemjs.config.js"></script>
        <script src="${authUrl}/js/keycloak.js"></script>
        <script>
            var keycloak = Keycloak('${authUrl}/realms/${realm}/account/keycloak.json');
            keycloak.init({onLoad: 'login-required'}).success(function(authenticated) {
                System.import('${resourceUrl}/main.js').catch(function (err) {
                    console.error(err);
                });
            }).error(function() {
                alert('failed to initialize keycloak');
            });
        </script>-->
     
        <!-- We should save these css and js into variables and then load in
             main.ts for better performance.  These might be loaded twice.
        -->
        <#if properties.styles?has_content>
            <#list properties.styles?split(' ') as style>
            <link href="${resourceUrl}/${style}" rel="stylesheet"/>
            </#list>
        </#if>
            
        <#if properties.scripts?has_content>
            <#list properties.scripts?split(' ') as script>
            <script type="text/javascript" src="${resourceUrl}/${script}"></script>
            </#list>
        </#if>
    </head>

    <app-root>
        <style>
            .kc-background {
                background: url('${resourceUrl}/app/assets/img/keycloak-bg-min.png') top left no-repeat;
                background-size: cover;
            }

            .logo-centered {
                position: fixed;
                top: 50%;
                left: 50%;
                transform: translate(-50%, -50%);
            }

            .kc-logo-text {
                background-image: url("${resourceUrl}/app/assets/img/keycloak-logo-text-min.png");
                background-repeat: no-repeat;
                width: 250px;
                height: 38px;
            }
        </style>

        <body class="cards-pf kc-background">
            <div class='logo-centered kc-logo-text'/>
        </body>
    </app-root>

</html>
