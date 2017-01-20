<!DOCTYPE html>
<html>
<head>
    <title></title>

    <meta charset="utf-8">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="robots" content="noindex, nofollow">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">

    <link rel="shortcut icon" href="${resourceUrl}/img/favicon.ico">
    <#if properties.styles?has_content>
    <#list properties.styles?split(' ') as style>
    <link href="${resourceUrl}/${style}" rel="stylesheet" />
    </#list>
    </#if>

    <script type="text/javascript">
        var authUrl = '${authUrl}';
        var resourceUrl = '${resourceUrl}';
        var masterRealm = '${masterRealm}';
    </script>

    <script src="${resourceUrl}/lib/jquery/jquery-1.10.2.js" type="text/javascript"></script>
    <script src="${resourceUrl}/lib/select2-3.4.1/select2.min.js" type="text/javascript"></script>

    <script src="${resourceUrl}/lib/angular/angular.js"></script>
    <script src="${resourceUrl}/lib/angular/angular-resource.js"></script>
    <script src="${resourceUrl}/lib/angular/angular-route.js"></script>
    <script src="${resourceUrl}/lib/angular/angular-cookies.js"></script>
    <script src="${resourceUrl}/lib/angular/angular-sanitize.js"></script>
    <script src="${resourceUrl}/lib/angular/angular-translate.js"></script>
    <script src="${resourceUrl}/lib/angular/angular-translate-loader-url.js"></script>
    <script src="${resourceUrl}/lib/angular/treeview/angular.treeview.js"></script>
    <script src="${resourceUrl}/lib/angular/ui-bootstrap-tpls-0.11.0.js"></script>

    <script src="${resourceUrl}/lib/angular/select2.js" type="text/javascript"></script>
    <script src="${resourceUrl}/lib/fileupload/angular-file-upload.min.js"></script>
    <script src="${resourceUrl}/lib/filesaver/FileSaver.js"></script>
    <script src="${resourceUrl}/lib/ui-ace/min/ace.js"></script>
    <script src="${resourceUrl}/lib/ui-ace/ui-ace.min.js"></script>

    <script src="${authUrl}/js/${resourceVersion}/keycloak.js" type="text/javascript"></script>

    <script src="${resourceUrl}/js/app.js" type="text/javascript"></script>
    <script src="${resourceUrl}/js/controllers/realm.js" type="text/javascript"></script>
    <script src="${resourceUrl}/js/controllers/clients.js" type="text/javascript"></script>
    <script src="${resourceUrl}/js/controllers/users.js" type="text/javascript"></script>
    <script src="${resourceUrl}/js/controllers/groups.js" type="text/javascript"></script>
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
        <span class="pficon-layered" ng-show="notification.type == 'danger'">
            <span class="pficon pficon-error-octagon"></span>
            <span class="pficon pficon-error-exclamation"></span>
        </span>
        <span class="pficon-layered" ng-show="notification.type == 'warning'">
            <span class="pficon pficon-warning-triangle"></span>
            <span class="pficon pficon-warning-exclamation"></span>
        </span>
        <strong>{{notification.header}}</strong> {{notification.message}}
    </div>
</div>

<div id="loading" class="loading">Loading...</div>

</body>
</html>