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
            cookieLoginAllowed: true,
            tokenLifespan: 300,
            tokenLifespanUnit: 'SECONDS',
            accessCodeLifespan: 300,
            accessCodeLifespanUnit: 'SECONDS',
            requiredCredentials: ['password'],
            requiredOAuthClientCredentials: ['password'],
            requiredApplicationCredentials: ['password']

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
        $scope.realm.tokenLifespanUnit = 'SECONDS';
        $scope.realm.accessCodeLifespanUnit = 'SECONDS';

    }

    var oldCopy = angular.copy($scope.realm);



    $scope.userCredentialOptions = {
        'multiple' : true,
        'simple_tags' : true,
        'tags' : ['password', 'totp', 'cert']
    };

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
            delete realmCopy["tokenLifespanUnit"];
            delete realmCopy["accessCodeLifespanUnit"];
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


module.controller('UserListCtrl', function($scope, realm, User) {
	$scope.realm = realm;
    $scope.users = [];
    $scope.query = "*";
    $scope.attribute = {};
    var params = {};

    $scope.addAttribute = function() {
        console.log('queryAttribute');
        params[$scope.attribute.name] = $scope.attribute.value;
        for (var key in params) {
            $scope.query = " " + key + "=" +params[key];
        }
    };

    $scope.executeQuery = function() {
        console.log('executeQuery');
        var parameters = angular.copy(params);
        parameters["realm"] = realm.id;
        $scope.users = User.query(parameters);
    };

    $scope.clearQuery = function() {
        params = {};
        $scopre.query = "*";
        $scope.users = [];
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
		if ($scope.userForm.$valid) {
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
        } else {
			$scope.userForm.showErrors = true;
		}
	};

	$scope.reset = function() {
		$scope.user = angular.copy(user);
		$scope.changed = false;
		$scope.userForm.showErrors = false;
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

module.controller('RoleListCtrl', function($scope, realm, roles) {
    $scope.realm = realm;
    $scope.roles = roles;
});

module.controller('RoleDetailCtrl', function($scope, realm, role, Role, $location, Dialog, Notifications) {
    $scope.realm = realm;
    $scope.role = angular.copy(role);
    $scope.create = !role.name;

    $scope.changed = $scope.create;

    $scope.$watch('role', function() {
        if (!angular.equals($scope.role, role)) {
            $scope.changed = true;
        }
    }, true);

    $scope.save = function() {
        if ($scope.roleForm.$valid) {

            if ($scope.create) {
                Role.save({
                    realm: realm.id
                }, $scope.role, function (data, headers) {
                    $scope.changed = false;
                    role = angular.copy($scope.role);

                    var l = headers().location;
                    var id = l.substring(l.lastIndexOf("/") + 1);
                    $location.url("/realms/" + realm.id + "/roles/" + id);
                    Notifications.success("Created role");

                });
            } else {
                Role.update({
                    realm : realm.id,
                    roleId : role.id
                }, $scope.role, function() {
                    $scope.changed = false;
                    role = angular.copy($scope.role);
                    Notifications.success("Saved changes to role");
                });
            }

         } else {
            $scope.roleForm.showErrors = true;
        }
    };

    $scope.reset = function() {
        $scope.role = angular.copy(role);
        $scope.changed = false;
        $scope.roleForm.showErrors = false;
    };

    $scope.cancel = function() {
        $location.url("/realms/" + realm.id + "/roles");
    };

    $scope.remove = function() {
        Dialog.confirmDelete($scope.role.name, 'role', function() {
            $scope.role.$remove({
                realm : realm.id,
                role : $scope.role.name
            }, function() {
                $location.url("/realms/" + realm.id + "/roles");
                Notifications.success("Deleted role");
            });
        });
    };
});

module.controller('ApplicationListCtrl', function($scope, realm, applications, Application, $location, Dialog, Notifications) {
    console.log('ApplicationListCtrl');
    $scope.realm = realm;
    $scope.selection = {
        applications : angular.copy(applications),
        application : null
    };


    $scope.create = false;

    $scope.changeApplication = function() {
        console.log('ApplicationListCtrl.changeApplication() - ' + $scope.selection.application.name);
        $location.url("/realms/" + realm.id + "/applications/" + $scope.selection.application.id);
    };


});

module.controller('ApplicationDetailCtrl', function($scope, realm, applications, application, Application, $location, Dialog, Notifications) {
    console.log('ApplicationDetailCtrl');

    $scope.realm = realm;
    $scope.create = !application.id;
    var selection = {
        applications : null,
        application : null
    };

    selection.applications = applications;

    for (var i=0;i < selection.applications.length; i++) {
        if (selection.applications[i].name == application.name) {
            console.log('app name: ' + application.name);
            selection.application = selection.applications[i];
            break;
        }
    }

    $scope.selection = selection;
    if (!$scope.create) {
        $scope.application= selection.application;
    } else {
        $scope.application = {};
    }

    $scope.changeApplication = function() {
        console.log('ApplicationDetailCtrl.changeApplication() - ' + $scope.selection.application.name);
        $location.url("/realms/" + realm.id + "/applications/" + $scope.selection.application.id);
    };

    $scope.$watch('application', function() {
        if (!angular.equals($scope.selection.application, application)) {
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
                id : $scope.applicatino.id
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
