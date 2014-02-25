'use strict';

var module = angular.module('keycloak', [ 'keycloak.services', 'keycloak.loaders', 'ui.bootstrap', 'ui.select2', 'angularFileUpload' ]);
var resourceRequests = 0;
var loadingTimer = -1;

angular.element(document).ready(function ($http) {
    $http.get('/auth/rest/admin/whoami').success(function(data) {
        var auth = {};
        auth.user = data;
        auth.loggedIn = true;

        module.factory('Auth', function() {
            return auth;
        });
        angular.bootstrap(document, ["keycloak"]);
    }).error(function() {
        var path = window.location.hash && window.location.hash.substring(1) || '/';
        window.location = '/auth/rest/admin/login?path=' + path;
    });
});


module.config([ '$routeProvider', function($routeProvider) {

    $routeProvider
        /*
        .when('/create/realm', {
            templateUrl : 'partials/realm-detail.html',
            resolve : {
                realm : function(RealmLoader) {
                    return {};
                }
            },
            controller : 'RealmDetailCtrl'
        })
        */
        .when('/create/realm', {
            templateUrl : 'partials/realm-create.html',
            resolve : {

            },
            controller : 'RealmCreateCtrl'
        })
        .when('/realms/:realm', {
            templateUrl : 'partials/realm-detail.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'RealmDetailCtrl'
        })
        .when('/realms', {
            templateUrl : 'partials/realm-list.html',
            controller : 'RealmListCtrl'
        })
        .when('/realms/:realm/token-settings', {
            templateUrl : 'partials/realm-tokens.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                }
            },
            controller : 'RealmTokenDetailCtrl'
        })
        .when('/realms/:realm/keys-settings', {
            templateUrl : 'partials/realm-keys.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                }
            },
            controller : 'RealmKeysDetailCtrl'
        })
        .when('/realms/:realm/social-settings', {
            templateUrl : 'partials/realm-social.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'RealmSocialCtrl'
        })
        .when('/realms/:realm/default-roles', {
            templateUrl : 'partials/realm-default-roles.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                applications : function(ApplicationListLoader) {
                    return ApplicationListLoader();
                },
                roles : function(RoleListLoader) {
                    return RoleListLoader();
                }
            },
            controller : 'RealmDefaultRolesCtrl'
        })
        .when('/realms/:realm/required-credentials', {
            templateUrl : 'partials/realm-credentials.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                }
            },
            controller : 'RealmRequiredCredentialsCtrl'
        })
        .when('/realms/:realm/smtp-settings', {
            templateUrl : 'partials/realm-smtp.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                }
            },
            controller : 'RealmSMTPSettingsCtrl'
        })
        .when('/create/user/:realm', {
            templateUrl : 'partials/user-detail.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                user : function() {
                    return {};
                }
            },
            controller : 'UserDetailCtrl'
        })
        .when('/realms/:realm/users/:user', {
            templateUrl : 'partials/user-detail.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                user : function(UserLoader) {
                    return UserLoader();
                }
            },
            controller : 'UserDetailCtrl'
        })
        .when('/realms/:realm/users/:user/user-credentials', {
            templateUrl : 'partials/user-credentials.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                user : function(UserLoader) {
                    return UserLoader();
                }
            },
            controller : 'UserCredentialsCtrl'
        })
        .when('/realms/:realm/users/:user/role-mappings', {
            templateUrl : 'partials/role-mappings.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                user : function(UserLoader) {
                    return UserLoader();
                },
                applications : function(ApplicationListLoader) {
                    return ApplicationListLoader();
                },
                roles : function(RoleListLoader) {
                    return RoleListLoader();
                }
            },
            controller : 'UserRoleMappingCtrl'
        })
        .when('/realms/:realm/users', {
            templateUrl : 'partials/user-list.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                }
            },
            controller : 'UserListCtrl'
        })

        .when('/create/role/:realm', {
            templateUrl : 'partials/role-detail.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                role : function() {
                    return {};
                },
                roles : function(RoleListLoader) {
                    return RoleListLoader();
                },
                applications : function(ApplicationListLoader) {
                    return ApplicationListLoader();
                }
            },
            controller : 'RoleDetailCtrl'
        })
        .when('/realms/:realm/roles/:role', {
            templateUrl : 'partials/role-detail.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                role : function(RoleLoader) {
                    return RoleLoader();
                },
                roles : function(RoleListLoader) {
                    return RoleListLoader();
                },
                applications : function(ApplicationListLoader) {
                    return ApplicationListLoader();
                }
            },
            controller : 'RoleDetailCtrl'
        })
        .when('/realms/:realm/roles', {
            templateUrl : 'partials/role-list.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                roles : function(RoleListLoader) {
                    return RoleListLoader();
                }
            },
            controller : 'RoleListCtrl'
        })

        .when('/create/role/:realm/applications/:application', {
            templateUrl : 'partials/application-role-detail.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                application : function(ApplicationLoader) {
                    return ApplicationLoader();
                },
                role : function() {
                    return {};
                },
                roles : function(RoleListLoader) {
                    return RoleListLoader();
                },
                applications : function(ApplicationListLoader) {
                    return ApplicationListLoader();
                }
            },
            controller : 'ApplicationRoleDetailCtrl'
        })
        .when('/realms/:realm/applications/:application/roles/:role', {
            templateUrl : 'partials/application-role-detail.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                application : function(ApplicationLoader) {
                    return ApplicationLoader();
                },
                role : function(ApplicationRoleLoader) {
                    return ApplicationRoleLoader();
                },
                roles : function(RoleListLoader) {
                    return RoleListLoader();
                },
                applications : function(ApplicationListLoader) {
                    return ApplicationListLoader();
                }
            },
            controller : 'ApplicationRoleDetailCtrl'
        })
        .when('/realms/:realm/applications/:application/credentials', {
            templateUrl : 'partials/application-credentials.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                application : function(ApplicationLoader) {
                    return ApplicationLoader();
                }
            },
            controller : 'ApplicationCredentialsCtrl'
        })
        .when('/realms/:realm/applications/:application/roles', {
            templateUrl : 'partials/application-role-list.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                application : function(ApplicationLoader) {
                    return ApplicationLoader();
                },
                roles : function(ApplicationRoleListLoader) {
                    return ApplicationRoleListLoader();
                }
            },
            controller : 'ApplicationRoleListCtrl'
        })
        .when('/realms/:realm/applications/:application/scope-mappings', {
            templateUrl : 'partials/application-scope-mappings.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                application : function(ApplicationLoader) {
                    return ApplicationLoader();
                },
                applications : function(ApplicationListLoader) {
                    return ApplicationListLoader();
                },
                roles : function(RoleListLoader) {
                    return RoleListLoader();
                }
            },
            controller : 'ApplicationScopeMappingCtrl'
        })
        .when('/realms/:realm/applications/:application/installation', {
            templateUrl : 'partials/application-installation.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                application : function(ApplicationLoader) {
                    return ApplicationLoader();
                }
            },
            controller : 'ApplicationInstallationCtrl'
        })
        .when('/realms/:realm/applications/:application/sessions', {
            templateUrl : 'partials/application-sessions.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                application : function(ApplicationLoader) {
                    return ApplicationLoader();
                }
            },
            controller : 'ApplicationSessionsCtrl'
        })
        .when('/create/application/:realm', {
            templateUrl : 'partials/application-detail.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                applications : function(ApplicationListLoader) {
                    return ApplicationListLoader();
                },
                application : function() {
                    return {};
                }
            },
            controller : 'ApplicationDetailCtrl'
        })
        .when('/realms/:realm/applications/:application', {
            templateUrl : 'partials/application-detail.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                applications : function(ApplicationListLoader) {
                    return ApplicationListLoader();
                },
                application : function(ApplicationLoader) {
                    return ApplicationLoader();
                }
            },
            controller : 'ApplicationDetailCtrl'
        })
        .when('/realms/:realm/applications', {
            templateUrl : 'partials/application-list.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                applications : function(ApplicationListLoader) {
                    return ApplicationListLoader();
                }
            },
            controller : 'ApplicationListCtrl'
        })

        // OAUTH Client

        .when('/realms/:realm/oauth-clients/:oauth/credentials', {
            templateUrl : 'partials/oauth-client-credentials.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                oauth : function(OAuthClientLoader) {
                    return OAuthClientLoader();
                }
            },
            controller : 'OAuthClientCredentialsCtrl'
        })
        .when('/realms/:realm/oauth-clients/:oauth/scope-mappings', {
            templateUrl : 'partials/oauth-client-scope-mappings.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                oauth : function(OAuthClientLoader) {
                    return OAuthClientLoader();
                },
                applications : function(ApplicationListLoader) {
                    return ApplicationListLoader();
                },
                roles : function(RoleListLoader) {
                    return RoleListLoader();
                }
            },
            controller : 'OAuthClientScopeMappingCtrl'
        })
        .when('/realms/:realm/oauth-clients/:oauth/installation', {
            templateUrl : 'partials/oauth-client-installation.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                oauth : function(OAuthClientLoader) {
                    return OAuthClientLoader();
                },
                installation : function(OAuthClientInstallationLoader) {
                    return OAuthClientInstallationLoader();
                }
            },
            controller : 'OAuthClientInstallationCtrl'
        })
        .when('/create/oauth-client/:realm', {
            templateUrl : 'partials/oauth-client-detail.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                oauth : function() {
                    return {};
                }
            },
            controller : 'OAuthClientDetailCtrl'
        })
        .when('/realms/:realm/oauth-clients/:oauth', {
            templateUrl : 'partials/oauth-client-detail.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                oauth : function(OAuthClientLoader) {
                    return OAuthClientLoader();
                }
            },
            controller : 'OAuthClientDetailCtrl'
        })
        .when('/realms/:realm/oauth-clients', {
            templateUrl : 'partials/oauth-client-list.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                oauthClients : function(OAuthClientListLoader) {
                    return OAuthClientListLoader();
                }
            },
            controller : 'OAuthClientListCtrl'
        })

        .when('/', {
            templateUrl : 'partials/home.html',
            controller : 'HomeCtrl'
        })
        .when('/mocks/:realm', {
            templateUrl : 'partials/realm-detail_mock.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                serverInfo : function(ServerInfoLoader) {
                    return ServerInfoLoader();
                }
            },
            controller : 'RealmDetailCtrl'
        })
        .otherwise({
            templateUrl : 'partials/notfound.html'
        });
} ]);

module.config(function($httpProvider) {
    $httpProvider.responseInterceptors.push('errorInterceptor');

    var spinnerFunction = function(data, headersGetter) {
        if (resourceRequests == 0) {
            loadingTimer = window.setTimeout(function() {
                $('#loading').show();
                loadingTimer = -1;
            }, 500);
        }
        resourceRequests++;
        return data;
    };
    $httpProvider.defaults.transformRequest.push(spinnerFunction);

    $httpProvider.responseInterceptors.push('spinnerInterceptor');

});

module.factory('errorInterceptor', function($q, $window, $rootScope, $location, Auth, Notifications) {
    return function(promise) {
        return promise.then(function(response) {
            return response;
        }, function(response) {
            if (response.status == 401) {
                console.log('session timeout?');
                Auth.loggedIn = false;
                window.location = '/auth/rest/admin/login?path=' + $location.path();
            } else if (response.status == 404) {
                Notifications.error("Not found");
            } else if (response.status) {
                if (response.data && response.data.errorMessage) {
                    Notifications.error(response.data.errorMessage);
                } else {
                    Notifications.error("An unexpected server error has occurred");
                }
            }
            return $q.reject(response);
        });
    };
});

module.factory('spinnerInterceptor', function($q, $window, $rootScope, $location) {
    return function(promise) {
        return promise.then(function(response) {
            resourceRequests--;
            if (resourceRequests == 0) {
                if(loadingTimer != -1) {
                    window.clearTimeout(loadingTimer);
                    loadingTimer = -1;
                }
                $('#loading').hide();
            }
            return response;
        }, function(response) {
            resourceRequests--;
            if (resourceRequests == 0) {
                if(loadingTimer != -1) {
                    window.clearTimeout(loadingTimer);
                    loadingTimer = -1;
                }
                $('#loading').hide();
            }

            return $q.reject(response);
        });
    };
});

// collapsable form fieldsets
module.directive('collapsable', function() {
    return function(scope, element, attrs) {
        element.click(function() {
            $(this).toggleClass('collapsed');
            $(this).find('.toggle-icons').toggleClass('kc-icon-collapse').toggleClass('kc-icon-expand');
            $(this).find('.toggle-icons').text($(this).text() == "Icon: expand" ? "Icon: collapse" : "Icon: expand");
            $(this).parent().find('.form-group').toggleClass('hidden');
        });
    }
});

// collapsable form fieldsets
module.directive('uncollapsed', function() {
    return function(scope, element, attrs) {
        element.prepend('<span class="kc-icon-collapse toggle-icons">Icon: collapse</span>');
        element.click(function() {
            $(this).toggleClass('collapsed');
            $(this).find('.toggle-icons').toggleClass('kc-icon-collapse').toggleClass('kc-icon-expand');
            $(this).find('.toggle-icons').text($(this).text() == "Icon: expand" ? "Icon: collapse" : "Icon: expand");
            $(this).parent().find('.form-group').toggleClass('hidden');
        });
    }
});

// collapsable form fieldsets
module.directive('collapsed', function() {
    return function(scope, element, attrs) {
        element.prepend('<span class="kc-icon-expand toggle-icons">Icon: expand</span>');
        element.parent().find('.form-group').toggleClass('hidden');
        element.click(function() {
            $(this).toggleClass('collapsed');
            $(this).find('.toggle-icons').toggleClass('kc-icon-collapse').toggleClass('kc-icon-expand');
            $(this).find('.toggle-icons').text($(this).text() == "Icon: expand" ? "Icon: collapse" : "Icon: expand");
            $(this).parent().find('.form-group').toggleClass('hidden');
        });
    }
});

/**
 * Directive for presenting an ON-OFF switch for checkbox.
 * Usage: <input ng-model="mmm" name="nnn" id="iii" onoffswitch [on-text="ooo" off-text="fff"] />
 */
module.directive('onoffswitch', function() {
    return {
        restrict: "EA",
        require: 'ngModel',
        replace: true,
        scope: {
            ngModel: '=',
            ngDisabled: '=',
            ngBind: '=',
            name: '=',
            id: '=',
            onText: '@onText',
            offText: '@offText'
        },
        compile: function(element, attrs) {
            if (!attrs.onText) { attrs.onText = "ON"; }
            if (!attrs.offText) { attrs.offText = "OFF"; }
            if (!attrs.ngDisabled) { attrs.ngDisabled = false; }

            var html = "<span><div class=\"onoffswitch\" data-ng-class=\"{disabled: ngDisabled}\" tabindex=\"0\" onkeydown=\"var code = event.keyCode || event.which; if (code === 32 || code === 13) { event.stopImmediatePropagation(); event.preventDefault(); $(event.target).find('input').click();}\">" +
                "<input type=\"checkbox\" data-ng-model=\"ngModel\" ng-disabled=\"ngDisabled\"" +
                " class=\"onoffswitch-checkbox\" name=\"" + attrs.name + "\" id=\"" + attrs.id + "\">" +
                "<label for=\"" + attrs.id + "\" class=\"onoffswitch-label\">" +
                "<span class=\"onoffswitch-inner\">" +
                "<span class=\"onoffswitch-active\">{{onText}}</span>" +
                "<span class=\"onoffswitch-inactive\">{{offText}}</span>" +
                "</span>" +
                "<span class=\"onoffswitch-switch\"></span>" +
                "</label>" +
                "</div></span>";

            element.replaceWith($(html));
        }
    }
});

module.directive('kcInput', function() {
    var d = {
        scope : true,
        replace : false,
        link : function(scope, element, attrs) {
            var form = element.children('form');
            var label = element.children('label');
            var input = element.children('input');

            var id = form.attr('name') + '.' + input.attr('name');

            element.attr('class', 'control-group');

            label.attr('class', 'control-label');
            label.attr('for', id);

            input.wrap('<div class="controls"/>');
            input.attr('id', id);

            if (!input.attr('placeHolder')) {
                input.attr('placeHolder', label.text());
            }

            if (input.attr('required')) {
                label.append(' <span class="required">*</span>');
            }
        }
    };
    return d;
});

module.directive('kcDisableForm', function() {
    var d = {
        scope : true,
        replace : false,
        link : function(scope, element, attrs) {
            var form = element.children('form');
            console.debug(form);
            var input = element.children('input');
            input.attr('disabled', 'true');
        }
    };
    return d;
});

module.directive('kcEnter', function() {
    return function(scope, element, attrs) {
        element.bind("keydown keypress", function(event) {
            if (event.which === 13) {
                scope.$apply(function() {
                    scope.$eval(attrs.kcEnter);
                });

                event.preventDefault();
            }
        });
    };
});

module.directive('kcSave', function ($compile, Notifications) {
    return {
        restrict: 'A',
        link: function ($scope, elem, attr, ctrl) {
            elem.addClass("btn btn-primary btn-lg");
            elem.attr("type","submit");
            elem.bind('click', function() {
                $scope.$apply(function() {
                    var form = elem.closest('form');
                    if (form && form.attr('name')) {
                        var ngValid = form.find('.ng-valid');
                        if ($scope[form.attr('name')].$valid) {
                            //ngValid.removeClass('error');
                            ngValid.parent().removeClass('has-error');
                            $scope['save']();
                        } else {
                            Notifications.error("Missing or invalid field(s). Please verify the fields in red.")
                            //ngValid.removeClass('error');
                            ngValid.parent().removeClass('has-error');

                            var ngInvalid = form.find('.ng-invalid');
                            //ngInvalid.addClass('error');
                            ngInvalid.parent().addClass('has-error');
                        }
                    }
                });
            })
        }
    }
});

module.directive('kcReset', function ($compile, Notifications) {
    return {
        restrict: 'A',
        link: function ($scope, elem, attr, ctrl) {
            elem.addClass("btn btn-default btn-lg");
            elem.attr("type","submit");
            elem.bind('click', function() {
                $scope.$apply(function() {
                    var form = elem.closest('form');
                    if (form && form.attr('name')) {
                        form.find('.ng-valid').removeClass('error');
                        form.find('.ng-invalid').removeClass('error');
                        $scope['reset']();
                    }
                })
            })
        }
    }
});

module.directive('kcCancel', function ($compile, Notifications) {
    return {
        restrict: 'A',
        link: function ($scope, elem, attr, ctrl) {
            elem.addClass("btn btn-default btn-lg");
            elem.attr("type","submit");
        }
    }
});

module.directive('kcDelete', function ($compile, Notifications) {
    return {
        restrict: 'A',
        link: function ($scope, elem, attr, ctrl) {
            elem.addClass("btn btn-danger btn-lg");
            elem.attr("type","submit");
        }
    }
});


module.directive('kcSelect', function ($compile, Notifications) {
    return {
        scope: {
            kcOptions: '=',
            kcModel: '=',
            id: "=",
            kcPlaceholder: '@'
        },
        restrict: 'EA',
        replace: true,
        templateUrl: 'templates/kc-select.html',
        link: function(scope, element, attr) {
            scope.updateModel = function(item) {
                scope.kcModel = item;
            };
        }
    }
});

module.directive('kcNavigation', function ($compile, Notifications) {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: 'templates/kc-navigation.html',

        compile: function(element, attrs){
            if (!attrs.kcSocial) {
                attrs.kcSocial = false;
            }
        }
    }
});

/*
*  Used to select the element (invoke $(elem).select()) on specified action list.
*  Usages kc-select-action="click mouseover"
*  When used in the textarea element, this will select/highlight the textarea content on specified action (i.e. click).
*/
module.directive('kcSelectAction', function ($compile, Notifications) {
    return {
        restrict: 'A',
        compile: function (elem, attrs) {

            var events = attrs.kcSelectAction.split(" ");

            for(var i=0; i < events.length; i++){

                elem.bind(events[i], function(){
                    elem.select();
                });
            }
        }
    }
});

module.filter('remove', function() {
    return function(input, remove, attribute) {
        if (!input || !remove) {
            return input;
        }

        var out = [];
        for ( var i = 0; i < input.length; i++) {
            var e = input[i];

            if (Array.isArray(remove)) {
                for (var j = 0; j < remove.length; j++) {
                    if (attribute) {
                        if (remove[j][attribute] == e[attribute]) {
                            e = null;
                            break;
                        }
                    } else {
                        if (remove[j] == e) {
                            e = null;
                            break;
                        }
                    }
                }
            } else {
                if (attribute) {
                    if (remove[attribute] == e[attribute]) {
                        e = null;
                    }
                } else {
                    if (remove == e) {
                        e = null;
                    }
                }
            }

            if (e != null) {
                out.push(e);
            }
        }

        return out;
    };
});

module.filter('capitalize', function() {
    return function(input) {
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }
});