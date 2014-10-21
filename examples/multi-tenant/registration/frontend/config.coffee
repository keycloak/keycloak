'use strict'

###*
 # @ngdoc service
 # @name frontendApp.config
 # @description
 # # config
 # Factory in the frontendApp.
###
angular.module('frontendApp')
  .factory 'config', (environment) ->
    environment['@@environment']