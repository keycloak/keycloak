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

angular.module('pascalprecht.translate')
/**
 * @ngdoc object
 * @name pascalprecht.translate.$translatePartialLoaderProvider
 *
 * @description
 * By using a $translatePartialLoaderProvider you can configure a list of a needed
 * translation parts directly during the configuration phase of your application's
 * lifetime. All parts you add by using this provider would be loaded by
 * angular-translate at the startup as soon as possible.
 */
  .provider('$translatePartialLoader', $translatePartialLoader);

function $translatePartialLoader() {

  'use strict';

  /**
   * @constructor
   * @name Part
   *
   * @description
   * Represents Part object to add and set parts at runtime.
   */
  function Part(name, priority) {
    this.name = name;
    this.isActive = true;
    this.tables = {};
    this.priority = priority || 0;
    this.langPromises = {};
  }

  /**
   * @name parseUrl
   * @method
   *
   * @description
   * Returns a parsed url template string and replaces given target lang
   * and part name it.
   *
   * @param {string|function} urlTemplate - Either a string containing an url pattern (with
   *                                        '{part}' and '{lang}') or a function(part, lang)
   *                                        returning a string.
   * @param {string} targetLang - Language key for language to be used.
   * @return {string} Parsed url template string
   */
  Part.prototype.parseUrl = function (urlTemplate, targetLang) {
    if (angular.isFunction(urlTemplate)) {
      return urlTemplate(this.name, targetLang);
    }
    return urlTemplate.replace(/\{part\}/g, this.name).replace(/\{lang\}/g, targetLang);
  };

  Part.prototype.getTable = function (lang, $q, $http, $httpOptions, urlTemplate, errorHandler) {

    //locals
    var self = this;
    var lastLangPromise = this.langPromises[lang];
    var deferred = $q.defer();

    //private helper helpers
    var fetchData = function () {
      return $http(
        angular.extend({
            method : 'GET',
            url : self.parseUrl(urlTemplate, lang)
          },
          $httpOptions)
      );
    };

    //private helper
    var handleNewData = function (data) {
      self.tables[lang] = data;
      deferred.resolve(data);
    };

    //private helper
    var rejectDeferredWithPartName = function () {
      deferred.reject(self.name);
    };

    //private helper
    var tryGettingThisTable = function () {
      //data fetching logic
      fetchData().then(
        function (result) {
          handleNewData(result.data);
        },
        function (errorResponse) {
          if (errorHandler) {
            errorHandler(self.name, lang, errorResponse).then(handleNewData, rejectDeferredWithPartName);
          } else {
            rejectDeferredWithPartName();
          }
        });
    };

    //loading logic
    if (!this.tables[lang]) {
      //let's try loading the data
      if (!lastLangPromise) {
        //this is the first request - just go ahead and hit the server
        tryGettingThisTable();
      } else {
        //this is an additional request after one or more unfinished or failed requests
        //chain the deferred off the previous request's promise so that this request conditionally executes
        //if the previous request succeeds then the result will be passed through, but if it fails then this request will try again and hit the server
        lastLangPromise.then(deferred.resolve, tryGettingThisTable);
      }
      //retain a reference to the last promise so we can continue the chain if another request is made before any succeed
      //you can picture the promise chain as a singly-linked list (formed by the .then handler queues) that's traversed by the execution context
      this.langPromises[lang] = deferred.promise;
    }
    else {
      //the part has already been loaded - if lastLangPromise is also undefined then the table has been populated using setPart
      //this breaks the promise chain because we're not tying langDeferred's outcome to a previous call's promise handler queues, but we don't care because there's no asynchronous execution context to keep track of anymore
      deferred.resolve(this.tables[lang]);
    }
    return deferred.promise;
  };

  var parts = {};

  function hasPart(name) {
    return Object.prototype.hasOwnProperty.call(parts, name);
  }

  function isStringValid(str) {
    return angular.isString(str) && str !== '';
  }

  function isPartAvailable(name) {
    if (!isStringValid(name)) {
      throw new TypeError('Invalid type of a first argument, a non-empty string expected.');
    }

    return (hasPart(name) && parts[name].isActive);
  }

  function deepExtend(dst, src) {
    for (var property in src) {
      if (src[property] && src[property].constructor &&
        src[property].constructor === Object) {
        dst[property] = dst[property] || {};
        deepExtend(dst[property], src[property]);
      } else {
        dst[property] = src[property];
      }
    }
    return dst;
  }

  function getPrioritizedParts() {
    var prioritizedParts = [];
    for (var part in parts) {
      if (parts[part].isActive) {
        prioritizedParts.push(parts[part]);
      }
    }
    prioritizedParts.sort(function (a, b) {
      return a.priority - b.priority;
    });
    return prioritizedParts;
  }


  /**
   * @ngdoc function
   * @name pascalprecht.translate.$translatePartialLoaderProvider#addPart
   * @methodOf pascalprecht.translate.$translatePartialLoaderProvider
   *
   * @description
   * Registers a new part of the translation table to be loaded once the
   * `angular-translate` gets into runtime phase. It does not actually load any
   * translation data, but only registers a part to be loaded in the future.
   *
   * @param {string} name A name of the part to add
   * @param {int} [priority=0] Sets the load priority of this part.
   *
   * @returns {object} $translatePartialLoaderProvider, so this method is chainable
   * @throws {TypeError} The method could throw a **TypeError** if you pass the param
   * of the wrong type. Please, note that the `name` param has to be a
   * non-empty **string**.
   */
  this.addPart = function (name, priority) {
    if (!isStringValid(name)) {
      throw new TypeError('Couldn\'t add part, part name has to be a string!');
    }

    if (!hasPart(name)) {
      parts[name] = new Part(name, priority);
    }
    parts[name].isActive = true;

    return this;
  };

  /**
   * @ngdocs function
   * @name pascalprecht.translate.$translatePartialLoaderProvider#setPart
   * @methodOf pascalprecht.translate.$translatePartialLoaderProvider
   *
   * @description
   * Sets a translation table to the specified part. This method does not make the
   * specified part available, but only avoids loading this part from the server.
   *
   * @param {string} lang A language of the given translation table
   * @param {string} part A name of the target part
   * @param {object} table A translation table to set to the specified part
   *
   * @return {object} $translatePartialLoaderProvider, so this method is chainable
   * @throws {TypeError} The method could throw a **TypeError** if you pass params
   * of the wrong type. Please, note that the `lang` and `part` params have to be a
   * non-empty **string**s and the `table` param has to be an object.
   */
  this.setPart = function (lang, part, table) {
    if (!isStringValid(lang)) {
      throw new TypeError('Couldn\'t set part.`lang` parameter has to be a string!');
    }
    if (!isStringValid(part)) {
      throw new TypeError('Couldn\'t set part.`part` parameter has to be a string!');
    }
    if (typeof table !== 'object' || table === null) {
      throw new TypeError('Couldn\'t set part. `table` parameter has to be an object!');
    }

    if (!hasPart(part)) {
      parts[part] = new Part(part);
      parts[part].isActive = false;
    }

    parts[part].tables[lang] = table;
    return this;
  };

  /**
   * @ngdoc function
   * @name pascalprecht.translate.$translatePartialLoaderProvider#deletePart
   * @methodOf pascalprecht.translate.$translatePartialLoaderProvider
   *
   * @description
   * Removes the previously added part of the translation data. So, `angular-translate` will not
   * load it at the startup.
   *
   * @param {string} name A name of the part to delete
   *
   * @returns {object} $translatePartialLoaderProvider, so this method is chainable
   *
   * @throws {TypeError} The method could throw a **TypeError** if you pass the param of the wrong
   * type. Please, note that the `name` param has to be a non-empty **string**.
   */
  this.deletePart = function (name) {
    if (!isStringValid(name)) {
      throw new TypeError('Couldn\'t delete part, first arg has to be string.');
    }

    if (hasPart(name)) {
      parts[name].isActive = false;
    }

    return this;
  };


  /**
   * @ngdoc function
   * @name pascalprecht.translate.$translatePartialLoaderProvider#isPartAvailable
   * @methodOf pascalprecht.translate.$translatePartialLoaderProvider
   *
   * @description
   * Checks if the specific part is available. A part becomes available after it was added by the
   * `addPart` method. Available parts would be loaded from the server once the `angular-translate`
   * asks the loader to that.
   *
   * @param {string} name A name of the part to check
   *
   * @returns {boolean} Returns **true** if the part is available now and **false** if not.
   *
   * @throws {TypeError} The method could throw a **TypeError** if you pass the param of the wrong
   * type. Please, note that the `name` param has to be a non-empty **string**.
   */
  this.isPartAvailable = isPartAvailable;

  /**
   * @ngdoc object
   * @name pascalprecht.translate.$translatePartialLoader
   *
   * @requires $q
   * @requires $http
   * @requires $injector
   * @requires $rootScope
   * @requires $translate
   *
   * @description
   *
   * @param {object} options Options object
   *
   * @throws {TypeError}
   */
  this.$get = ['$rootScope', '$injector', '$q', '$http',
    function ($rootScope, $injector, $q, $http) {

      /**
       * @ngdoc event
       * @name pascalprecht.translate.$translatePartialLoader#$translatePartialLoaderStructureChanged
       * @eventOf pascalprecht.translate.$translatePartialLoader
       * @eventType broadcast on root scope
       *
       * @description
       * A $translatePartialLoaderStructureChanged event is called when a state of the loader was
       * changed somehow. It could mean either some part is added or some part is deleted. Anyway when
       * you get this event the translation table is not longer current and has to be updated.
       *
       * @param {string} name A name of the part which is a reason why the event was fired
       */

      var service = function (options) {
        if (!isStringValid(options.key)) {
          throw new TypeError('Unable to load data, a key is not a non-empty string.');
        }

        if (!isStringValid(options.urlTemplate) && !angular.isFunction(options.urlTemplate)) {
          throw new TypeError('Unable to load data, a urlTemplate is not a non-empty string or not a function.');
        }

        var errorHandler = options.loadFailureHandler;
        if (errorHandler !== undefined) {
          if (!angular.isString(errorHandler)) {
            throw new Error('Unable to load data, a loadFailureHandler is not a string.');
          } else {
            errorHandler = $injector.get(errorHandler);
          }
        }

        var loaders = [],
          prioritizedParts = getPrioritizedParts();

        angular.forEach(prioritizedParts, function (part) {
          loaders.push(
            part.getTable(options.key, $q, $http, options.$http, options.urlTemplate, errorHandler)
          );
          part.urlTemplate = options.urlTemplate;
        });

        return $q.all(loaders)
          .then(function () {
            var table = {};
            prioritizedParts = getPrioritizedParts();
            angular.forEach(prioritizedParts, function (part) {
              deepExtend(table, part.tables[options.key]);
            });
            return table;
          }, function () {
            return $q.reject(options.key);
          });
      };

      /**
       * @ngdoc function
       * @name pascalprecht.translate.$translatePartialLoader#addPart
       * @methodOf pascalprecht.translate.$translatePartialLoader
       *
       * @description
       * Registers a new part of the translation table. This method does not actually perform any xhr
       * requests to get translation data. The new parts will be loaded in order of priority from the server next time
       * `angular-translate` asks the loader to load translations.
       *
       * @param {string} name A name of the part to add
       * @param {int} [priority=0] Sets the load priority of this part.
       *
       * @returns {object} $translatePartialLoader, so this method is chainable
       *
       * @fires {$translatePartialLoaderStructureChanged} The $translatePartialLoaderStructureChanged
       * event would be fired by this method in case the new part affected somehow on the loaders
       * state. This way it means that there are a new translation data available to be loaded from
       * the server.
       *
       * @throws {TypeError} The method could throw a **TypeError** if you pass the param of the wrong
       * type. Please, note that the `name` param has to be a non-empty **string**.
       */
      service.addPart = function (name, priority) {
        if (!isStringValid(name)) {
          throw new TypeError('Couldn\'t add part, first arg has to be a string');
        }

        if (!hasPart(name)) {
          parts[name] = new Part(name, priority);
          $rootScope.$emit('$translatePartialLoaderStructureChanged', name);
        } else if (!parts[name].isActive) {
          parts[name].isActive = true;
          $rootScope.$emit('$translatePartialLoaderStructureChanged', name);
        }

        return service;
      };

      /**
       * @ngdoc function
       * @name pascalprecht.translate.$translatePartialLoader#deletePart
       * @methodOf pascalprecht.translate.$translatePartialLoader
       *
       * @description
       * Deletes the previously added part of the translation data. The target part could be deleted
       * either logically or physically. When the data is deleted logically it is not actually deleted
       * from the browser, but the loader marks it as not active and prevents it from affecting on the
       * translations. If the deleted in such way part is added again, the loader will use the
       * previously loaded data rather than loading it from the server once more time. But if the data
       * is deleted physically, the loader will completely remove all information about it. So in case
       * of recycling this part will be loaded from the server again.
       *
       * @param {string} name A name of the part to delete
       * @param {boolean=} [removeData=false] An indicator if the loader has to remove a loaded
       * translation data physically. If the `removeData` if set to **false** the loaded data will not be
       * deleted physically and might be reused in the future to prevent an additional xhr requests.
       *
       * @returns {object} $translatePartialLoader, so this method is chainable
       *
       * @fires {$translatePartialLoaderStructureChanged} The $translatePartialLoaderStructureChanged
       * event would be fired by this method in case a part deletion process affects somehow on the
       * loaders state. This way it means that some part of the translation data is now deprecated and
       * the translation table has to be recompiled with the remaining translation parts.
       *
       * @throws {TypeError} The method could throw a **TypeError** if you pass some param of the
       * wrong type. Please, note that the `name` param has to be a non-empty **string** and
       * the `removeData` param has to be either **undefined** or **boolean**.
       */
      service.deletePart = function (name, removeData) {
        if (!isStringValid(name)) {
          throw new TypeError('Couldn\'t delete part, first arg has to be string');
        }

        if (removeData === undefined) {
          removeData = false;
        } else if (typeof removeData !== 'boolean') {
          throw new TypeError('Invalid type of a second argument, a boolean expected.');
        }

        if (hasPart(name)) {
          var wasActive = parts[name].isActive;
          if (removeData) {
            var $translate = $injector.get('$translate');
            var cache = $translate.loaderCache();
            if (typeof(cache) === 'string') {
              // getting on-demand instance of loader
              cache = $injector.get(cache);
            }
            // Purging items from cache...
            if (typeof(cache) === 'object') {
              angular.forEach(parts[name].tables, function (value, key) {
                cache.remove(parts[name].parseUrl(parts[name].urlTemplate, key));
              });
            }
            delete parts[name];
          } else {
            parts[name].isActive = false;
          }
          if (wasActive) {
            $rootScope.$emit('$translatePartialLoaderStructureChanged', name);
          }
        }

        return service;
      };

      /**
       * @ngdoc function
       * @name pascalprecht.translate.$translatePartialLoader#isPartLoaded
       * @methodOf pascalprecht.translate.$translatePartialLoader
       *
       * @description
       * Checks if the registered translation part is loaded into the translation table.
       *
       * @param {string} name A name of the part
       * @param {string} lang A key of the language
       *
       * @returns {boolean} Returns **true** if the translation of the part is loaded to the translation table and **false** if not.
       *
       * @throws {TypeError} The method could throw a **TypeError** if you pass the param of the wrong
       * type. Please, note that the `name` and `lang` params have to be non-empty **string**.
       */
      service.isPartLoaded = function (name, lang) {
        return angular.isDefined(parts[name]) && angular.isDefined(parts[name].tables[lang]);
      };

      /**
       * @ngdoc function
       * @name pascalprecht.translate.$translatePartialLoader#getRegisteredParts
       * @methodOf pascalprecht.translate.$translatePartialLoader
       *
       * @description
       * Gets names of the parts that were added with the `addPart`.
       *
       * @returns {array} Returns array of registered parts, if none were registered then an empty array is returned.
       */
      service.getRegisteredParts = function () {
        var registeredParts = [];
        angular.forEach(parts, function (p) {
          if (p.isActive) {
            registeredParts.push(p.name);
          }
        });
        return registeredParts;
      };


      /**
       * @ngdoc function
       * @name pascalprecht.translate.$translatePartialLoader#isPartAvailable
       * @methodOf pascalprecht.translate.$translatePartialLoader
       *
       * @description
       * Checks if a target translation part is available. The part becomes available just after it was
       * added by the `addPart` method. Part's availability does not mean that it was loaded from the
       * server, but only that it was added to the loader. The available part might be loaded next
       * time the loader is called.
       *
       * @param {string} name A name of the part to delete
       *
       * @returns {boolean} Returns **true** if the part is available now and **false** if not.
       *
       * @throws {TypeError} The method could throw a **TypeError** if you pass the param of the wrong
       * type. Please, note that the `name` param has to be a non-empty **string**.
       */
      service.isPartAvailable = isPartAvailable;

      return service;

    }];

}

$translatePartialLoader.displayName = '$translatePartialLoader';
return 'pascalprecht.translate';

}));
