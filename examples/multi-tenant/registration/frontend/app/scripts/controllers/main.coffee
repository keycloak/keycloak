'use strict'

###*
 # @ngdoc function
 # @name frontendApp.controller:MainCtrl
 # @description
 # # MainCtrl
 # Controller of the frontendApp
###
angular.module('frontendApp').controller 'MainCtrl', ($scope, $log, $location, Registration) ->

  $scope.register = ->
    $log.debug("trying to register")
    $scope.submitted = true
    $scope.loading = true

    $scope.registration.$save({}, ->
      if $scope.registration.errorMessage
        $scope.success = false
      else
        $scope.success = true

      $scope.loading = false
      $log.info("registration was successful")
    , (httpResponse) ->
      $log.error("registration was not successful")
      $scope.success = false
      $scope.loading = false
      $log.debug(httpResponse)
    )

  $scope.load = ->
    $scope.registration = new Registration({})
    $scope.loading = true
    $scope.success = false
    $scope.submitted = false

  $scope.load()