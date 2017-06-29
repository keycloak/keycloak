/*!
 * angular-translate - v2.15.1 - 2017-03-04
 * 
 * Copyright (c) 2017 The angular-translate team, Pascal Precht; Licensed MIT
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

$translateCookieStorageFactory.$inject = ['$injector'];
angular.module('pascalprecht.translate')

/**
 * @ngdoc object
 * @name pascalprecht.translate.$translateCookieStorage
 * @requires $cookieStore
 *
 * @description
 * Abstraction layer for cookieStore. This service is used when telling angular-translate
 * to use cookieStore as storage.
 *
 */
  .factory('$translateCookieStorage', $translateCookieStorageFactory);

function $translateCookieStorageFactory($injector) {

  'use strict';

  // Since AngularJS 1.4, $cookieStore is deprecated
  var delegate;
  if (angular.version.major === 1 && angular.version.minor >= 4) {
    var $cookies = $injector.get('$cookies');
    delegate = {
      get : function (key) {
        return $cookies.get(key);
      },
      put : function (key, value) {
        $cookies.put(key, value);
      }
    };
  } else {
    var $cookieStore = $injector.get('$cookieStore');
    delegate = {
      get : function (key) {
        return $cookieStore.get(key);
      },
      put : function (key, value) {
        $cookieStore.put(key, value);
      }
    };
  }

  var $translateCookieStorage = {

    /**
     * @ngdoc function
     * @name pascalprecht.translate.$translateCookieStorage#get
     * @methodOf pascalprecht.translate.$translateCookieStorage
     *
     * @description
     * Returns an item from cookieStorage by given name.
     *
     * @param {string} name Item name
     * @return {string} Value of item name
     */
    get : function (name) {
      return delegate.get(name);
    },

    /**
     * @ngdoc function
     * @name pascalprecht.translate.$translateCookieStorage#set
     * @methodOf pascalprecht.translate.$translateCookieStorage
     *
     * @description
     * Sets an item in cookieStorage by given name.
     *
     * @deprecated use #put
     *
     * @param {string} name Item name
     * @param {string} value Item value
     */
    set : function (name, value) {
      delegate.put(name, value);
    },

    /**
     * @ngdoc function
     * @name pascalprecht.translate.$translateCookieStorage#put
     * @methodOf pascalprecht.translate.$translateCookieStorage
     *
     * @description
     * Sets an item in cookieStorage by given name.
     *
     * @param {string} name Item name
     * @param {string} value Item value
     */
    put : function (name, value) {
      delegate.put(name, value);
    }
  };

  return $translateCookieStorage;
}

$translateCookieStorageFactory.displayName = '$translateCookieStorage';
return 'pascalprecht.translate';

}));
