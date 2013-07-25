var keycloakModule = angular.module('keycloak', [ 'ngResource' ]);

keycloakModule.factory('messages', function() {
	var messages = {};
	messages['user_registered'] = "User registered";
	messages['invalid_provider'] = "Social provider not found";
	messages['provider_error'] = "Failed to login with social provider";
	return messages
});

keycloakModule.factory('queryParams', function($location) {
	var queryParams = {};
	var locationParameters = window.location.search.substring(1).split("&");
    for ( var i = 0; i < locationParameters.length; i++) {
        var param = locationParameters[i].split("=");
        queryParams[decodeURIComponent(param[0])] = decodeURIComponent(param[1]);
    }
    return queryParams;
});

keycloakModule.controller('GlobalCtrl', function($scope, $resource, queryParams, messages) {
	$scope.config = $resource("/keycloak-server/sdk/api/" + queryParams.application + "/login/config").get();
	$scope.info = queryParams.info && (messages[queryParams.info] || queryParams.info);
	$scope.error = queryParams.error && (messages[queryParams.error] || queryParams.error);
});