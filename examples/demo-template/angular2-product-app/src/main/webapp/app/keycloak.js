System.register(['angular2/core'], function(exports_1) {
    var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
        var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
        if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
        else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
        return c > 3 && r && Object.defineProperty(target, key, r), r;
    };
    var __metadata = (this && this.__metadata) || function (k, v) {
        if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
    };
    var core_1;
    var KeycloakService;
    return {
        setters:[
            function (core_1_1) {
                core_1 = core_1_1;
            }],
        execute: function() {
            KeycloakService = (function () {
                function KeycloakService() {
                }
                KeycloakService.init = function () {
                    var keycloakAuth = new Keycloak('keycloak.json');
                    KeycloakService.auth.loggedIn = false;
                    return new Promise(function (resolve, reject) {
                        keycloakAuth.init({ onLoad: 'login-required' })
                            .success(function () {
                            KeycloakService.auth.loggedIn = true;
                            KeycloakService.auth.authz = keycloakAuth;
                            KeycloakService.auth.logoutUrl = keycloakAuth.authServerUrl + "/realms/demo/tokens/logout?redirect_uri=/angular2-product/index.html";
                            resolve(null);
                        })
                            .error(function () {
                            reject(null);
                        });
                    });
                };
                KeycloakService.prototype.logout = function () {
                    console.log('*** LOGOUT');
                    KeycloakService.auth.loggedIn = false;
                    KeycloakService.auth.authz = null;
                    window.location.href = KeycloakService.auth.logoutUrl;
                };
                KeycloakService.prototype.getToken = function () {
                    return new Promise(function (resolve, reject) {
                        if (KeycloakService.auth.authz.token) {
                            KeycloakService.auth.authz.updateToken(5).success(function () {
                                resolve(KeycloakService.auth.authz.token);
                            })
                                .error(function () {
                                reject('Failed to refresh token');
                            });
                        }
                    });
                };
                KeycloakService.auth = {};
                KeycloakService = __decorate([
                    core_1.Injectable(), 
                    __metadata('design:paramtypes', [])
                ], KeycloakService);
                return KeycloakService;
            })();
            exports_1("KeycloakService", KeycloakService);
        }
    }
});
