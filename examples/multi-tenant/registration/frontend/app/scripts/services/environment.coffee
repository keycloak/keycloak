'use strict'

###*
 # @ngdoc service
 # @name frontendApp.environment
 # @description
 # # environment
 # Constant in the frontendApp.
###
angular.module('frontendApp')
  .constant 'environment',
  {
    development: {
      apihost: 'http://localhost:8080/multitenant-registration-backend'
    }

    production: {
      apihost: 'https://rhq.server.host/registration-backend'
    }

    test: {
      apihost: 'http://test.rhq.server.host:8080/registration-backend'
    }
  }
  