'use strict'

###*
 # @ngdoc service
 # @name frontendApp.Registration
 # @description
 # # Registration
 # Service in the frontendApp.
###
angular.module('frontendApp').service 'Registration', ($resource, config) ->
    $resource("#{config.apihost}/registration")
