<!doctype html>
<html lang="en">

<head>
    <meta charset="utf-8">
    <title>Keycloak Admin Console</title>

    <link rel="icon" href="${resourceUrl}/img/favicon.ico">

    <link rel="stylesheet" href="${resourceUrl}/css/styles.css">

    <script type="text/javascript">
        var authUrl = '${authUrl}';
        var resourceUrl = '${resourceUrl}';
    </script>

    <script src="${resourceUrl}/lib/jquery/jquery-1.10.2.js" type="text/javascript"></script>
    <script src="${resourceUrl}/lib/select2-3.4.1/select2.js" type="text/javascript"></script>

    <script src="${resourceUrl}/lib/angular/angular.js"></script>
    <script src="${resourceUrl}/lib/angular/angular-resource.js"></script>
    <script src="${resourceUrl}/lib/angular/angular-route.js"></script>
    <script src="${resourceUrl}/lib/angular/ui-bootstrap-tpls-0.11.0.js"></script>

    <script src="${resourceUrl}/lib/angular/select2.js" type="text/javascript"></script>
    <script src="${resourceUrl}/lib/fileupload/angular-file-upload.min.js"></script>
    <script src="${resourceUrl}/lib/filesaver/FileSaver.js"></script>

    <script src="/auth/js/${resourceVersion}/keycloak.js" type="text/javascript"></script>

    <script src="${resourceUrl}/js/app.js" type="text/javascript"></script>
    <script src="${resourceUrl}/js/controllers/realm.js" type="text/javascript"></script>
    <script src="${resourceUrl}/js/controllers/applications.js" type="text/javascript"></script>
    <script src="${resourceUrl}/js/controllers/oauth-clients.js" type="text/javascript"></script>
    <script src="${resourceUrl}/js/controllers/users.js" type="text/javascript"></script>
    <script src="${resourceUrl}/js/loaders.js" type="text/javascript"></script>
    <script src="${resourceUrl}/js/services.js" type="text/javascript"></script>

    <style>
        [ng\:cloak], [ng-cloak], .ng-cloak {
            display: none !important;
        }
    </style>
</head>

<body class="admin-console" data-ng-controller="GlobalCtrl" data-ng-cloak data-ng-show="auth.user">

<div class="feedback-aligner" data-ng-show="notification" data-ng-click="notification = null">
    <div class="alert alert-{{notification.type}}">
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

<header class="navbar navbar-default navbar-pf navbar-main header">
    <div data-ng-include data-src="resourceUrl + '/partials/menu.html'"></div>
</header>

<div class="container" data-ng-show="auth.hasAnyAccess">
    <div data-ng-view id="view"></div>
    <div id="loading" class="loading-backdrop">
        <div class="loading">
            <span>Loading...</span>
        </div>
    </div>
</div>

</body>
</html>
