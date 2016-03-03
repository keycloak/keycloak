/*!
 * angular-translate - v2.7.2 - 2015-06-01
 * http://github.com/angular-translate/angular-translate
 * Copyright (c) 2015 ; Licensed MIT
 */
(function (root, factory) {
  if (typeof define === 'function' && define.amd) {
    // AMD. Register as an anonymous module unless amdModuleId is set
    define([], function () {
      return (factory());
    });
  } else if (typeof exports === 'object') {
    // Node. Does not work with strict CommonJS, but
    // only CommonJS-like environments that support module.exports,
    // like Node.
    module.exports = factory();
  } else {
    factory();
  }
}(this, function () {

angular.module('pascalprecht.translate')
/**
 * @ngdoc object
 * @name pascalprecht.translate.$translateUrlLoader
 * @requires $q
 * @requires $http
 *
 * @description
 * Creates a loading function for a typical dynamic url pattern:
 * "locale.php?lang=en_US", "locale.php?lang=de_DE", "locale.php?language=nl_NL" etc.
 * Prefixing the specified url, the current requested, language id will be applied
 * with "?{queryParameter}={key}".
 * Using this service, the response of these urls must be an object of
 * key-value pairs.
 *
 * @param {object} options Options object, which gets the url, key and
 * optional queryParameter ('lang' is used by default).
 */
.factory('$translateUrlLoader', $translateUrlLoader);

function $translateUrlLoader($q, $http) {

  'use strict';

  return function (options) {

    if (!options || !options.url) {
      throw new Error('Couldn\'t use urlLoader since no url is given!');
    }

    var deferred = $q.defer(),
        requestParams = {};

    requestParams[options.queryParameter || 'lang'] = options.key;

    $http(angular.extend({
      url: options.url,
      params: requestParams,
      method: 'GET'
    }, options.$http)).success(function (data) {
      deferred.resolve(data);
    }).error(function () {
      deferred.reject(options.key);
    });

    return deferred.promise;
  };
}
$translateUrlLoader.$inject = ['$q', '$http'];

$translateUrlLoader.displayName = '$translateUrlLoader';
return 'pascalprecht.translate';

}));
