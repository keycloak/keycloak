<!DOCTYPE html>
<html>
<head>
    <title></title>

    <meta charset="utf-8">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="robots" content="noindex, nofollow">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">

    <link rel="shortcut icon" href="${resourceUrl}/img/favicon.ico">
    <#if properties.stylesCommon?has_content>
    <#list properties.stylesCommon?split(' ') as style>
    <link href="${resourceCommonUrl}/${style}" rel="stylesheet" />
    </#list>
    <#list properties.styles?split(' ') as style>
    <link href="${resourceUrl}/${style}" rel="stylesheet" />
    </#list>
    </#if>

    <script type="text/javascript">
        var authServerUrl = '${authServerUrl}';
        var authUrl = '${authUrl}';
        var consoleBaseUrl = '${consoleBaseUrl}';
        var resourceUrl = '${resourceUrl}';
        var masterRealm = '${masterRealm}';
        var resourceVersion = '${resourceVersion}';
    </script>

    <!-- Minimized versions (for those that have one) -->
    <script src="${resourceCommonUrl}/node_modules/jquery/dist/jquery.min.js" type="text/javascript"></script>
    <script src="${resourceCommonUrl}/node_modules/select2/select2.js" type="text/javascript"></script>
    <script src="${resourceCommonUrl}/node_modules/angular/angular.min.js"></script>
    <script src="${resourceCommonUrl}/node_modules/angular-resource/angular-resource.min.js"></script>
    <script src="${resourceCommonUrl}/node_modules/angular-route/angular-route.min.js"></script>
    <script src="${resourceCommonUrl}/node_modules/angular-cookies/angular-cookies.min.js"></script>
    <script src="${resourceCommonUrl}/node_modules/angular-sanitize/angular-sanitize.min.js"></script>
    <script src="${resourceCommonUrl}/node_modules/angular-translate/dist/angular-translate.min.js"></script>
    <script src="${resourceCommonUrl}/node_modules/angular-translate-loader-url/angular-translate-loader-url.min.js"></script>
    <script src="${resourceCommonUrl}/node_modules/angular-ui-select2/src/select2.js" type="text/javascript"></script>
    <script src="${resourceCommonUrl}/node_modules/autofill-event/autofill-event.js"></script>

    <!-- Libraries not managed by yarn -->
    <script src="${resourceCommonUrl}/lib/angular/ui-bootstrap-tpls-0.11.0.js"></script>
    <script src="${resourceCommonUrl}/lib/angular/treeview/angular.treeview.js"></script>
    <script src="${resourceCommonUrl}/lib/fileupload/angular-file-upload.min.js"></script>
    <script src="${resourceCommonUrl}/lib/filesaver/FileSaver.js"></script>
    <script src="${resourceCommonUrl}/lib/ui-ace/min/ace.js"></script>
    <script src="${resourceCommonUrl}/lib/ui-ace/ui-ace.min.js"></script>

    <script src="${authUrl}/js/keycloak.js?version=${resourceVersion}" type="text/javascript"></script>

    <script src="${resourceUrl}/js/app.js" type="text/javascript"></script>
    <script src="${resourceUrl}/js/controllers/realm.js" type="text/javascript"></script>
    <script src="${resourceUrl}/js/controllers/clients.js" type="text/javascript"></script>
    <script src="${resourceUrl}/js/controllers/users.js" type="text/javascript"></script>
    <script src="${resourceUrl}/js/controllers/groups.js" type="text/javascript"></script>
    <script src="${resourceUrl}/js/controllers/roles.js" type="text/javascript"></script>
    <script src="${resourceUrl}/js/loaders.js" type="text/javascript"></script>
    <script src="${resourceUrl}/js/services.js" type="text/javascript"></script>

    <!-- Authorization -->
    <script src="${resourceUrl}/js/authz/authz-app.js" type="text/javascript"></script>
    <script src="${resourceUrl}/js/authz/authz-controller.js" type="text/javascript"></script>
    <script src="${resourceUrl}/js/authz/authz-services.js" type="text/javascript"></script>

    <#if properties.scripts?has_content>
        <#list properties.scripts?split(' ') as script>
            <script type="text/javascript" src="${resourceUrl}/${script}"></script>
        </#list>
    </#if>
</head>
<body data-ng-controller="GlobalCtrl" data-ng-cloak data-ng-show="auth.user">

<nav class="navbar navbar-default navbar-pf" role="navigation" data-ng-include data-src="resourceUrl + '/partials/menu.html'">
</nav>

<div class="container-fluid">
<div class="row">
    <div data-ng-view id="view"></div>
</div>
</div>

<div class="feedback-aligner" data-ng-show="notification.display">
    <div class="alert alert-{{notification.type}} alert-dismissable">
        <button type="button" class="close" data-ng-click="notification.remove()" id="notification-close">
            <span class="pficon pficon-close"/>
        </button>

        <span class="pficon pficon-ok" ng-show="notification.type == 'success'"></span>
        <span class="pficon pficon-info" ng-show="notification.type == 'info'"></span>
        <span class="pficon pficon-warning-triangle-o" ng-show="notification.type == 'warning'"></span>
        <span class="pficon pficon-error-circle-o" ng-show="notification.type == 'danger'"></span>
        <strong>{{notification.header}}</strong> {{notification.message}}
    </div>
</div>

<div id="loading" class="loading">Loading...</div>

</body>
</html>
