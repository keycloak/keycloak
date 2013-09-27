'use strict';

var module = angular.module('keycloak.controllers', [ 'keycloak.services' ]);

var realmslist = {};


module.controller('GlobalCtrl', function($scope, $http, Auth, Current, $location, Notifications) {
	$scope.addMessage = function() {
		Notifications.success("test");
	};

	$scope.auth = Auth;
    $http.get('/auth-server/rest/saas/whoami').success(function(data, status) {
        Auth.user = data;
        Auth.loggedIn = true;
    })
        .error(function(data, status) {
            Auth.loggedIn = false;
        });

	$scope.$watch(function() {
		return $location.path();
	}, function() {
		$scope.path = $location.path().substring(1).split("/");
	});

    $http.get('/auth-server/rest/saas/admin/realms').success(function(data) {
        Current.realms = data;
        if (data.length > 0) {
            Current.realm = data[0];
            $location.url("/realms/" + Current.realm.id);
        }
    });
});

module.controller('RealmListCtrl', function($scope, Realm, Current) {
	$scope.realms = Realm.get();
    Current.realms = $scope.realms;
});

module.controller('RealmDropdownCtrl', function($scope, Realm, Current, Auth, $location) {
//    Current.realms = Realm.get();
    $scope.current = Current;
    if (Current.realms.length > 0) {
        console.log('[0]: ' + current.realms[0].realm);
    }
    $scope.changeRealm = function() {
        $location.url("/realms/" + $scope.current.realm.id);
    };
    $scope.showNav = function() {
        var show = Current.realms.length > 0;
        console.log('Show dropdown? ' + show);
        return Auth.loggedIn && show;
    }
});

module.controller('RealmDetailCtrl', function($scope, Current, Realm, realm, $http, $location, Dialog, Notifications) {
	$scope.createRealm = !realm.id;

    console.log('RealmDetailCtrl');

    if ($scope.createRealm) {
        $scope.realm = {
            enabled: true,
            requireSsl: true,
            cookieLoginAllowed: true
        };
    } else {
        if (Current.realm == null || Current.realm.id != realm.id) {
            for (var i = 0; i < Current.realms.length; i++) {
                if (realm.id == Current.realms[i].id) {
                    Current.realm = Current.realms[i];
                    break;
                }
            }
        }
        if (Current.realm == null || Current.realm.id != realm.id) {
            console.log('should be unreachable');
            console.log('Why? ' + Current.realms.length + ' ' + Current.realm);
            return;
        }
        $scope.realm = angular.copy(realm);
        $scope.realm.requireSsl = !realm.sslNotRequired;
    }

    var oldCopy = angular.copy($scope.realm);



	$scope.changed = $scope.create;

	$scope.$watch('realm', function() {
		if (!angular.equals($scope.realm, oldCopy)) {
			$scope.changed = true;
		}
	}, true);

	$scope.save = function() {
		if ($scope.realmForm.$valid) {
            var realmCopy = angular.copy($scope.realm);
            realmCopy.sslNotRequired = !realmCopy.requireSsl;
            delete realmCopy["requireSsl"];
            if ($scope.createRealm) {
				Realm.save(realmCopy, function(data, headers) {
                    console.log('creating new realm');
					var l = headers().location;
					var id = l.substring(l.lastIndexOf("/") + 1);
                    var data = Realm.query(function() {
                        Current.realms = data;
                        for (var i = 0; i < Current.realms.length; i++) {
                            if (Current.realms[i].id == id) {
                                Current.realm = Current.realms[i];
                            }
                        }
                    });
                    $location.url("/realms/" + id);
					Notifications.success("Created realm");
				});
			} else {
                console.log('updating realm...');
                $scope.changed = false;
				Realm.update(realmCopy, function() {
                    var id = realmCopy.id;
                    var data = Realm.query(function() {
                        Current.realms = data;
                        for (var i = 0; i < Current.realms.length; i++) {
                            if (Current.realms[i].id == id) {
                                Current.realm = Current.realms[i];
                                oldCopy = angular.copy($scope.realm);
                            }
                        }
                    });
                    $location.url("/realms/" + id);
					Notifications.success("Saved changes to realm");
				});
			}
		} else {
			$scope.realmForm.showErrors = true;
		}
	};

	$scope.reset = function() {
		$scope.realm = angular.copy(oldCopy);
		$scope.changed = false;
		$scope.realmForm.showErrors = false;
	};

	$scope.cancel = function() {
		$location.url("/realms");
	};

	$scope.remove = function() {
		Dialog.confirmDelete($scope.realm.name, 'realm', function() {
			Realm.remove($scope.realm, function() {
                Current.realms = Realm.get();
				$location.url("/realms");
				Notifications.success("Deleted realm");
			});
		});
	};
});

module.controller('RealmRequiredCredentialsCtrl', function($scope, Realm, realm, $http, $location, Dialog, Notifications) {
    console.log('RealmRequiredCredentialsCtrl');

    $scope.realm = {
        id : realm.id, realm : realm.realm,
        requiredCredentials : realm.requiredCredentials,
        requiredApplicationCredentials : realm.requiredApplicationCredentials,
        requiredOAuthClientCredentials : realm.requiredOAuthClientCredentials
    };

    $scope.userCredentialOptions = {
        'multiple' : true,
        'simple_tags' : true,
        'tags' : ['password', 'totp', 'cert']
    };

    var oldCopy = angular.copy($scope.realm);
    $scope.changed = false;

    $scope.$watch('realm', function() {
        if (!angular.equals($scope.realm, oldCopy)) {
            $scope.changed = true;
        }
    }, true);

    $scope.save = function() {
        if ($scope.realmForm.$valid) {
            var realmCopy = angular.copy($scope.realm);
            $scope.changed = false;
            Realm.update(realmCopy, function () {
                $location.url("/realms/" + realm.id + "/required-credentials");
                Notifications.success("Saved changes to realm");
            });
        } else {
            $scope.realmForm.showErrors = true;
        }
    };

    $scope.reset = function() {
        $scope.realm = angular.copy(oldCopy);
        $scope.changed = false;
        $scope.realmForm.showErrors = false;
    };
});


module.controller('RealmTokenDetailCtrl', function($scope, Realm, realm, $http, $location, Dialog, Notifications) {
    console.log('RealmTokenDetailCtrl');

    $scope.realm = { id : realm.id, realm : realm.realm, tokenLifespan : realm.tokenLifespan,  accessCodeLifespan : realm.accessCodeLifespan };
    $scope.realm.tokenLifespanUnit = 'Seconds';
    $scope.realm.accessCodeLifespanUnit = 'Seconds';

    var oldCopy = angular.copy($scope.realm);
    $scope.changed = false;

    $scope.$watch('realm', function() {
        if (!angular.equals($scope.realm, oldCopy)) {
            $scope.changed = true;
        }
    }, true);

    $scope.save = function() {
        if ($scope.realmForm.$valid) {
            var realmCopy = angular.copy($scope.realm);
            delete realmCopy["tokenLifespanUnit"];
            delete realmCopy["accessCodeLifespanUnit"];
            if ($scope.realm.tokenLifespanUnit == 'Minutes') {
                realmCopy.tokenLifespan = $scope.realm.tokenLifespan * 60;
            } else if ($scope.realm.tokenLifespanUnit == 'Hours') {
                realmCopy.tokenLifespan = $scope.realm.tokenLifespan * 60 * 60;
            } else if ($scope.realm.tokenLifespanUnit == 'Days') {
                realmCopy.tokenLifespan = $scope.realm.tokenLifespan * 60 * 60 * 24;
            }
            if ($scope.realm.accessCodeLifespanUnit == 'Minutes') {
                realmCopy.accessCodeLifespan = $scope.realm.accessCodeLifespan * 60;
            } else if ($scope.realm.accessCodeLifespanUnit == 'Hours') {
                realmCopy.accessCodeLifespan = $scope.realm.accessCodeLifespan * 60 * 60;
            } else if ($scope.realm.accessCodeLifespanUnit == 'Days') {
                realmCopy.accessCodeLifespan = $scope.realm.accessCodeLifespan * 60 * 60 * 24;
            }
            $scope.changed = false;
            Realm.update(realmCopy, function () {
                $location.url("/realms/" + realm.id + "/token-settings");
                Notifications.success("Saved changes to realm");
            });
        } else {
            $scope.realmForm.showErrors = true;
        }
    };

    $scope.reset = function() {
        $scope.realm = angular.copy(oldCopy);
        $scope.changed = false;
        $scope.realmForm.showErrors = false;
    };
});



module.controller('UserListCtrl', function($scope, realm, User) {
	$scope.realm = realm;

    $scope.searchQuery = function() {
        console.log('search: ' + $scope.search);
        var parameters = { search : $scope.search };
        parameters["realm"] = realm.id;
        $scope.users = User.query(parameters);
    };
});

Array.prototype.remove = function(from, to) {
    var rest = this.slice((to || from) + 1 || this.length);
    this.length = from < 0 ? this.length + from : from;
    return this.push.apply(this, rest);
};

module.controller('UserRoleMappingCtrl', function($scope, $http, realm, user, roles, applications, RealmRoleMapping, ApplicationRoleMapping, ApplicationRole) {
    $scope.realm = realm;
    $scope.user = user;
    $scope.realmRoles = angular.copy(roles);
    $scope.selectedRealmRoles = [];
    $scope.selectedRealmMappings = [];
    $scope.realmMappings = [];
    $scope.applications = applications;
    $scope.applicationRoles = [];
    $scope.selectedApplicationRoles = [];
    $scope.selectedApplicationMappings = [];
    $scope.applicationMappings = [];



    $scope.realmMappings = RealmRoleMapping.query({realm : realm.id, userId : user.username}, function(){
       for (var i = 0; i < $scope.realmMappings.length; i++) {
           var role = $scope.realmMappings[i];
           for (var j = 0; j < $scope.realmRoles.length; j++) {
               var realmRole = $scope.realmRoles[j];
               if (realmRole.id == role.id) {
                   var idx = $scope.realmRoles.indexOf(realmRole);
                   if (idx != -1) {
                       $scope.realmRoles.splice(idx, 1);
                       break;
                   }
               }
           }
       }
    });

    $scope.addRealmRole = function() {
        $http.post('/auth-server/rest/saas/admin/realms/' + realm.id + '/users/' + user.username + '/role-mappings/realm',
                   $scope.selectedRealmRoles).success(function() {
                for (var i = 0; i < $scope.selectedRealmRoles.length; i++) {
                    var role = $scope.selectedRealmRoles[i];
                    var idx = $scope.realmRoles.indexOf($scope.selectedRealmRoles[i]);
                    if (idx != -1) {
                        $scope.realmRoles.splice(idx, 1);
                        $scope.realmMappings.push(role);
                    }
                }
                $scope.selectRealmRoles = [];
            });
    };

    $scope.deleteRealmRole = function() {
        $http.delete('/auth-server/rest/saas/admin/realms/' + realm.id + '/users/' + user.username + '/role-mappings/realm',
                {data : $scope.selectedRealmMappings, headers : {"content-type" : "application/json"}}).success(function() {
                for (var i = 0; i < $scope.selectedRealmMappings.length; i++) {
                    var role = $scope.selectedRealmMappings[i];
                    var idx = $scope.realmMappings.indexOf($scope.selectedRealmMappings[i]);
                    if (idx != -1) {
                        $scope.realmMappings.splice(idx, 1);
                        $scope.realmRoles.push(role);
                    }
                }
                $scope.selectedRealmMappings = [];
            });
    };

    $scope.addApplicationRole = function() {
        $http.post('/auth-server/rest/saas/admin/realms/' + realm.id + '/users/' + user.username + '/role-mappings/applications/' + $scope.application.id,
                $scope.selectedApplicationRoles).success(function() {
                for (var i = 0; i < $scope.selectedApplicationRoles.length; i++) {
                    var role = $scope.selectedApplicationRoles[i];
                    var idx = $scope.applicationRoles.indexOf($scope.selectedApplicationRoles[i]);
                    if (idx != -1) {
                        $scope.applicationRoles.splice(idx, 1);
                        $scope.applicationMappings.push(role);
                    }
                }
                $scope.selectedApplicationRoles = [];
            });
    };

    $scope.deleteApplicationRole = function() {
        $http.delete('/auth-server/rest/saas/admin/realms/' + realm.id + '/users/' + user.username + '/role-mappings/applications/' + $scope.application.id,
            {data : $scope.selectedApplicationMappings, headers : {"content-type" : "application/json"}}).success(function() {
                for (var i = 0; i < $scope.selectedApplicationMappings.length; i++) {
                    var role = $scope.selectedApplicationMappings[i];
                    var idx = $scope.applicationMappings.indexOf($scope.selectedApplicationMappings[i]);
                    if (idx != -1) {
                        $scope.applicationMappings.splice(idx, 1);
                        $scope.applicationRoles.push(role);
                    }
                }
                $scope.selectedApplicationMappings = [];
            });
    };


    $scope.changeApplication = function() {
        $scope.applicationRoles = ApplicationRole.query({realm : realm.id, userId : user.username, application : $scope.application.id}, function() {
                $scope.applicationMappings = ApplicationRoleMapping.query({realm : realm.id, userId : user.username, application : $scope.application.id}, function(){
                    for (var i = 0; i < $scope.applicationMappings.length; i++) {
                        var role = $scope.applicationMappings[i];
                        for (var j = 0; j < $scope.applicationRoles.length; j++) {
                            var realmRole = $scope.applicationRoles[j];
                            if (realmRole.id == role.id) {
                                var idx = $scope.applicationRoles.indexOf(realmRole);
                                if (idx != -1) {
                                    $scope.applicationRoles.splice(idx, 1);
                                    break;
                                }
                            }
                        }
                    }
                });

            }
        );
    };



});

module.controller('UserDetailCtrl', function($scope, realm, user, User, $location, Dialog, Notifications) {
	$scope.realm = realm;
	$scope.user = angular.copy(user);
	$scope.create = !user.username;

	$scope.changed = $scope.create;

	$scope.$watch('user', function() {
		if (!angular.equals($scope.user, user)) {
			$scope.changed = true;
		}
	}, true);

	$scope.save = function() {
        if ($scope.create) {
            User.save({
                realm: realm.id
            }, $scope.user, function () {
                $scope.changed = false;
                user = angular.copy($scope.user);

                $location.url("/realms/" + realm.id + "/users/" + $scope.user.username);
                Notifications.success("Created user");
            });
        } else {
            User.update({
                realm: realm.id,
                userId: $scope.user.username
            }, $scope.user, function () {
                $scope.changed = false;
                user = angular.copy($scope.user);
                Notifications.success("Saved changes to user");
            });

        }
	};

	$scope.reset = function() {
		$scope.user = angular.copy(user);
		$scope.changed = false;
	};

	$scope.cancel = function() {
		$location.url("/realms/" + realm.id + "/users");
	};

	$scope.remove = function() {
		Dialog.confirmDelete($scope.user.userId, 'user', function() {
			$scope.user.$remove({
				realm : realm.id,
				userId : $scope.user.username
			}, function() {
				$location.url("/realms/" + realm.id + "/users");
				Notifications.success("Deleted user");
			});
		});
	};
});

module.controller('RoleListCtrl', function($scope, $location, realm, roles) {
    $scope.realm = realm;
    $scope.roles = roles;

    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });
});

module.controller('ApplicationRoleListCtrl', function($scope, $location, realm, application, roles) {
    $scope.realm = realm;
    $scope.roles = roles;
    $scope.application = application;

    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });
});



module.controller('ApplicationRoleDetailCtrl', function($scope, realm, application, role, ApplicationRole, $location, Dialog, Notifications) {
    $scope.realm = realm;
    $scope.application = application;
    $scope.role = angular.copy(role);
    $scope.create = !role.name;

    $scope.changed = $scope.create;

    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });

    $scope.$watch('role', function() {
        if (!angular.equals($scope.role, role)) {
            $scope.changed = true;
        }
    }, true);

    $scope.save = function() {
        if ($scope.create) {
            ApplicationRole.save({
                realm: realm.id,
                application : application.id
            }, $scope.role, function (data, headers) {
                $scope.changed = false;
                role = angular.copy($scope.role);

                var l = headers().location;
                var id = l.substring(l.lastIndexOf("/") + 1);
                $location.url("/realms/" + realm.id + "/applications/" + application.id + "/roles/" + id);
                Notifications.success("Created role");

            });
        } else {
            ApplicationRole.update({
                realm : realm.id,
                application : application.id,
                roleId : role.id
            }, $scope.role, function() {
                $scope.changed = false;
                role = angular.copy($scope.role);
                Notifications.success("Saved changes to role");
            });
        }
    };

    $scope.reset = function() {
        $scope.role = angular.copy(role);
        $scope.changed = false;
        $scope.roleForm.showErrors = false;
    };

    $scope.cancel = function() {
        $location.url("/realms/" + realm.id + "/applications/" + application.id + "/roles");
    };

    $scope.remove = function() {
        Dialog.confirmDelete($scope.role.name, 'role', function() {
            $scope.role.$remove({
                realm : realm.id,
                application : application.id,
                role : $scope.role.name
            }, function() {
                $location.url("/realms/" + realm.id + "/applications/" + application.id + "/roles");
                Notifications.success("Deleted role");
            });
        });
    };
});

module.controller('ApplicationListCtrl', function($scope, realm, applications, Application, $location) {
    console.log('ApplicationListCtrl');
    $scope.realm = realm;
    $scope.applications = applications;
    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });
});

module.controller('ApplicationDetailCtrl', function($scope, realm, application, Application, $location, Dialog, Notifications) {
    console.log('ApplicationDetailCtrl');

    $scope.realm = realm;
    $scope.create = !application.id;
    if (!$scope.create) {
        $scope.application= angular.copy(application);
    } else {
        $scope.application = {};
    }

    $scope.$watch(function() {
        return $location.path();
    }, function() {
        $scope.path = $location.path().substring(1).split("/");
    });

    $scope.$watch('application', function() {
        console.log('watch application');
        if (!angular.equals($scope.application, application)) {
            console.log('application changed');
            $scope.changed = true;
        }
    }, true);


    $scope.save = function() {
        if ($scope.applicationForm.$valid) {

            if ($scope.create) {
                Application.save({
                    realm: realm.id
                }, $scope.application, function (data, headers) {
                    $scope.changed = false;
                    var l = headers().location;
                    var id = l.substring(l.lastIndexOf("/") + 1);
                    $location.url("/realms/" + realm.id + "/applications/" + id);
                    Notifications.success("Created application");
                });
            } else {
                Application.update({
                    realm : realm.id,
                    id : application.id
                }, $scope.application, function() {
                    $scope.changed = false;
                    application = angular.copy($scope.application);
                    Notifications.success("Saved changes to application");
                });
            }

        } else {
            $scope.applicationForm.showErrors = true;
        }
    };

    $scope.reset = function() {
        $scope.application = angular.copy(application);
        $scope.changed = false;
        $scope.applicationForm.showErrors = false;
    };

    $scope.cancel = function() {
        $location.url("/realms/" + realm.id + "/applications");
    };

    $scope.remove = function() {
        Dialog.confirmDelete($scope.application.name, 'application', function() {
            $scope.application.$remove({
                realm : realm.id,
                id : $scope.application.id
            }, function() {
                $location.url("/realms/" + realm.id + "/applications");
                Notifications.success("Deleted application");
            });
        });
    };


});



module.controller('RoleMappingCtrl', function($scope, realm, User, users, role, RoleMapping, Notifications) {
	$scope.realm = realm;
	$scope.realmId = realm.realm || realm.id;
	$scope.allUsers = User.query({ realm : $scope.realmId });
	$scope.users = users;
	$scope.role = role;

	$scope.addUser = function() {
		var user = $scope.newUser;
		$scope.newUser = null;
		
		for ( var i = 0; i < $scope.allUsers.length; i++) {
			if ($scope.allUsers[i].userId == user) {
				user = $scope.allUsers[i];
				RoleMapping.save({
					realm : $scope.realmId,
					role : role
				}, user, function() {
					$scope.users = RoleMapping.query({
						realm : $scope.realmId,
						role : role
					});
					Notifications.success("Added role mapping for user");
				});
			}
		}
	}

	$scope.removeUser = function(userId) {
		for (var i = 0; i < $scope.users.length; i++) {
			var user = $scope.users[i];
			if ($scope.users[i].userId == userId) {
				RoleMapping.delete({
					realm : $scope.realmId,
					role : role
				}, user, function() {
					$scope.users = RoleMapping.query({
						realm : $scope.realmId,
						role : role
					});

					Notifications.success("Removed role mapping for user");
				});
			}
		}
	}
});
