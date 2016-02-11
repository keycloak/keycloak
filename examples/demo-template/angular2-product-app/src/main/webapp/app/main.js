System.register(['rxjs/Rx', 'angular2/platform/browser', 'angular2/http', './keycloak', './app'], function(exports_1) {
    var browser_1, http_1, keycloak_1, app_1;
    return {
        setters:[
            function (_1) {},
            function (browser_1_1) {
                browser_1 = browser_1_1;
            },
            function (http_1_1) {
                http_1 = http_1_1;
            },
            function (keycloak_1_1) {
                keycloak_1 = keycloak_1_1;
            },
            function (app_1_1) {
                app_1 = app_1_1;
            }],
        execute: function() {
            keycloak_1.KeycloakService.init().then(function (o) {
                browser_1.bootstrap(app_1.AppComponent, [http_1.HTTP_BINDINGS, keycloak_1.KeycloakService]);
            }, function (x) {
                window.location.reload();
            });
        }
    }
});
