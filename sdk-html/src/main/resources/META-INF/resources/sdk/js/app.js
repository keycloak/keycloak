var keycloakModule = angular.module('keycloak', [ 'ngResource' ]);

keycloakModule.controller('LoginCtrl', function($scope, $resource) {
	$scope.config = $resource("example-config.json").get();
});
