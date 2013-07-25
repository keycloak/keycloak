'use strict';

var module = angular.module('keycloak.controllers', [ 'keycloak.services' ]);

module.controller('GlobalCtrl', function($scope, Auth, $location, Notifications) {
	$scope.addMessage = function() {
		Notifications.success("test");
	};

	$scope.auth = Auth;

	$scope.$watch(function() {
		return $location.path();
	}, function() {
		$scope.path = $location.path().substring(1).split("/");
	});
});

module.controller('ApplicationListCtrl', function($scope, applications) {
	$scope.applications = applications;
});

module.controller('ApplicationDetailCtrl', function($scope, applications, application, Application, realms, providers, $location, $window, $dialog,
		Notifications) {
	$scope.application = angular.copy(application);
	$scope.applications = applications;
	$scope.realms = realms;
	$scope.providers = providers;

	$scope.callbackUrl = $window.location.origin + "/ejs-identity/api/callback/" + application.id;

	$scope.create = !application.id;

	$scope.changed = $scope.create;

	$scope.$watch('application', function() {
		if (!angular.equals($scope.application, application)) {
			$scope.changed = true;
		}
	}, true);


	$scope.addRole = function() {
		if ($scope.newRole) {
			if (!$scope.application.roles) {
				$scope.application.roles = [];
			}

			$scope.application.roles.push($scope.newRole);
			$scope.newRole = null;
		}
	}

	$scope.removeRole = function(i) {
		$scope.application.roles.splice(i, 1);
	};

	$scope.addInitialRole = function() {
		if ($scope.newInitialRole) {
			if (!$scope.application.initialRoles) {
				$scope.application.initialRoles = [];
			}

			$scope.application.initialRoles.push($scope.newInitialRole);
			$scope.newInitialRole = null;
		}
	}

	$scope.removeInitialRole = function(i) {
		$scope.application.initialRoles.splice(i, 1);
	};
	
	$scope.save = function() {
		if ($scope.applicationForm.$valid) {
			if ($scope.create) {
				Application.save($scope.application, function(data, headers) {
					var l = headers().location;
					var id = l.substring(l.lastIndexOf("/") + 1);
					$location.url("/applications/" + id);
					Notifications.success("Created application");
				});
			} else {
				Application.update($scope.application, function() {
					$scope.changed = false;
					application = angular.copy($scope.application);
					Notifications.success("Saved changes to the application");
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
		$location.url("/applications");
	};

	$scope.remove = function() {
		var title = 'Delete ' + $scope.application.name;
		var msg = 'Are you sure you want to permanently delete this application?';
		var btns = [ {
			result : 'cancel',
			label : 'Cancel'
		}, {
			result : 'ok',
			label : 'Delete this application',
			cssClass : 'btn-primary'
		} ];

		$dialog.messageBox(title, msg, btns).open().then(function(result) {
			if (result == "ok") {
				$scope.application.$remove(function() {
					$location.url("/applications");
					Notifications.success("Deleted application");
				});
			}
		});
	};

	$scope.availableProviders = [];

	$scope.addProvider = function() {
		if (!$scope.application.providers) {
			$scope.application.providers = [];
		}

		$scope.application.providers.push({
			"providerId" : $scope.newProviderId
		});

		$scope.newProviderId = null;
	};

	$scope.getProviderDescription = function(providerId) {
		for ( var i = 0; i < $scope.providers.length; i++) {
			if ($scope.providers[i].id == providerId) {
				return $scope.providers[i];
			}
		}
	};

	$scope.removeProvider = function(i) {
		$scope.application.providers.splice(i, 1);
	};

	var updateAvailableProviders = function() {
		$scope.availableProviders.splice(0, $scope.availableProviders.length);

		for ( var i in $scope.providers) {
			var add = true;

			for ( var j in $scope.application.providers) {
				if ($scope.application.providers[j].providerId == $scope.providers[i].id) {
					add = false;
					break;
				}
			}

			if (add) {
				$scope.availableProviders.push($scope.providers[i]);
			}
		}
	};

	$scope.openHelp = function(i) {
		$scope.providerHelpModal = true;
		$scope.providerHelp = {};
		$scope.providerHelp.index = i;
		$scope.providerHelp.description = $scope.getProviderDescription($scope.application.providers[i].providerId);
	};

	$scope.closeHelp = function() {
		$scope.providerHelpModal = false;
		$scope.providerHelp = null;
	};

	$scope.$watch("providers.length + application.providers.length", updateAvailableProviders);
});

module.controller('RealmListCtrl', function($scope, realms) {
	$scope.realms = realms;
});

module.controller('UserListCtrl', function($scope, realms, realm, users) {
	$scope.realms = realms;
	$scope.realm = realm;
	$scope.users = users;
});

module.controller('UserDetailCtrl', function($scope, realms, realm, user, User, $location, $dialog, Notifications) {
	$scope.realms = realms;
	$scope.realm = realm;
	$scope.user = angular.copy(user);
	$scope.create = !user.userId;

	$scope.changed = $scope.create;

	$scope.$watch('user', function() {
		if (!angular.equals($scope.user, user)) {
			$scope.changed = true;
		}
	}, true);

	$scope.save = function() {
		if ($scope.userForm.$valid) {
			User.save({
				realmKey : realm.key
			}, $scope.user, function() {
				$scope.changed = false;
				user = angular.copy($scope.user);

				if ($scope.create) {
					$location.url("/realms/" + realm.key + "/users/" + user.userId);
					Notifications.success("Created user");
				} else {
					Notifications.success("Saved changes to user");
				}
			});
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
		$location.url("/realms/" + realm.key + "/users");
	};

	$scope.remove = function() {
		var title = 'Delete ' + $scope.user.userId;
		var msg = 'Are you sure you want to permanently delete this user?';
		var btns = [ {
			result : 'cancel',
			label : 'Cancel'
		}, {
			result : 'ok',
			label : 'Delete this user',
			cssClass : 'btn-primary'
		} ];

		$dialog.messageBox(title, msg, btns).open().then(function(result) {
			if (result == "ok") {
				$scope.user.$remove({
					realmKey : realm.key,
					userId : $scope.user.userId
				}, function() {
					$location.url("/realms/" + realm.key + "/users");
					Notifications.success("Deleted user");
				});
			}
		});
	};
});

module.controller('RealmDetailCtrl', function($scope, Realm, realms, realm, $location, $dialog, Notifications) {
	$scope.realms = realms;
	$scope.realm = angular.copy(realm);
	$scope.create = !realm.name;

	$scope.changed = $scope.create;

	$scope.$watch('realm', function() {
		if (!angular.equals($scope.realm, realm)) {
			$scope.changed = true;
		}
	}, true);

	$scope.addRole = function() {
		if ($scope.newRole) {
			if (!$scope.realm.roles) {
				$scope.realm.roles = [];
			}

			$scope.realm.roles.push($scope.newRole);
			$scope.newRole = null;
		}
	}

	$scope.removeRole = function(i) {
		$scope.realm.roles.splice(i, 1);
	};

	$scope.addInitialRole = function() {
		if ($scope.newInitialRole) {
			if (!$scope.realm.initialRoles) {
				$scope.realm.initialRoles = [];
			}

			$scope.realm.initialRoles.push($scope.newInitialRole);
			$scope.newInitialRole = null;
		}
	}

	$scope.removeInitialRole = function(i) {
		$scope.realm.initialRoles.splice(i, 1);
	};

	$scope.save = function() {
		if ($scope.realmForm.$valid) {
			if ($scope.create) {
				Realm.save($scope.realm, function(data, headers) {
					var l = headers().location;
					var id = l.substring(l.lastIndexOf("/") + 1);
					$location.url("/realms/" + id);
					Notifications.success("Created realm");
				});
			} else {
				Realm.update($scope.realm, function() {
					$scope.changed = false;
					realm = angular.copy($scope.realm);
					Notifications.success("Saved changes to realm");
				});
			}
		} else {
			$scope.realmForm.showErrors = true;
		}
	};

	$scope.reset = function() {
		$scope.realm = angular.copy(realm);
		$scope.changed = false;
		$scope.realmForm.showErrors = false;
	};

	$scope.cancel = function() {
		$location.url("/realms");
	};

	$scope.remove = function() {
		var title = 'Delete ' + $scope.realm.name;
		var msg = 'Are you sure you want to permanently delete this realm?';
		var btns = [ {
			result : 'cancel',
			label : 'Cancel'
		}, {
			result : 'ok',
			label : 'Delete this realm',
			cssClass : 'btn-primary'
		} ];

		$dialog.messageBox(title, msg, btns).open().then(function(result) {
			if (result == "ok") {
				Realm.remove($scope.realm, function() {
					$location.url("/realms");
					Notifications.success("Deleted realm");
				});
			}
		});
	};
});