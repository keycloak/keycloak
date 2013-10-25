'use strict';

var module = angular.module('keycloak', [ 'keycloak.services', 'keycloak.loaders', 'keycloak.controllers', 'ui.bootstrap', 'ui.select2' ]);
var resourceRequests = 0;

module.config([ '$routeProvider', function($routeProvider) {
	
	$routeProvider

	.when('/create/realm', {
		templateUrl : 'partials/realm-detail.html',
		resolve : {
			realm : function(RealmLoader) {
				return {};
			}
		},
		controller : 'RealmDetailCtrl'
	}).when('/realms/:realm', {
		templateUrl : 'partials/realm-detail.html',
		resolve : {
			realm : function(RealmLoader) {
				return RealmLoader();
			}
		},
		controller : 'RealmDetailCtrl'
	}).when('/realms', {
		templateUrl : 'partials/realm-list.html',
		controller : 'RealmListCtrl'
	}).when('/realms/:realm/token-settings', {
            templateUrl : 'partials/realm-tokens.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                }
            },
            controller : 'RealmTokenDetailCtrl'
        }).when('/realms/:realm/social-settings', {
            templateUrl : 'partials/realm-social.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                }
            },
            controller : 'RealmSocialCtrl'
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
	}).when('/realms/:realm/users/:user', {
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
        }).when('/realms/:realm/users/:user/role-mappings', {
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
        }).when('/realms/:realm/users', {
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
                }
            },
            controller : 'RoleDetailCtrl'
        }).when('/realms/:realm/roles/:role', {
            templateUrl : 'partials/role-detail.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                role : function(RoleLoader) {
                    return RoleLoader();
                }
            },
            controller : 'RoleDetailCtrl'
        }).when('/realms/:realm/roles', {
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
                }
            },
            controller : 'ApplicationRoleDetailCtrl'
        }).when('/realms/:realm/applications/:application/roles/:role', {
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
                }
            },
            controller : 'ApplicationRoleDetailCtrl'
        }).when('/realms/:realm/applications/:application/credentials', {
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
        }).when('/realms/:realm/applications/:application/roles', {
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
        }).when('/realms/:realm/applications/:application/scope-mappings', {
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
        }).when('/realms/:realm/applications/:application', {
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
        }).when('/realms/:realm/applications', {
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
	.otherwise({
		templateUrl : 'partials/home.html'
	});
} ]);

module.config(function($httpProvider) {
	$httpProvider.responseInterceptors.push('errorInterceptor');

	var spinnerFunction = function(data, headersGetter) {
		if (resourceRequests == 0) {
			$('#loading').show();
		}
		resourceRequests++;
		return data;
	};
	$httpProvider.defaults.transformRequest.push(spinnerFunction);

	$httpProvider.responseInterceptors.push('spinnerInterceptor');

});

module.factory('errorInterceptor', function($q, $window, $rootScope, $location, Auth) {
	return function(promise) {
		return promise.then(function(response) {
			$rootScope.httpProviderError = null;
			return response;
		}, function(response) {
            if (response.status == 401) {
                console.log('session timeout?');
                Auth.loggedIn = false;
                $location.url('/');
            } else {
                $rootScope.httpProviderError = response.status;
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
				$('#loading').hide();
			}
			return response;
		}, function(response) {
			resourceRequests--;
			if (resourceRequests == 0) {
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
            $(this).find('.toggle-icons').toggleClass('icon-collapse').toggleClass('icon-expand');
            $(this).find('.toggle-icons').text($(this).text() == "Icon: expand" ? "Icon: collapse" : "Icon: expand");
            $(this).parent().find('.form-group').toggleClass('hidden');
        });
    }
});

// collapsable form fieldsets
module.directive('uncollapsed', function() {
    return function(scope, element, attrs) {
        element.prepend('<span class="icon-collapse toggle-icons">Icon: collapse</span>');
        element.click(function() {
            $(this).toggleClass('collapsed');
            $(this).find('.toggle-icons').toggleClass('icon-collapse').toggleClass('icon-expand');
            $(this).find('.toggle-icons').text($(this).text() == "Icon: expand" ? "Icon: collapse" : "Icon: expand");
            $(this).parent().find('.form-group').toggleClass('hidden');
        });
    }
});

// collapsable form fieldsets
module.directive('collapsed', function() {
    return function(scope, element, attrs) {
        element.prepend('<span class="icon-expand toggle-icons">Icon: expand</span>');
        element.parent().find('.form-group').toggleClass('hidden');
        element.click(function() {
            $(this).toggleClass('collapsed');
            $(this).find('.toggle-icons').toggleClass('icon-collapse').toggleClass('icon-expand');
            $(this).find('.toggle-icons').text($(this).text() == "Icon: expand" ? "Icon: collapse" : "Icon: expand");
            $(this).parent().find('.form-group').toggleClass('hidden');
        });
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


module.filter('remove', function() {
	return function(input, remove, attribute) {
		if (!input || !remove) {
			return input;
		}

		var out = [];
		for ( var i = 0; i < input.length; i++) {
			var e = input[i];

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

			if (e != null) {
				out.push(e);
			}
		}

		return out;
	};
});