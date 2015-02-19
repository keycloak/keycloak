'use strict';

// Init the application configuration module for AngularJS application
var ApplicationConfiguration = (function() {
	// Init module configuration options
	var applicationModuleName = 'dellTheme';
	var applicationModuleVendorDependencies = ['cui', 'ngCookies', 'dibCtrl', 'dibSvc'];

	// Add a new vertical module
	var registerModule = function(moduleName, dependencies) {
		// Create angular module
		angular.module(moduleName, dependencies || []);

		// Add the module to the AngularJS configuration file
		angular.module(applicationModuleName).requires.push(moduleName);
	};

  var projections = [];

  var registerProjection = function(moduleName) {
    projections.push(moduleName);
  }

	return {
		applicationModuleName: applicationModuleName,
		applicationModuleVendorDependencies: applicationModuleVendorDependencies,
    registerModule: registerModule,
		registerProjection: registerProjection,
    projections: projections
	};
})();
