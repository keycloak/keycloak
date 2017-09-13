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

$translateLocalStorageFactory.$inject = ['$window', '$translateCookieStorage'];
angular.module('pascalprecht.translate')

/**
 * @ngdoc object
 * @name pascalprecht.translate.$translateLocalStorage
 * @requires $window
 * @requires $translateCookieStorage
 *
 * @description
 * Abstraction layer for localStorage. This service is used when telling angular-translate
 * to use localStorage as storage.
 *
 */
.factory('$translateLocalStorage', $translateLocalStorageFactory);

function $translateLocalStorageFactory($window, $translateCookieStorage) {

  'use strict';

  // Setup adapter
  var localStorageAdapter = (function(){
    var langKey;
    return {
      /**
       * @ngdoc function
       * @name pascalprecht.translate.$translateLocalStorage#get
       * @methodOf pascalprecht.translate.$translateLocalStorage
       *
       * @description
       * Returns an item from localStorage by given name.
       *
       * @param {string} name Item name
       * @return {string} Value of item name
       */
      get: function (name) {
        if(!langKey) {
          langKey = $window.localStorage.getItem(name);
        }

        return langKey;
      },
      /**
       * @ngdoc function
       * @name pascalprecht.translate.$translateLocalStorage#set
       * @methodOf pascalprecht.translate.$translateLocalStorage
       *
       * @description
       * Sets an item in localStorage by given name.
       *
       * @deprecated use #put
       *
       * @param {string} name Item name
       * @param {string} value Item value
       */
      set: function (name, value) {
        langKey=value;
        $window.localStorage.setItem(name, value);
      },
      /**
       * @ngdoc function
       * @name pascalprecht.translate.$translateLocalStorage#put
       * @methodOf pascalprecht.translate.$translateLocalStorage
       *
       * @description
       * Sets an item in localStorage by given name.
       *
       * @param {string} name Item name
       * @param {string} value Item value
       */
      put: function (name, value) {
        langKey=value;
        $window.localStorage.setItem(name, value);
      }
    };
  }());

  var hasLocalStorageSupport = 'localStorage' in $window;
  if (hasLocalStorageSupport) {
    var testKey = 'pascalprecht.translate.storageTest';
    try {
      // this check have to be wrapped within a try/catch because on
      // a SecurityError: Dom Exception 18 on iOS
      if ($window.localStorage !== null) {
        $window.localStorage.setItem(testKey, 'foo');
        $window.localStorage.removeItem(testKey);
        hasLocalStorageSupport = true;
      } else {
        hasLocalStorageSupport = false;
      }
    } catch (e){
      hasLocalStorageSupport = false;
    }
  }
  var $translateLocalStorage = hasLocalStorageSupport ? localStorageAdapter : $translateCookieStorage;
  return $translateLocalStorage;
}

$translateLocalStorageFactory.displayName = '$translateLocalStorageFactory';
return 'pascalprecht.translate';

}));
