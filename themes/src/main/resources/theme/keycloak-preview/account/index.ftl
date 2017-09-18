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
                var referrer_uri = '${referrer_uri?html}';
            </#if>
        
            <#if msg??>
                var locale = '${locale}';
                var l18n_msg = JSON.parse('${msg}');
            </#if>
        </script>

        <base href="${baseUrl}/">

        <meta charset="UTF-8">
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="robots" content="noindex, nofollow">
        <meta name="viewport" content="width=device-width, initial-scale=1">

        <link rel="icon" href="${resourceUrl}/app/assets/img/favicon.ico" type="image/x-icon"/>

        <#if properties.styles?has_content>
            <#list properties.styles?split(' ') as style>
            <link href="${resourceUrl}/${style}" rel="stylesheet"/>
            </#list>
        </#if>

        <link rel="stylesheet" href="${resourceUrl}/styles.css">

        <!--<script src="${authUrl}/js/${resourceVersion}/keycloak.js" type="text/javascript"></script>-->

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
        <script src="${resourceUrl}/node_modules/jquery/dist/jquery.min.js"></script>
        <script src="${resourceUrl}/node_modules/bootstrap/dist/js/bootstrap.min.js"></script>
        <script src="${resourceUrl}/node_modules/jquery-match-height/dist/jquery.matchHeight-min.js"></script>
        <script src="${resourceUrl}/node_modules/patternfly/dist/js/patternfly.min.js"></script>

        <!-- Polyfill(s) for older browsers -->
        <script src="${resourceUrl}/node_modules/core-js/client/shim.min.js"></script>

        <#if properties.scripts?has_content>
            <#list properties.scripts?split(' ') as script>
            <script type="text/javascript" src="${resourceUrl}/${script}"></script>
            </#list>
        </#if>

        <script src="${resourceUrl}/node_modules/zone.js/dist/zone.js"></script>
        <script src="${resourceUrl}/node_modules/systemjs/dist/system.src.js"></script>

        <script src="${resourceUrl}/systemjs.config.js"></script>
        <script>
            System.import('${resourceUrl}/main.js').catch(function (err) {
                console.error(err);
            });
        </script>
    </head>

    <app-root>
        <style>
            .kc-background {
                background: url('${resourceUrl}/img/keycloak-bg.png') top left no-repeat;
                background-size: cover;
            }

            .logo-centered {
                position: fixed;
                top: 50%;
                left: 50%;
                transform: translate(-50%, -50%);
            }

            .kc-logo-text {
                background-image: url("${resourceUrl}/img/keycloak-logo-text.png");
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
