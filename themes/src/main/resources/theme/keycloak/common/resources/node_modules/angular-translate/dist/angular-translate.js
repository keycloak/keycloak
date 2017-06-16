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

/**
 * @ngdoc overview
 * @name pascalprecht.translate
 *
 * @description
 * The main module which holds everything together.
 */
runTranslate.$inject = ['$translate'];
$translate.$inject = ['$STORAGE_KEY', '$windowProvider', '$translateSanitizationProvider', 'pascalprechtTranslateOverrider'];
$translateDefaultInterpolation.$inject = ['$interpolate', '$translateSanitization'];
translateDirective.$inject = ['$translate', '$interpolate', '$compile', '$parse', '$rootScope'];
translateAttrDirective.$inject = ['$translate', '$rootScope'];
translateCloakDirective.$inject = ['$translate', '$rootScope'];
translateFilterFactory.$inject = ['$parse', '$translate'];
$translationCache.$inject = ['$cacheFactory'];
angular.module('pascalprecht.translate', ['ng'])
  .run(runTranslate);

function runTranslate($translate) {

  'use strict';

  var key = $translate.storageKey(),
    storage = $translate.storage();

  var fallbackFromIncorrectStorageValue = function () {
    var preferred = $translate.preferredLanguage();
    if (angular.isString(preferred)) {
      $translate.use(preferred);
      // $translate.use() will also remember the language.
      // So, we don't need to call storage.put() here.
    } else {
      storage.put(key, $translate.use());
    }
  };

  fallbackFromIncorrectStorageValue.displayName = 'fallbackFromIncorrectStorageValue';

  if (storage) {
    if (!storage.get(key)) {
      fallbackFromIncorrectStorageValue();
    } else {
      $translate.use(storage.get(key))['catch'](fallbackFromIncorrectStorageValue);
    }
  } else if (angular.isString($translate.preferredLanguage())) {
    $translate.use($translate.preferredLanguage());
  }
}

runTranslate.displayName = 'runTranslate';

/**
 * @ngdoc object
 * @name pascalprecht.translate.$translateSanitizationProvider
 *
 * @description
 *
 * Configurations for $translateSanitization
 */
angular.module('pascalprecht.translate').provider('$translateSanitization', $translateSanitizationProvider);

function $translateSanitizationProvider () {

  'use strict';

  var $sanitize,
      $sce,
      currentStrategy = null, // TODO change to either 'sanitize', 'escape' or ['sanitize', 'escapeParameters'] in 3.0.
      hasConfiguredStrategy = false,
      hasShownNoStrategyConfiguredWarning = false,
      strategies;

  /**
   * Definition of a sanitization strategy function
   * @callback StrategyFunction
   * @param {string|object} value - value to be sanitized (either a string or an interpolated value map)
   * @param {string} mode - either 'text' for a string (translation) or 'params' for the interpolated params
   * @return {string|object}
   */

  /**
   * @ngdoc property
   * @name strategies
   * @propertyOf pascalprecht.translate.$translateSanitizationProvider
   *
   * @description
   * Following strategies are built-in:
   * <dl>
   *   <dt>sanitize</dt>
   *   <dd>Sanitizes HTML in the translation text using $sanitize</dd>
   *   <dt>escape</dt>
   *   <dd>Escapes HTML in the translation</dd>
   *   <dt>sanitizeParameters</dt>
   *   <dd>Sanitizes HTML in the values of the interpolation parameters using $sanitize</dd>
   *   <dt>escapeParameters</dt>
   *   <dd>Escapes HTML in the values of the interpolation parameters</dd>
   *   <dt>escaped</dt>
   *   <dd>Support legacy strategy name 'escaped' for backwards compatibility (will be removed in 3.0)</dd>
   * </dl>
   *
   */

  strategies = {
    sanitize: function (value, mode/*, context*/) {
      if (mode === 'text') {
        value = htmlSanitizeValue(value);
      }
      return value;
    },
    escape: function (value, mode/*, context*/) {
      if (mode === 'text') {
        value = htmlEscapeValue(value);
      }
      return value;
    },
    sanitizeParameters: function (value, mode/*, context*/) {
      if (mode === 'params') {
        value = mapInterpolationParameters(value, htmlSanitizeValue);
      }
      return value;
    },
    escapeParameters: function (value, mode/*, context*/) {
      if (mode === 'params') {
        value = mapInterpolationParameters(value, htmlEscapeValue);
      }
      return value;
    },
    sce: function (value, mode, context) {
      if (mode === 'text') {
        value = htmlTrustValue(value);
      } else if (mode === 'params') {
        if (context !== 'filter') {
          // do html escape in filter context #1101
          value = mapInterpolationParameters(value, htmlEscapeValue);
        }
      }
      return value;
    },
    sceParameters: function (value, mode/*, context*/) {
      if (mode === 'params') {
        value = mapInterpolationParameters(value, htmlTrustValue);
      }
      return value;
    }
  };
  // Support legacy strategy name 'escaped' for backwards compatibility.
  // TODO should be removed in 3.0
  strategies.escaped = strategies.escapeParameters;

  /**
   * @ngdoc function
   * @name pascalprecht.translate.$translateSanitizationProvider#addStrategy
   * @methodOf pascalprecht.translate.$translateSanitizationProvider
   *
   * @description
   * Adds a sanitization strategy to the list of known strategies.
   *
   * @param {string} strategyName - unique key for a strategy
   * @param {StrategyFunction} strategyFunction - strategy function
   * @returns {object} this
   */
  this.addStrategy = function (strategyName, strategyFunction) {
    strategies[strategyName] = strategyFunction;
    return this;
  };

  /**
   * @ngdoc function
   * @name pascalprecht.translate.$translateSanitizationProvider#removeStrategy
   * @methodOf pascalprecht.translate.$translateSanitizationProvider
   *
   * @description
   * Removes a sanitization strategy from the list of known strategies.
   *
   * @param {string} strategyName - unique key for a strategy
   * @returns {object} this
   */
  this.removeStrategy = function (strategyName) {
    delete strategies[strategyName];
    return this;
  };

  /**
   * @ngdoc function
   * @name pascalprecht.translate.$translateSanitizationProvider#useStrategy
   * @methodOf pascalprecht.translate.$translateSanitizationProvider
   *
   * @description
   * Selects a sanitization strategy. When an array is provided the strategies will be executed in order.
   *
   * @param {string|StrategyFunction|array} strategy The sanitization strategy / strategies which should be used. Either a name of an existing strategy, a custom strategy function, or an array consisting of multiple names and / or custom functions.
   * @returns {object} this
   */
  this.useStrategy = function (strategy) {
    hasConfiguredStrategy = true;
    currentStrategy = strategy;
    return this;
  };

  /**
   * @ngdoc object
   * @name pascalprecht.translate.$translateSanitization
   * @requires $injector
   * @requires $log
   *
   * @description
   * Sanitizes interpolation parameters and translated texts.
   *
   */
  this.$get = ['$injector', '$log', function ($injector, $log) {

    var cachedStrategyMap = {};

    var applyStrategies = function (value, mode, context, selectedStrategies) {
      angular.forEach(selectedStrategies, function (selectedStrategy) {
        if (angular.isFunction(selectedStrategy)) {
          value = selectedStrategy(value, mode, context);
        } else if (angular.isFunction(strategies[selectedStrategy])) {
          value = strategies[selectedStrategy](value, mode, context);
        } else if (angular.isString(strategies[selectedStrategy])) {
          if (!cachedStrategyMap[strategies[selectedStrategy]]) {
            try {
              cachedStrategyMap[strategies[selectedStrategy]] = $injector.get(strategies[selectedStrategy]);
            } catch (e) {
              cachedStrategyMap[strategies[selectedStrategy]] = function() {};
              throw new Error('pascalprecht.translate.$translateSanitization: Unknown sanitization strategy: \'' + selectedStrategy + '\'');
            }
          }
          value = cachedStrategyMap[strategies[selectedStrategy]](value, mode, context);
        } else {
          throw new Error('pascalprecht.translate.$translateSanitization: Unknown sanitization strategy: \'' + selectedStrategy + '\'');
        }
      });
      return value;
    };

    // TODO: should be removed in 3.0
    var showNoStrategyConfiguredWarning = function () {
      if (!hasConfiguredStrategy && !hasShownNoStrategyConfiguredWarning) {
        $log.warn('pascalprecht.translate.$translateSanitization: No sanitization strategy has been configured. This can have serious security implications. See http://angular-translate.github.io/docs/#/guide/19_security for details.');
        hasShownNoStrategyConfiguredWarning = true;
      }
    };

    if ($injector.has('$sanitize')) {
      $sanitize = $injector.get('$sanitize');
    }
    if ($injector.has('$sce')) {
      $sce = $injector.get('$sce');
    }

    return {
      /**
       * @ngdoc function
       * @name pascalprecht.translate.$translateSanitization#useStrategy
       * @methodOf pascalprecht.translate.$translateSanitization
       *
       * @description
       * Selects a sanitization strategy. When an array is provided the strategies will be executed in order.
       *
       * @param {string|StrategyFunction|array} strategy The sanitization strategy / strategies which should be used. Either a name of an existing strategy, a custom strategy function, or an array consisting of multiple names and / or custom functions.
       */
      useStrategy: (function (self) {
        return function (strategy) {
          self.useStrategy(strategy);
        };
      })(this),

      /**
       * @ngdoc function
       * @name pascalprecht.translate.$translateSanitization#sanitize
       * @methodOf pascalprecht.translate.$translateSanitization
       *
       * @description
       * Sanitizes a value.
       *
       * @param {string|object} value The value which should be sanitized.
       * @param {string} mode The current sanitization mode, either 'params' or 'text'.
       * @param {string|StrategyFunction|array} [strategy] Optional custom strategy which should be used instead of the currently selected strategy.
       * @param {string} [context] The context of this call: filter, service. Default is service
       * @returns {string|object} sanitized value
       */
      sanitize: function (value, mode, strategy, context) {
        if (!currentStrategy) {
          showNoStrategyConfiguredWarning();
        }

        if (!strategy && strategy !== null) {
          strategy = currentStrategy;
        }

        if (!strategy) {
          return value;
        }

        if (!context) {
          context = 'service';
        }

        var selectedStrategies = angular.isArray(strategy) ? strategy : [strategy];
        return applyStrategies(value, mode, context, selectedStrategies);
      }
    };
  }];

  var htmlEscapeValue = function (value) {
    var element = angular.element('<div></div>');
    element.text(value); // not chainable, see #1044
    return element.html();
  };

  var htmlSanitizeValue = function (value) {
    if (!$sanitize) {
      throw new Error('pascalprecht.translate.$translateSanitization: Error cannot find $sanitize service. Either include the ngSanitize module (https://docs.angularjs.org/api/ngSanitize) or use a sanitization strategy which does not depend on $sanitize, such as \'escape\'.');
    }
    return $sanitize(value);
  };

  var htmlTrustValue = function (value) {
    if (!$sce) {
      throw new Error('pascalprecht.translate.$translateSanitization: Error cannot find $sce service.');
    }
    return $sce.trustAsHtml(value);
  };

  var mapInterpolationParameters = function (value, iteratee, stack) {
    if (angular.isDate(value)) {
      return value;
    } else if (angular.isObject(value)) {
      var result = angular.isArray(value) ? [] : {};

      if (!stack) {
        stack = [];
      } else {
        if (stack.indexOf(value) > -1) {
          throw new Error('pascalprecht.translate.$translateSanitization: Error cannot interpolate parameter due recursive object');
        }
      }

      stack.push(value);
      angular.forEach(value, function (propertyValue, propertyKey) {

        /* Skipping function properties. */
        if (angular.isFunction(propertyValue)) {
          return;
        }

        result[propertyKey] = mapInterpolationParameters(propertyValue, iteratee, stack);
      });
      stack.splice(-1, 1); // remove last

      return result;
    } else if (angular.isNumber(value)) {
      return value;
    } else if (!angular.isUndefined(value) && value !== null) {
      return iteratee(value);
    } else {
      return value;
    }
  };
}

/**
 * @ngdoc object
 * @name pascalprecht.translate.$translateProvider
 * @description
 *
 * $translateProvider allows developers to register translation-tables, asynchronous loaders
 * and similar to configure translation behavior directly inside of a module.
 *
 */
angular.module('pascalprecht.translate')
  .constant('pascalprechtTranslateOverrider', {})
  .provider('$translate', $translate);

function $translate($STORAGE_KEY, $windowProvider, $translateSanitizationProvider, pascalprechtTranslateOverrider) {

  'use strict';

  var $translationTable = {},
    $preferredLanguage,
    $availableLanguageKeys = [],
    $languageKeyAliases,
    $fallbackLanguage,
    $fallbackWasString,
    $uses,
    $nextLang,
    $storageFactory,
    $storageKey = $STORAGE_KEY,
    $storagePrefix,
    $missingTranslationHandlerFactory,
    $interpolationFactory,
    $interpolatorFactories = [],
    $loaderFactory,
    $cloakClassName = 'translate-cloak',
    $loaderOptions,
    $notFoundIndicatorLeft,
    $notFoundIndicatorRight,
    $postCompilingEnabled = false,
    $forceAsyncReloadEnabled = false,
    $nestedObjectDelimeter = '.',
    $isReady = false,
    $keepContent = false,
    loaderCache,
    directivePriority = 0,
    statefulFilter = true,
    postProcessFn,
    uniformLanguageTagResolver = 'default',
    languageTagResolver = {
      'default' : function (tag) {
        return (tag || '').split('-').join('_');
      },
      java : function (tag) {
        var temp = (tag || '').split('-').join('_');
        var parts = temp.split('_');
        return parts.length > 1 ? (parts[0].toLowerCase() + '_' + parts[1].toUpperCase()) : temp;
      },
      bcp47 : function (tag) {
        var temp = (tag || '').split('_').join('-');
        var parts = temp.split('-');
        return parts.length > 1 ? (parts[0].toLowerCase() + '-' + parts[1].toUpperCase()) : temp;
      },
      'iso639-1' : function (tag) {
        var temp = (tag || '').split('_').join('-');
        var parts = temp.split('-');
        return parts[0].toLowerCase();
      }
    };

  var version = '2.15.1';

  // tries to determine the browsers language
  var getFirstBrowserLanguage = function () {

    // internal purpose only
    if (angular.isFunction(pascalprechtTranslateOverrider.getLocale)) {
      return pascalprechtTranslateOverrider.getLocale();
    }

    var nav = $windowProvider.$get().navigator,
      browserLanguagePropertyKeys = ['language', 'browserLanguage', 'systemLanguage', 'userLanguage'],
      i,
      language;

    // support for HTML 5.1 "navigator.languages"
    if (angular.isArray(nav.languages)) {
      for (i = 0; i < nav.languages.length; i++) {
        language = nav.languages[i];
        if (language && language.length) {
          return language;
        }
      }
    }

    // support for other well known properties in browsers
    for (i = 0; i < browserLanguagePropertyKeys.length; i++) {
      language = nav[browserLanguagePropertyKeys[i]];
      if (language && language.length) {
        return language;
      }
    }

    return null;
  };
  getFirstBrowserLanguage.displayName = 'angular-translate/service: getFirstBrowserLanguage';

  // tries to determine the browsers locale
  var getLocale = function () {
    var locale = getFirstBrowserLanguage() || '';
    if (languageTagResolver[uniformLanguageTagResolver]) {
      locale = languageTagResolver[uniformLanguageTagResolver](locale);
    }
    return locale;
  };
  getLocale.displayName = 'angular-translate/service: getLocale';

  /**
   * @name indexOf
   * @private
   *
   * @description
   * indexOf polyfill. Kinda sorta.
   *
   * @param {array} array Array to search in.
   * @param {string} searchElement Element to search for.
   *
   * @returns {int} Index of search element.
   */
  var indexOf = function (array, searchElement) {
    for (var i = 0, len = array.length; i < len; i++) {
      if (array[i] === searchElement) {
        return i;
      }
    }
    return -1;
  };

  /**
   * @name trim
   * @private
   *
   * @description
   * trim polyfill
   *
   * @returns {string} The string stripped of whitespace from both ends
   */
  var trim = function () {
    return this.toString().replace(/^\s+|\s+$/g, '');
  };

  var negotiateLocale = function (preferred) {
    if (!preferred) {
      return;
    }

    var avail = [],
      locale = angular.lowercase(preferred),
      i = 0,
      n = $availableLanguageKeys.length;

    for (; i < n; i++) {
      avail.push(angular.lowercase($availableLanguageKeys[i]));
    }

    // Check for an exact match in our list of available keys
    if (indexOf(avail, locale) > -1) {
      return preferred;
    }

    if ($languageKeyAliases) {
      var alias;
      for (var langKeyAlias in $languageKeyAliases) {
        if ($languageKeyAliases.hasOwnProperty(langKeyAlias)) {
          var hasWildcardKey = false;
          var hasExactKey = Object.prototype.hasOwnProperty.call($languageKeyAliases, langKeyAlias) &&
            angular.lowercase(langKeyAlias) === angular.lowercase(preferred);

          if (langKeyAlias.slice(-1) === '*') {
            hasWildcardKey = langKeyAlias.slice(0, -1) === preferred.slice(0, langKeyAlias.length - 1);
          }
          if (hasExactKey || hasWildcardKey) {
            alias = $languageKeyAliases[langKeyAlias];
            if (indexOf(avail, angular.lowercase(alias)) > -1) {
              return alias;
            }
          }
        }
      }
    }

    // Check for a language code without region
    var parts = preferred.split('_');

    if (parts.length > 1 && indexOf(avail, angular.lowercase(parts[0])) > -1) {
      return parts[0];
    }

    // If everything fails, return undefined.
    return;
  };

  /**
   * @ngdoc function
   * @name pascalprecht.translate.$translateProvider#translations
   * @methodOf pascalprecht.translate.$translateProvider
   *
   * @description
   * Registers a new translation table for specific language key.
   *
   * To register a translation table for specific language, pass a defined language
   * key as first parameter.
   *
   * <pre>
   *  // register translation table for language: 'de_DE'
   *  $translateProvider.translations('de_DE', {
   *    'GREETING': 'Hallo Welt!'
   *  });
   *
   *  // register another one
   *  $translateProvider.translations('en_US', {
   *    'GREETING': 'Hello world!'
   *  });
   * </pre>
   *
   * When registering multiple translation tables for for the same language key,
   * the actual translation table gets extended. This allows you to define module
   * specific translation which only get added, once a specific module is loaded in
   * your app.
   *
   * Invoking this method with no arguments returns the translation table which was
   * registered with no language key. Invoking it with a language key returns the
   * related translation table.
   *
   * @param {string} langKey A language key.
   * @param {object} translationTable A plain old JavaScript object that represents a translation table.
   *
   */
  var translations = function (langKey, translationTable) {

    if (!langKey && !translationTable) {
      return $translationTable;
    }

    if (langKey && !translationTable) {
      if (angular.isString(langKey)) {
        return $translationTable[langKey];
      }
    } else {
      if (!angular.isObject($translationTable[langKey])) {
        $translationTable[langKey] = {};
      }
      angular.extend($translationTable[langKey], flatObject(translationTable));
    }
    return this;
  };

  this.translations = translations;

  /**
   * @ngdoc function
   * @name pascalprecht.translate.$translateProvider#cloakClassName
   * @methodOf pascalprecht.translate.$translateProvider
   *
   * @description
   *
   * Let's you change the class name for `translate-cloak` directive.
   * Default class name is `translate-cloak`.
   *
   * @param {string} name translate-cloak class name
   */
  this.cloakClassName = function (name) {
    if (!name) {
      return $cloakClassName;
    }
    $cloakClassName = name;
    return this;
  };

  /**
   * @ngdoc function
   * @name pascalprecht.translate.$translateProvider#nestedObjectDelimeter
   * @methodOf pascalprecht.translate.$translateProvider
   *
   * @description
   *
   * Let's you change the delimiter for namespaced translations.
   * Default delimiter is `.`.
   *
   * @param {string} delimiter namespace separator
   */
  this.nestedObjectDelimeter = function (delimiter) {
    if (!delimiter) {
      return $nestedObjectDelimeter;
    }
    $nestedObjectDelimeter = delimiter;
    return this;
  };

  /**
   * @name flatObject
   * @private
   *
   * @description
   * Flats an object. This function is used to flatten given translation data with
   * namespaces, so they are later accessible via dot notation.
   */
  var flatObject = function (data, path, result, prevKey) {
    var key, keyWithPath, keyWithShortPath, val;

    if (!path) {
      path = [];
    }
    if (!result) {
      result = {};
    }
    for (key in data) {
      if (!Object.prototype.hasOwnProperty.call(data, key)) {
        continue;
      }
      val = data[key];
      if (angular.isObject(val)) {
        flatObject(val, path.concat(key), result, key);
      } else {
        keyWithPath = path.length ? ('' + path.join($nestedObjectDelimeter) + $nestedObjectDelimeter + key) : key;
        if (path.length && key === prevKey) {
          // Create shortcut path (foo.bar == foo.bar.bar)
          keyWithShortPath = '' + path.join($nestedObjectDelimeter);
          // Link it to original path
          result[keyWithShortPath] = '@:' + keyWithPath;
        }
        result[keyWithPath] = val;
      }
    }
    return result;
  };
  flatObject.displayName = 'flatObject';

  /**
   * @ngdoc function
   * @name pascalprecht.translate.$translateProvider#addInterpolation
   * @methodOf pascalprecht.translate.$translateProvider
   *
   * @description
   * Adds interpolation services to angular-translate, so it can manage them.
   *
   * @param {object} factory Interpolation service factory
   */
  this.addInterpolation = function (factory) {
    $interpolatorFactories.push(factory);
    return this;
  };

  /**
   * @ngdoc function
   * @name pascalprecht.translate.$translateProvider#useMessageFormatInterpolation
   * @methodOf pascalprecht.translate.$translateProvider
   *
   * @description
   * Tells angular-translate to use interpolation functionality of messageformat.js.
   * This is useful when having high level pluralization and gender selection.
   */
  this.useMessageFormatInterpolation = function () {
    return this.useInterpolation('$translateMessageFormatInterpolation');
  };

  /**
   * @ngdoc function
   * @name pascalprecht.translate.$translateProvider#useInterpolation
   * @methodOf pascalprecht.translate.$translateProvider
   *
   * @description
   * Tells angular-translate which interpolation style to use as default, application-wide.
   * Simply pass a factory/service name. The interpolation service has to implement
   * the correct interface.
   *
   * @param {string} factory Interpolation service name.
   */
  this.useInterpolation = function (factory) {
    $interpolationFactory = factory;
    return this;
  };

  /**
   * @ngdoc function
   * @name pascalprecht.translate.$translateProvider#useSanitizeStrategy
   * @methodOf pascalprecht.translate.$translateProvider
   *
   * @description
   * Simply sets a sanitation strategy type.
   *
   * @param {string} value Strategy type.
   */
  this.useSanitizeValueStrategy = function (value) {
    $translateSanitizationProvider.useStrategy(value);
    return this;
  };

  /**
   * @ngdoc function
   * @name pascalprecht.translate.$translateProvider#preferredLanguage
   * @methodOf pascalprecht.translate.$translateProvider
   *
   * @description
   * Tells the module which of the registered translation tables to use for translation
   * at initial startup by passing a language key. Similar to `$translateProvider#use`
   * only that it says which language to **prefer**.
   *
   * @param {string} langKey A language key.
   */
  this.preferredLanguage = function (langKey) {
    if (langKey) {
      setupPreferredLanguage(langKey);
      return this;
    }
    return $preferredLanguage;
  };
  var setupPreferredLanguage = function (langKey) {
    if (langKey) {
      $preferredLanguage = langKey;
    }
    return $preferredLanguage;
  };
  /**
   * @ngdoc function
   * @name pascalprecht.translate.$translateProvider#translationNotFoundIndicator
   * @methodOf pascalprecht.translate.$translateProvider
   *
   * @description
   * Sets an indicator which is used when a translation isn't found. E.g. when
   * setting the indicator as 'X' and one tries to translate a translation id
   * called `NOT_FOUND`, this will result in `X NOT_FOUND X`.
   *
   * Internally this methods sets a left indicator and a right indicator using
   * `$translateProvider.translationNotFoundIndicatorLeft()` and
   * `$translateProvider.translationNotFoundIndicatorRight()`.
   *
   * **Note**: These methods automatically add a whitespace between the indicators
   * and the translation id.
   *
   * @param {string} indicator An indicator, could be any string.
   */
  this.translationNotFoundIndicator = function (indicator) {
    this.translationNotFoundIndicatorLeft(indicator);
    this.translationNotFoundIndicatorRight(indicator);
    return this;
  };

  /**
   * ngdoc function
   * @name pascalprecht.translate.$translateProvider#translationNotFoundIndicatorLeft
   * @methodOf pascalprecht.translate.$translateProvider
   *
   * @description
   * Sets an indicator which is used when a translation isn't found left to the
   * translation id.
   *
   * @param {string} indicator An indicator.
   */
  this.translationNotFoundIndicatorLeft = function (indicator) {
    if (!indicator) {
      return $notFoundIndicatorLeft;
    }
    $notFoundIndicatorLeft = indicator;
    return this;
  };

  /**
   * ngdoc function
   * @name pascalprecht.translate.$translateProvider#translationNotFoundIndicatorLeft
   * @methodOf pascalprecht.translate.$translateProvider
   *
   * @description
   * Sets an indicator which is used when a translation isn't found right to the
   * translation id.
   *
   * @param {string} indicator An indicator.
   */
  this.translationNotFoundIndicatorRight = function (indicator) {
    if (!indicator) {
      return $notFoundIndicatorRight;
    }
    $notFoundIndicatorRight = indicator;
    return this;
  };

  /**
   * @ngdoc function
   * @name pascalprecht.translate.$translateProvider#fallbackLanguage
   * @methodOf pascalprecht.translate.$translateProvider
   *
   * @description
   * Tells the module which of the registered translation tables to use when missing translations
   * at initial startup by passing a language key. Similar to `$translateProvider#use`
   * only that it says which language to **fallback**.
   *
   * @param {string||array} langKey A language key.
   *
   */
  this.fallbackLanguage = function (langKey) {
    fallbackStack(langKey);
    return this;
  };

  var fallbackStack = function (langKey) {
    if (langKey) {
      if (angular.isString(langKey)) {
        $fallbackWasString = true;
        $fallbackLanguage = [langKey];
      } else if (angular.isArray(langKey)) {
        $fallbackWasString = false;
        $fallbackLanguage = langKey;
      }
      if (angular.isString($preferredLanguage) && indexOf($fallbackLanguage, $preferredLanguage) < 0) {
        $fallbackLanguage.push($preferredLanguage);
      }

      return this;
    } else {
      if ($fallbackWasString) {
        return $fallbackLanguage[0];
      } else {
        return $fallbackLanguage;
      }
    }
  };

  /**
   * @ngdoc function
   * @name pascalprecht.translate.$translateProvider#use
   * @methodOf pascalprecht.translate.$translateProvider
   *
   * @description
   * Set which translation table to use for translation by given language key. When
   * trying to 'use' a language which isn't provided, it'll throw an error.
   *
   * You actually don't have to use this method since `$translateProvider#preferredLanguage`
   * does the job too.
   *
   * @param {string} langKey A language key.
   */
  this.use = function (langKey) {
    if (langKey) {
      if (!$translationTable[langKey] && (!$loaderFactory)) {
        // only throw an error, when not loading translation data asynchronously
        throw new Error('$translateProvider couldn\'t find translationTable for langKey: \'' + langKey + '\'');
      }
      $uses = langKey;
      return this;
    }
    return $uses;
  };

  /**
   * @ngdoc function
   * @name pascalprecht.translate.$translateProvider#resolveClientLocale
   * @methodOf pascalprecht.translate.$translateProvider
   *
   * @description
   * This returns the current browser/client's language key. The result is processed with the configured uniform tag resolver.
   *
   * @returns {string} the current client/browser language key
   */
  this.resolveClientLocale = function () {
    return getLocale();
  };

  /**
   * @ngdoc function
   * @name pascalprecht.translate.$translateProvider#storageKey
   * @methodOf pascalprecht.translate.$translateProvider
   *
   * @description
   * Tells the module which key must represent the choosed language by a user in the storage.
   *
   * @param {string} key A key for the storage.
   */
  var storageKey = function (key) {
    if (!key) {
      if ($storagePrefix) {
        return $storagePrefix + $storageKey;
      }
      return $storageKey;
    }
    $storageKey = key;
    return this;
  };

  this.storageKey = storageKey;

  /**
   * @ngdoc function
   * @name pascalprecht.translate.$translateProvider#useUrlLoader
   * @methodOf pascalprecht.translate.$translateProvider
   *
   * @description
   * Tells angular-translate to use `$translateUrlLoader` extension service as loader.
   *
   * @param {string} url Url
   * @param {Object=} options Optional configuration object
   */
  this.useUrlLoader = function (url, options) {
    return this.useLoader('$translateUrlLoader', angular.extend({url : url}, options));
  };

  /**
   * @ngdoc function
   * @name pascalprecht.translate.$translateProvider#useStaticFilesLoader
   * @methodOf pascalprecht.translate.$translateProvider
   *
   * @description
   * Tells angular-translate to use `$translateStaticFilesLoader` extension service as loader.
   *
   * @param {Object=} options Optional configuration object
   */
  this.useStaticFilesLoader = function (options) {
    return this.useLoader('$translateStaticFilesLoader', options);
  };

  /**
   * @ngdoc function
   * @name pascalprecht.translate.$translateProvider#useLoader
   * @methodOf pascalprecht.translate.$translateProvider
   *
   * @description
   * Tells angular-translate to use any other service as loader.
   *
   * @param {string} loaderFactory Factory name to use
   * @param {Object=} options Optional configuration object
   */
  this.useLoader = function (loaderFactory, options) {
    $loaderFactory = loaderFactory;
    $loaderOptions = options || {};
    return this;
  };

  /**
   * @ngdoc function
   * @name pascalprecht.translate.$translateProvider#useLocalStorage
   * @methodOf pascalprecht.translate.$translateProvider
   *
   * @description
   * Tells angular-translate to use `$translateLocalStorage` service as storage layer.
   *
   */
  this.useLocalStorage = function () {
    return this.useStorage('$translateLocalStorage');
  };

  /**
   * @ngdoc function
   * @name pascalprecht.translate.$translateProvider#useCookieStorage
   * @methodOf pascalprecht.translate.$translateProvider
   *
   * @description
   * Tells angular-translate to use `$translateCookieStorage` service as storage layer.
   */
  this.useCookieStorage = function () {
    return this.useStorage('$translateCookieStorage');
  };

  /**
   * @ngdoc function
   * @name pascalprecht.translate.$translateProvider#useStorage
   * @methodOf pascalprecht.translate.$translateProvider
   *
   * @description
   * Tells angular-translate to use custom service as storage layer.
   */
  this.useStorage = function (storageFactory) {
    $storageFactory = storageFactory;
    return this;
  };

  /**
   * @ngdoc function
   * @name pascalprecht.translate.$translateProvider#storagePrefix
   * @methodOf pascalprecht.translate.$translateProvider
   *
   * @description
   * Sets prefix for storage key.
   *
   * @param {string} prefix Storage key prefix
   */
  this.storagePrefix = function (prefix) {
    if (!prefix) {
      return prefix;
    }
    $storagePrefix = prefix;
    return this;
  };

  /**
   * @ngdoc function
   * @name pascalprecht.translate.$translateProvider#useMissingTranslationHandlerLog
   * @methodOf pascalprecht.translate.$translateProvider
   *
   * @description
   * Tells angular-translate to use built-in log handler when trying to translate
   * a translation Id which doesn't exist.
   *
   * This is actually a shortcut method for `useMissingTranslationHandler()`.
   *
   */
  this.useMissingTranslationHandlerLog = function () {
    return this.useMissingTranslationHandler('$translateMissingTranslationHandlerLog');
  };

  /**
   * @ngdoc function
   * @name pascalprecht.translate.$translateProvider#useMissingTranslationHandler
   * @methodOf pascalprecht.translate.$translateProvider
   *
   * @description
   * Expects a factory name which later gets instantiated with `$injector`.
   * This method can be used to tell angular-translate to use a custom
   * missingTranslationHandler. Just build a factory which returns a function
   * and expects a translation id as argument.
   *
   * Example:
   * <pre>
   *  app.config(function ($translateProvider) {
   *    $translateProvider.useMissingTranslationHandler('customHandler');
   *  });
   *
   *  app.factory('customHandler', function (dep1, dep2) {
   *    return function (translationId) {
   *      // something with translationId and dep1 and dep2
   *    };
   *  });
   * </pre>
   *
   * @param {string} factory Factory name
   */
  this.useMissingTranslationHandler = function (factory) {
    $missingTranslationHandlerFactory = factory;
    return this;
  };

  /**
   * @ngdoc function
   * @name pascalprecht.translate.$translateProvider#usePostCompiling
   * @methodOf pascalprecht.translate.$translateProvider
   *
   * @description
   * If post compiling is enabled, all translated values will be processed
   * again with AngularJS' $compile.
   *
   * Example:
   * <pre>
   *  app.config(function ($translateProvider) {
   *    $translateProvider.usePostCompiling(true);
   *  });
   * </pre>
   *
   * @param {string} factory Factory name
   */
  this.usePostCompiling = function (value) {
    $postCompilingEnabled = !(!value);
    return this;
  };

  /**
   * @ngdoc function
   * @name pascalprecht.translate.$translateProvider#forceAsyncReload
   * @methodOf pascalprecht.translate.$translateProvider
   *
   * @description
   * If force async reload is enabled, async loader will always be called
   * even if $translationTable already contains the language key, adding
   * possible new entries to the $translationTable.
   *
   * Example:
   * <pre>
   *  app.config(function ($translateProvider) {
   *    $translateProvider.forceAsyncReload(true);
   *  });
   * </pre>
   *
   * @param {boolean} value - valid values are true or false
   */
  this.forceAsyncReload = function (value) {
    $forceAsyncReloadEnabled = !(!value);
    return this;
  };

  /**
   * @ngdoc function
   * @name pascalprecht.translate.$translateProvider#uniformLanguageTag
   * @methodOf pascalprecht.translate.$translateProvider
   *
   * @description
   * Tells angular-translate which language tag should be used as a result when determining
   * the current browser language.
   *
   * This setting must be set before invoking {@link pascalprecht.translate.$translateProvider#methods_determinePreferredLanguage determinePreferredLanguage()}.
   *
   * <pre>
   * $translateProvider
   *   .uniformLanguageTag('bcp47')
   *   .determinePreferredLanguage()
   * </pre>
   *
   * The resolver currently supports:
   * * default
   *     (traditionally: hyphens will be converted into underscores, i.e. en-US => en_US)
   *     en-US => en_US
   *     en_US => en_US
   *     en-us => en_us
   * * java
   *     like default, but the second part will be always in uppercase
   *     en-US => en_US
   *     en_US => en_US
   *     en-us => en_US
   * * BCP 47 (RFC 4646 & 4647)
   *     en-US => en-US
   *     en_US => en-US
   *     en-us => en-US
   *
   * See also:
   * * http://en.wikipedia.org/wiki/IETF_language_tag
   * * http://www.w3.org/International/core/langtags/
   * * http://tools.ietf.org/html/bcp47
   *
   * @param {string|object} options - options (or standard)
   * @param {string} options.standard - valid values are 'default', 'bcp47', 'java'
   */
  this.uniformLanguageTag = function (options) {

    if (!options) {
      options = {};
    } else if (angular.isString(options)) {
      options = {
        standard : options
      };
    }

    uniformLanguageTagResolver = options.standard;

    return this;
  };

  /**
   * @ngdoc function
   * @name pascalprecht.translate.$translateProvider#determinePreferredLanguage
   * @methodOf pascalprecht.translate.$translateProvider
   *
   * @description
   * Tells angular-translate to try to determine on its own which language key
   * to set as preferred language. When `fn` is given, angular-translate uses it
   * to determine a language key, otherwise it uses the built-in `getLocale()`
   * method.
   *
   * The `getLocale()` returns a language key in the format `[lang]_[country]` or
   * `[lang]` depending on what the browser provides.
   *
   * Use this method at your own risk, since not all browsers return a valid
   * locale (see {@link pascalprecht.translate.$translateProvider#methods_uniformLanguageTag uniformLanguageTag()}).
   *
   * @param {Function=} fn Function to determine a browser's locale
   */
  this.determinePreferredLanguage = function (fn) {

    var locale = (fn && angular.isFunction(fn)) ? fn() : getLocale();

    if (!$availableLanguageKeys.length) {
      $preferredLanguage = locale;
    } else {
      $preferredLanguage = negotiateLocale(locale) || locale;
    }

    return this;
  };

  /**
   * @ngdoc function
   * @name pascalprecht.translate.$translateProvider#registerAvailableLanguageKeys
   * @methodOf pascalprecht.translate.$translateProvider
   *
   * @description
   * Registers a set of language keys the app will work with. Use this method in
   * combination with
   * {@link pascalprecht.translate.$translateProvider#determinePreferredLanguage determinePreferredLanguage}.
   * When available languages keys are registered, angular-translate
   * tries to find the best fitting language key depending on the browsers locale,
   * considering your language key convention.
   *
   * @param {object} languageKeys Array of language keys the your app will use
   * @param {object=} aliases Alias map.
   */
  this.registerAvailableLanguageKeys = function (languageKeys, aliases) {
    if (languageKeys) {
      $availableLanguageKeys = languageKeys;
      if (aliases) {
        $languageKeyAliases = aliases;
      }
      return this;
    }
    return $availableLanguageKeys;
  };

  /**
   * @ngdoc function
   * @name pascalprecht.translate.$translateProvider#useLoaderCache
   * @methodOf pascalprecht.translate.$translateProvider
   *
   * @description
   * Registers a cache for internal $http based loaders.
   * {@link pascalprecht.translate.$translationCache $translationCache}.
   * When false the cache will be disabled (default). When true or undefined
   * the cache will be a default (see $cacheFactory). When an object it will
   * be treat as a cache object itself: the usage is $http({cache: cache})
   *
   * @param {object} cache boolean, string or cache-object
   */
  this.useLoaderCache = function (cache) {
    if (cache === false) {
      // disable cache
      loaderCache = undefined;
    } else if (cache === true) {
      // enable cache using AJS defaults
      loaderCache = true;
    } else if (typeof(cache) === 'undefined') {
      // enable cache using default
      loaderCache = '$translationCache';
    } else if (cache) {
      // enable cache using given one (see $cacheFactory)
      loaderCache = cache;
    }
    return this;
  };

  /**
   * @ngdoc function
   * @name pascalprecht.translate.$translateProvider#directivePriority
   * @methodOf pascalprecht.translate.$translateProvider
   *
   * @description
   * Sets the default priority of the translate directive. The standard value is `0`.
   * Calling this function without an argument will return the current value.
   *
   * @param {number} priority for the translate-directive
   */
  this.directivePriority = function (priority) {
    if (priority === undefined) {
      // getter
      return directivePriority;
    } else {
      // setter with chaining
      directivePriority = priority;
      return this;
    }
  };

  /**
   * @ngdoc function
   * @name pascalprecht.translate.$translateProvider#statefulFilter
   * @methodOf pascalprecht.translate.$translateProvider
   *
   * @description
   * Since AngularJS 1.3, filters which are not stateless (depending at the scope)
   * have to explicit define this behavior.
   * Sets whether the translate filter should be stateful or stateless. The standard value is `true`
   * meaning being stateful.
   * Calling this function without an argument will return the current value.
   *
   * @param {boolean} state - defines the state of the filter
   */
  this.statefulFilter = function (state) {
    if (state === undefined) {
      // getter
      return statefulFilter;
    } else {
      // setter with chaining
      statefulFilter = state;
      return this;
    }
  };

  /**
   * @ngdoc function
   * @name pascalprecht.translate.$translateProvider#postProcess
   * @methodOf pascalprecht.translate.$translateProvider
   *
   * @description
   * The post processor will be intercept right after the translation result. It can modify the result.
   *
   * @param {object} fn Function or service name (string) to be called after the translation value has been set / resolved. The function itself will enrich every value being processed and then continue the normal resolver process
   */
  this.postProcess = function (fn) {
    if (fn) {
      postProcessFn = fn;
    } else {
      postProcessFn = undefined;
    }
    return this;
  };

  /**
   * @ngdoc function
   * @name pascalprecht.translate.$translateProvider#keepContent
   * @methodOf pascalprecht.translate.$translateProvider
   *
   * @description
   * If keepContent is set to true than translate directive will always use innerHTML
   * as a default translation
   *
   * Example:
   * <pre>
   *  app.config(function ($translateProvider) {
   *    $translateProvider.keepContent(true);
   *  });
   * </pre>
   *
   * @param {boolean} value - valid values are true or false
   */
  this.keepContent = function (value) {
    $keepContent = !(!value);
    return this;
  };

  /**
   * @ngdoc object
   * @name pascalprecht.translate.$translate
   * @requires $interpolate
   * @requires $log
   * @requires $rootScope
   * @requires $q
   *
   * @description
   * The `$translate` service is the actual core of angular-translate. It expects a translation id
   * and optional interpolate parameters to translate contents.
   *
   * <pre>
   *  $translate('HEADLINE_TEXT').then(function (translation) {
   *    $scope.translatedText = translation;
   *  });
   * </pre>
   *
   * @param {string|array} translationId A token which represents a translation id
   *                                     This can be optionally an array of translation ids which
   *                                     results that the function returns an object where each key
   *                                     is the translation id and the value the translation.
   * @param {object=} interpolateParams An object hash for dynamic values
   * @param {string} interpolationId The id of the interpolation to use
   * @param {string} defaultTranslationText the optional default translation text that is written as
   *                                        as default text in case it is not found in any configured language
   * @param {string} forceLanguage A language to be used instead of the current language
   * @returns {object} promise
   */
  this.$get = ['$log', '$injector', '$rootScope', '$q', function ($log, $injector, $rootScope, $q) {

    var Storage,
      defaultInterpolator = $injector.get($interpolationFactory || '$translateDefaultInterpolation'),
      pendingLoader = false,
      interpolatorHashMap = {},
      langPromises = {},
      fallbackIndex,
      startFallbackIteration;

    var $translate = function (translationId, interpolateParams, interpolationId, defaultTranslationText, forceLanguage) {
      if (!$uses && $preferredLanguage) {
        $uses = $preferredLanguage;
      }
      var uses = (forceLanguage && forceLanguage !== $uses) ? // we don't want to re-negotiate $uses
        (negotiateLocale(forceLanguage) || forceLanguage) : $uses;

      // Check forceLanguage is present
      if (forceLanguage) {
        loadTranslationsIfMissing(forceLanguage);
      }

      // Duck detection: If the first argument is an array, a bunch of translations was requested.
      // The result is an object.
      if (angular.isArray(translationId)) {
        // Inspired by Q.allSettled by Kris Kowal
        // https://github.com/kriskowal/q/blob/b0fa72980717dc202ffc3cbf03b936e10ebbb9d7/q.js#L1553-1563
        // This transforms all promises regardless resolved or rejected
        var translateAll = function (translationIds) {
          var results = {}; // storing the actual results
          var promises = []; // promises to wait for
          // Wraps the promise a) being always resolved and b) storing the link id->value
          var translate = function (translationId) {
            var deferred = $q.defer();
            var regardless = function (value) {
              results[translationId] = value;
              deferred.resolve([translationId, value]);
            };
            // we don't care whether the promise was resolved or rejected; just store the values
            $translate(translationId, interpolateParams, interpolationId, defaultTranslationText, forceLanguage).then(regardless, regardless);
            return deferred.promise;
          };
          for (var i = 0, c = translationIds.length; i < c; i++) {
            promises.push(translate(translationIds[i]));
          }
          // wait for all (including storing to results)
          return $q.all(promises).then(function () {
            // return the results
            return results;
          });
        };
        return translateAll(translationId);
      }

      var deferred = $q.defer();

      // trim off any whitespace
      if (translationId) {
        translationId = trim.apply(translationId);
      }

      var promiseToWaitFor = (function () {
        var promise = $preferredLanguage ?
          langPromises[$preferredLanguage] :
          langPromises[uses];

        fallbackIndex = 0;

        if ($storageFactory && !promise) {
          // looks like there's no pending promise for $preferredLanguage or
          // $uses. Maybe there's one pending for a language that comes from
          // storage.
          var langKey = Storage.get($storageKey);
          promise = langPromises[langKey];

          if ($fallbackLanguage && $fallbackLanguage.length) {
            var index = indexOf($fallbackLanguage, langKey);
            // maybe the language from storage is also defined as fallback language
            // we increase the fallback language index to not search in that language
            // as fallback, since it's probably the first used language
            // in that case the index starts after the first element
            fallbackIndex = (index === 0) ? 1 : 0;

            // but we can make sure to ALWAYS fallback to preferred language at least
            if (indexOf($fallbackLanguage, $preferredLanguage) < 0) {
              $fallbackLanguage.push($preferredLanguage);
            }
          }
        }
        return promise;
      }());

      if (!promiseToWaitFor) {
        // no promise to wait for? okay. Then there's no loader registered
        // nor is a one pending for language that comes from storage.
        // We can just translate.
        determineTranslation(translationId, interpolateParams, interpolationId, defaultTranslationText, uses).then(deferred.resolve, deferred.reject);
      } else {
        var promiseResolved = function () {
          // $uses may have changed while waiting
          if (!forceLanguage) {
            uses = $uses;
          }
          determineTranslation(translationId, interpolateParams, interpolationId, defaultTranslationText, uses).then(deferred.resolve, deferred.reject);
        };
        promiseResolved.displayName = 'promiseResolved';

        promiseToWaitFor['finally'](promiseResolved)
          .catch(angular.noop); // we don't care about errors here, already handled
      }
      return deferred.promise;
    };

    /**
     * @name applyNotFoundIndicators
     * @private
     *
     * @description
     * Applies not fount indicators to given translation id, if needed.
     * This function gets only executed, if a translation id doesn't exist,
     * which is why a translation id is expected as argument.
     *
     * @param {string} translationId Translation id.
     * @returns {string} Same as given translation id but applied with not found
     * indicators.
     */
    var applyNotFoundIndicators = function (translationId) {
      // applying notFoundIndicators
      if ($notFoundIndicatorLeft) {
        translationId = [$notFoundIndicatorLeft, translationId].join(' ');
      }
      if ($notFoundIndicatorRight) {
        translationId = [translationId, $notFoundIndicatorRight].join(' ');
      }
      return translationId;
    };

    /**
     * @name useLanguage
     * @private
     *
     * @description
     * Makes actual use of a language by setting a given language key as used
     * language and informs registered interpolators to also use the given
     * key as locale.
     *
     * @param {string} key Locale key.
     */
    var useLanguage = function (key) {
      $uses = key;

      // make sure to store new language key before triggering success event
      if ($storageFactory) {
        Storage.put($translate.storageKey(), $uses);
      }

      $rootScope.$emit('$translateChangeSuccess', {language : key});

      // inform default interpolator
      defaultInterpolator.setLocale($uses);

      var eachInterpolator = function (interpolator, id) {
        interpolatorHashMap[id].setLocale($uses);
      };
      eachInterpolator.displayName = 'eachInterpolatorLocaleSetter';

      // inform all others too!
      angular.forEach(interpolatorHashMap, eachInterpolator);
      $rootScope.$emit('$translateChangeEnd', {language : key});
    };

    /**
     * @name loadAsync
     * @private
     *
     * @description
     * Kicks off registered async loader using `$injector` and applies existing
     * loader options. When resolved, it updates translation tables accordingly
     * or rejects with given language key.
     *
     * @param {string} key Language key.
     * @return {Promise} A promise.
     */
    var loadAsync = function (key) {
      if (!key) {
        throw 'No language key specified for loading.';
      }

      var deferred = $q.defer();

      $rootScope.$emit('$translateLoadingStart', {language : key});
      pendingLoader = true;

      var cache = loaderCache;
      if (typeof(cache) === 'string') {
        // getting on-demand instance of loader
        cache = $injector.get(cache);
      }

      var loaderOptions = angular.extend({}, $loaderOptions, {
        key : key,
        $http : angular.extend({}, {
          cache : cache
        }, $loaderOptions.$http)
      });

      var onLoaderSuccess = function (data) {
        var translationTable = {};
        $rootScope.$emit('$translateLoadingSuccess', {language : key});

        if (angular.isArray(data)) {
          angular.forEach(data, function (table) {
            angular.extend(translationTable, flatObject(table));
          });
        } else {
          angular.extend(translationTable, flatObject(data));
        }
        pendingLoader = false;
        deferred.resolve({
          key : key,
          table : translationTable
        });
        $rootScope.$emit('$translateLoadingEnd', {language : key});
      };
      onLoaderSuccess.displayName = 'onLoaderSuccess';

      var onLoaderError = function (key) {
        $rootScope.$emit('$translateLoadingError', {language : key});
        deferred.reject(key);
        $rootScope.$emit('$translateLoadingEnd', {language : key});
      };
      onLoaderError.displayName = 'onLoaderError';

      $injector.get($loaderFactory)(loaderOptions)
        .then(onLoaderSuccess, onLoaderError);

      return deferred.promise;
    };

    if ($storageFactory) {
      Storage = $injector.get($storageFactory);

      if (!Storage.get || !Storage.put) {
        throw new Error('Couldn\'t use storage \'' + $storageFactory + '\', missing get() or put() method!');
      }
    }

    // if we have additional interpolations that were added via
    // $translateProvider.addInterpolation(), we have to map'em
    if ($interpolatorFactories.length) {
      var eachInterpolationFactory = function (interpolatorFactory) {
        var interpolator = $injector.get(interpolatorFactory);
        // setting initial locale for each interpolation service
        interpolator.setLocale($preferredLanguage || $uses);
        // make'em recognizable through id
        interpolatorHashMap[interpolator.getInterpolationIdentifier()] = interpolator;
      };
      eachInterpolationFactory.displayName = 'interpolationFactoryAdder';

      angular.forEach($interpolatorFactories, eachInterpolationFactory);
    }

    /**
     * @name getTranslationTable
     * @private
     *
     * @description
     * Returns a promise that resolves to the translation table
     * or is rejected if an error occurred.
     *
     * @param langKey
     * @returns {Q.promise}
     */
    var getTranslationTable = function (langKey) {
      var deferred = $q.defer();
      if (Object.prototype.hasOwnProperty.call($translationTable, langKey)) {
        deferred.resolve($translationTable[langKey]);
      } else if (langPromises[langKey]) {
        var onResolve = function (data) {
          translations(data.key, data.table);
          deferred.resolve(data.table);
        };
        onResolve.displayName = 'translationTableResolver';
        langPromises[langKey].then(onResolve, deferred.reject);
      } else {
        deferred.reject();
      }
      return deferred.promise;
    };

    /**
     * @name getFallbackTranslation
     * @private
     *
     * @description
     * Returns a promise that will resolve to the translation
     * or be rejected if no translation was found for the language.
     * This function is currently only used for fallback language translation.
     *
     * @param langKey The language to translate to.
     * @param translationId
     * @param interpolateParams
     * @param Interpolator
     * @param sanitizeStrategy
     * @returns {Q.promise}
     */
    var getFallbackTranslation = function (langKey, translationId, interpolateParams, Interpolator, sanitizeStrategy) {
      var deferred = $q.defer();

      var onResolve = function (translationTable) {
        if (Object.prototype.hasOwnProperty.call(translationTable, translationId) && translationTable[translationId] !== null) {
          Interpolator.setLocale(langKey);
          var translation = translationTable[translationId];
          if (translation.substr(0, 2) === '@:') {
            getFallbackTranslation(langKey, translation.substr(2), interpolateParams, Interpolator, sanitizeStrategy)
              .then(deferred.resolve, deferred.reject);
          } else {
            var interpolatedValue = Interpolator.interpolate(translationTable[translationId], interpolateParams, 'service', sanitizeStrategy, translationId);
            interpolatedValue = applyPostProcessing(translationId, translationTable[translationId], interpolatedValue, interpolateParams, langKey);

            deferred.resolve(interpolatedValue);

          }
          Interpolator.setLocale($uses);
        } else {
          deferred.reject();
        }
      };
      onResolve.displayName = 'fallbackTranslationResolver';

      getTranslationTable(langKey).then(onResolve, deferred.reject);

      return deferred.promise;
    };

    /**
     * @name getFallbackTranslationInstant
     * @private
     *
     * @description
     * Returns a translation
     * This function is currently only used for fallback language translation.
     *
     * @param langKey The language to translate to.
     * @param translationId
     * @param interpolateParams
     * @param Interpolator
     * @param sanitizeStrategy sanitize strategy override
     *
     * @returns {string} translation
     */
    var getFallbackTranslationInstant = function (langKey, translationId, interpolateParams, Interpolator, sanitizeStrategy) {
      var result, translationTable = $translationTable[langKey];

      if (translationTable && Object.prototype.hasOwnProperty.call(translationTable, translationId) && translationTable[translationId] !== null) {
        Interpolator.setLocale(langKey);
        result = Interpolator.interpolate(translationTable[translationId], interpolateParams, 'filter', sanitizeStrategy, translationId);
        result = applyPostProcessing(translationId, translationTable[translationId], result, interpolateParams, langKey, sanitizeStrategy);
        // workaround for TrustedValueHolderType
        if (!angular.isString(result) && angular.isFunction(result.$$unwrapTrustedValue)) {
          var result2 = result.$$unwrapTrustedValue();
          if (result2.substr(0, 2) === '@:') {
            return getFallbackTranslationInstant(langKey, result2.substr(2), interpolateParams, Interpolator, sanitizeStrategy);
          }
        } else if (result.substr(0, 2) === '@:') {
          return getFallbackTranslationInstant(langKey, result.substr(2), interpolateParams, Interpolator, sanitizeStrategy);
        }
        Interpolator.setLocale($uses);
      }

      return result;
    };


    /**
     * @name translateByHandler
     * @private
     *
     * Translate by missing translation handler.
     *
     * @param translationId
     * @param interpolateParams
     * @param defaultTranslationText
     * @param sanitizeStrategy sanitize strategy override
     *
     * @returns translation created by $missingTranslationHandler or translationId is $missingTranslationHandler is
     * absent
     */
    var translateByHandler = function (translationId, interpolateParams, defaultTranslationText, sanitizeStrategy) {
      // If we have a handler factory - we might also call it here to determine if it provides
      // a default text for a translationid that can't be found anywhere in our tables
      if ($missingTranslationHandlerFactory) {
        return $injector.get($missingTranslationHandlerFactory)(translationId, $uses, interpolateParams, defaultTranslationText, sanitizeStrategy);
      } else {
        return translationId;
      }
    };

    /**
     * @name resolveForFallbackLanguage
     * @private
     *
     * Recursive helper function for fallbackTranslation that will sequentially look
     * for a translation in the fallbackLanguages starting with fallbackLanguageIndex.
     *
     * @param fallbackLanguageIndex
     * @param translationId
     * @param interpolateParams
     * @param Interpolator
     * @param defaultTranslationText
     * @param sanitizeStrategy
     * @returns {Q.promise} Promise that will resolve to the translation.
     */
    var resolveForFallbackLanguage = function (fallbackLanguageIndex, translationId, interpolateParams, Interpolator, defaultTranslationText, sanitizeStrategy) {
      var deferred = $q.defer();

      if (fallbackLanguageIndex < $fallbackLanguage.length) {
        var langKey = $fallbackLanguage[fallbackLanguageIndex];
        getFallbackTranslation(langKey, translationId, interpolateParams, Interpolator, sanitizeStrategy).then(
          function (data) {
            deferred.resolve(data);
          },
          function () {
            // Look in the next fallback language for a translation.
            // It delays the resolving by passing another promise to resolve.
            return resolveForFallbackLanguage(fallbackLanguageIndex + 1, translationId, interpolateParams, Interpolator, defaultTranslationText, sanitizeStrategy).then(deferred.resolve, deferred.reject);
          }
        );
      } else {
        // No translation found in any fallback language
        // if a default translation text is set in the directive, then return this as a result
        if (defaultTranslationText) {
          deferred.resolve(defaultTranslationText);
        } else {
          var missingTranslationHandlerTranslation = translateByHandler(translationId, interpolateParams, defaultTranslationText);

          // if no default translation is set and an error handler is defined, send it to the handler
          // and then return the result if it isn't undefined
          if ($missingTranslationHandlerFactory && missingTranslationHandlerTranslation) {
            deferred.resolve(missingTranslationHandlerTranslation);
          } else {
            deferred.reject(applyNotFoundIndicators(translationId));
          }
        }
      }
      return deferred.promise;
    };

    /**
     * @name resolveForFallbackLanguageInstant
     * @private
     *
     * Recursive helper function for fallbackTranslation that will sequentially look
     * for a translation in the fallbackLanguages starting with fallbackLanguageIndex.
     *
     * @param fallbackLanguageIndex
     * @param translationId
     * @param interpolateParams
     * @param Interpolator
     * @param sanitizeStrategy
     * @returns {string} translation
     */
    var resolveForFallbackLanguageInstant = function (fallbackLanguageIndex, translationId, interpolateParams, Interpolator, sanitizeStrategy) {
      var result;

      if (fallbackLanguageIndex < $fallbackLanguage.length) {
        var langKey = $fallbackLanguage[fallbackLanguageIndex];
        result = getFallbackTranslationInstant(langKey, translationId, interpolateParams, Interpolator, sanitizeStrategy);
        if (!result && result !== '') {
          result = resolveForFallbackLanguageInstant(fallbackLanguageIndex + 1, translationId, interpolateParams, Interpolator);
        }
      }
      return result;
    };

    /**
     * Translates with the usage of the fallback languages.
     *
     * @param translationId
     * @param interpolateParams
     * @param Interpolator
     * @param defaultTranslationText
     * @param sanitizeStrategy
     * @returns {Q.promise} Promise, that resolves to the translation.
     */
    var fallbackTranslation = function (translationId, interpolateParams, Interpolator, defaultTranslationText, sanitizeStrategy) {
      // Start with the fallbackLanguage with index 0
      return resolveForFallbackLanguage((startFallbackIteration > 0 ? startFallbackIteration : fallbackIndex), translationId, interpolateParams, Interpolator, defaultTranslationText, sanitizeStrategy);
    };

    /**
     * Translates with the usage of the fallback languages.
     *
     * @param translationId
     * @param interpolateParams
     * @param Interpolator
     * @param sanitizeStrategy
     * @returns {String} translation
     */
    var fallbackTranslationInstant = function (translationId, interpolateParams, Interpolator, sanitizeStrategy) {
      // Start with the fallbackLanguage with index 0
      return resolveForFallbackLanguageInstant((startFallbackIteration > 0 ? startFallbackIteration : fallbackIndex), translationId, interpolateParams, Interpolator, sanitizeStrategy);
    };

    var determineTranslation = function (translationId, interpolateParams, interpolationId, defaultTranslationText, uses, sanitizeStrategy) {

      var deferred = $q.defer();

      var table = uses ? $translationTable[uses] : $translationTable,
        Interpolator = (interpolationId) ? interpolatorHashMap[interpolationId] : defaultInterpolator;

      // if the translation id exists, we can just interpolate it
      if (table && Object.prototype.hasOwnProperty.call(table, translationId) && table[translationId] !== null) {
        var translation = table[translationId];

        // If using link, rerun $translate with linked translationId and return it
        if (translation.substr(0, 2) === '@:') {

          $translate(translation.substr(2), interpolateParams, interpolationId, defaultTranslationText, uses)
            .then(deferred.resolve, deferred.reject);
        } else {
          //
          var resolvedTranslation = Interpolator.interpolate(translation, interpolateParams, 'service', sanitizeStrategy, translationId);
          resolvedTranslation = applyPostProcessing(translationId, translation, resolvedTranslation, interpolateParams, uses);
          deferred.resolve(resolvedTranslation);
        }
      } else {
        var missingTranslationHandlerTranslation;
        // for logging purposes only (as in $translateMissingTranslationHandlerLog), value is not returned to promise
        if ($missingTranslationHandlerFactory && !pendingLoader) {
          missingTranslationHandlerTranslation = translateByHandler(translationId, interpolateParams, defaultTranslationText);
        }

        // since we couldn't translate the inital requested translation id,
        // we try it now with one or more fallback languages, if fallback language(s) is
        // configured.
        if (uses && $fallbackLanguage && $fallbackLanguage.length) {
          fallbackTranslation(translationId, interpolateParams, Interpolator, defaultTranslationText, sanitizeStrategy)
            .then(function (translation) {
              deferred.resolve(translation);
            }, function (_translationId) {
              deferred.reject(applyNotFoundIndicators(_translationId));
            });
        } else if ($missingTranslationHandlerFactory && !pendingLoader && missingTranslationHandlerTranslation) {
          // looks like the requested translation id doesn't exists.
          // Now, if there is a registered handler for missing translations and no
          // asyncLoader is pending, we execute the handler
          if (defaultTranslationText) {
            deferred.resolve(defaultTranslationText);
          } else {
            deferred.resolve(missingTranslationHandlerTranslation);
          }
        } else {
          if (defaultTranslationText) {
            deferred.resolve(defaultTranslationText);
          } else {
            deferred.reject(applyNotFoundIndicators(translationId));
          }
        }
      }
      return deferred.promise;
    };

    var determineTranslationInstant = function (translationId, interpolateParams, interpolationId, uses, sanitizeStrategy) {

      var result, table = uses ? $translationTable[uses] : $translationTable,
        Interpolator = defaultInterpolator;

      // if the interpolation id exists use custom interpolator
      if (interpolatorHashMap && Object.prototype.hasOwnProperty.call(interpolatorHashMap, interpolationId)) {
        Interpolator = interpolatorHashMap[interpolationId];
      }

      // if the translation id exists, we can just interpolate it
      if (table && Object.prototype.hasOwnProperty.call(table, translationId) && table[translationId] !== null) {
        var translation = table[translationId];

        // If using link, rerun $translate with linked translationId and return it
        if (translation.substr(0, 2) === '@:') {
          result = determineTranslationInstant(translation.substr(2), interpolateParams, interpolationId, uses, sanitizeStrategy);
        } else {
          result = Interpolator.interpolate(translation, interpolateParams, 'filter', sanitizeStrategy, translationId);
          result = applyPostProcessing(translationId, translation, result, interpolateParams, uses, sanitizeStrategy);
        }
      } else {
        var missingTranslationHandlerTranslation;
        // for logging purposes only (as in $translateMissingTranslationHandlerLog), value is not returned to promise
        if ($missingTranslationHandlerFactory && !pendingLoader) {
          missingTranslationHandlerTranslation = translateByHandler(translationId, interpolateParams, sanitizeStrategy);
        }

        // since we couldn't translate the inital requested translation id,
        // we try it now with one or more fallback languages, if fallback language(s) is
        // configured.
        if (uses && $fallbackLanguage && $fallbackLanguage.length) {
          fallbackIndex = 0;
          result = fallbackTranslationInstant(translationId, interpolateParams, Interpolator, sanitizeStrategy);
        } else if ($missingTranslationHandlerFactory && !pendingLoader && missingTranslationHandlerTranslation) {
          // looks like the requested translation id doesn't exists.
          // Now, if there is a registered handler for missing translations and no
          // asyncLoader is pending, we execute the handler
          result = missingTranslationHandlerTranslation;
        } else {
          result = applyNotFoundIndicators(translationId);
        }
      }

      return result;
    };

    var clearNextLangAndPromise = function (key) {
      if ($nextLang === key) {
        $nextLang = undefined;
      }
      langPromises[key] = undefined;
    };

    var applyPostProcessing = function (translationId, translation, resolvedTranslation, interpolateParams, uses, sanitizeStrategy) {
      var fn = postProcessFn;

      if (fn) {

        if (typeof(fn) === 'string') {
          // getting on-demand instance
          fn = $injector.get(fn);
        }
        if (fn) {
          return fn(translationId, translation, resolvedTranslation, interpolateParams, uses, sanitizeStrategy);
        }
      }

      return resolvedTranslation;
    };

    var loadTranslationsIfMissing = function (key) {
      if (!$translationTable[key] && $loaderFactory && !langPromises[key]) {
        langPromises[key] = loadAsync(key).then(function (translation) {
          translations(translation.key, translation.table);
          return translation;
        });
      }
    };

    /**
     * @ngdoc function
     * @name pascalprecht.translate.$translate#preferredLanguage
     * @methodOf pascalprecht.translate.$translate
     *
     * @description
     * Returns the language key for the preferred language.
     *
     * @param {string} langKey language String or Array to be used as preferredLanguage (changing at runtime)
     *
     * @return {string} preferred language key
     */
    $translate.preferredLanguage = function (langKey) {
      if (langKey) {
        setupPreferredLanguage(langKey);
      }
      return $preferredLanguage;
    };

    /**
     * @ngdoc function
     * @name pascalprecht.translate.$translate#cloakClassName
     * @methodOf pascalprecht.translate.$translate
     *
     * @description
     * Returns the configured class name for `translate-cloak` directive.
     *
     * @return {string} cloakClassName
     */
    $translate.cloakClassName = function () {
      return $cloakClassName;
    };

    /**
     * @ngdoc function
     * @name pascalprecht.translate.$translate#nestedObjectDelimeter
     * @methodOf pascalprecht.translate.$translate
     *
     * @description
     * Returns the configured delimiter for nested namespaces.
     *
     * @return {string} nestedObjectDelimeter
     */
    $translate.nestedObjectDelimeter = function () {
      return $nestedObjectDelimeter;
    };

    /**
     * @ngdoc function
     * @name pascalprecht.translate.$translate#fallbackLanguage
     * @methodOf pascalprecht.translate.$translate
     *
     * @description
     * Returns the language key for the fallback languages or sets a new fallback stack.
     *
     * @param {string=} langKey language String or Array of fallback languages to be used (to change stack at runtime)
     *
     * @return {string||array} fallback language key
     */
    $translate.fallbackLanguage = function (langKey) {
      if (langKey !== undefined && langKey !== null) {
        fallbackStack(langKey);

        // as we might have an async loader initiated and a new translation language might have been defined
        // we need to add the promise to the stack also. So - iterate.
        if ($loaderFactory) {
          if ($fallbackLanguage && $fallbackLanguage.length) {
            for (var i = 0, len = $fallbackLanguage.length; i < len; i++) {
              if (!langPromises[$fallbackLanguage[i]]) {
                langPromises[$fallbackLanguage[i]] = loadAsync($fallbackLanguage[i]);
              }
            }
          }
        }
        $translate.use($translate.use());
      }
      if ($fallbackWasString) {
        return $fallbackLanguage[0];
      } else {
        return $fallbackLanguage;
      }

    };

    /**
     * @ngdoc function
     * @name pascalprecht.translate.$translate#useFallbackLanguage
     * @methodOf pascalprecht.translate.$translate
     *
     * @description
     * Sets the first key of the fallback language stack to be used for translation.
     * Therefore all languages in the fallback array BEFORE this key will be skipped!
     *
     * @param {string=} langKey Contains the langKey the iteration shall start with. Set to false if you want to
     * get back to the whole stack
     */
    $translate.useFallbackLanguage = function (langKey) {
      if (langKey !== undefined && langKey !== null) {
        if (!langKey) {
          startFallbackIteration = 0;
        } else {
          var langKeyPosition = indexOf($fallbackLanguage, langKey);
          if (langKeyPosition > -1) {
            startFallbackIteration = langKeyPosition;
          }
        }

      }

    };

    /**
     * @ngdoc function
     * @name pascalprecht.translate.$translate#proposedLanguage
     * @methodOf pascalprecht.translate.$translate
     *
     * @description
     * Returns the language key of language that is currently loaded asynchronously.
     *
     * @return {string} language key
     */
    $translate.proposedLanguage = function () {
      return $nextLang;
    };

    /**
     * @ngdoc function
     * @name pascalprecht.translate.$translate#storage
     * @methodOf pascalprecht.translate.$translate
     *
     * @description
     * Returns registered storage.
     *
     * @return {object} Storage
     */
    $translate.storage = function () {
      return Storage;
    };

    /**
     * @ngdoc function
     * @name pascalprecht.translate.$translate#negotiateLocale
     * @methodOf pascalprecht.translate.$translate
     *
     * @description
     * Returns a language key based on available languages and language aliases. If a
     * language key cannot be resolved, returns undefined.
     *
     * If no or a falsy key is given, returns undefined.
     *
     * @param {string} [key] Language key
     * @return {string|undefined} Language key or undefined if no language key is found.
     */
    $translate.negotiateLocale = negotiateLocale;

    /**
     * @ngdoc function
     * @name pascalprecht.translate.$translate#use
     * @methodOf pascalprecht.translate.$translate
     *
     * @description
     * Tells angular-translate which language to use by given language key. This method is
     * used to change language at runtime. It also takes care of storing the language
     * key in a configured store to let your app remember the choosed language.
     *
     * When trying to 'use' a language which isn't available it tries to load it
     * asynchronously with registered loaders.
     *
     * Returns promise object with loaded language file data or string of the currently used language.
     *
     * If no or a falsy key is given it returns the currently used language key.
     * The returned string will be ```undefined``` if setting up $translate hasn't finished.
     * @example
     * $translate.use("en_US").then(function(data){
       *   $scope.text = $translate("HELLO");
       * });
     *
     * @param {string} [key] Language key
     * @return {object|string} Promise with loaded language data or the language key if a falsy param was given.
     */
    $translate.use = function (key) {
      if (!key) {
        return $uses;
      }

      var deferred = $q.defer();
      deferred.promise.then(null, angular.noop); // AJS "Possibly unhandled rejection"

      $rootScope.$emit('$translateChangeStart', {language : key});

      // Try to get the aliased language key
      var aliasedKey = negotiateLocale(key);
      // Ensure only registered language keys will be loaded
      if ($availableLanguageKeys.length > 0 && !aliasedKey) {
        return $q.reject(key);
      }

      if (aliasedKey) {
        key = aliasedKey;
      }

      // if there isn't a translation table for the language we've requested,
      // we load it asynchronously
      $nextLang = key;
      if (($forceAsyncReloadEnabled || !$translationTable[key]) && $loaderFactory && !langPromises[key]) {
        langPromises[key] = loadAsync(key).then(function (translation) {
          translations(translation.key, translation.table);
          deferred.resolve(translation.key);
          if ($nextLang === key) {
            useLanguage(translation.key);
          }
          return translation;
        }, function (key) {
          $rootScope.$emit('$translateChangeError', {language : key});
          deferred.reject(key);
          $rootScope.$emit('$translateChangeEnd', {language : key});
          return $q.reject(key);
        });
        langPromises[key]['finally'](function () {
          clearNextLangAndPromise(key);
        }).catch(angular.noop); // we don't care about errors (clearing)
      } else if (langPromises[key]) {
        // we are already loading this asynchronously
        // resolve our new deferred when the old langPromise is resolved
        langPromises[key].then(function (translation) {
          if ($nextLang === translation.key) {
            useLanguage(translation.key);
          }
          deferred.resolve(translation.key);
          return translation;
        }, function (key) {
          // find first available fallback language if that request has failed
          if (!$uses && $fallbackLanguage && $fallbackLanguage.length > 0 && $fallbackLanguage[0] !== key) {
            return $translate.use($fallbackLanguage[0]).then(deferred.resolve, deferred.reject);
          } else {
            return deferred.reject(key);
          }
        });
      } else {
        deferred.resolve(key);
        useLanguage(key);
      }

      return deferred.promise;
    };

    /**
     * @ngdoc function
     * @name pascalprecht.translate.$translate#resolveClientLocale
     * @methodOf pascalprecht.translate.$translate
     *
     * @description
     * This returns the current browser/client's language key. The result is processed with the configured uniform tag resolver.
     *
     * @returns {string} the current client/browser language key
     */
    $translate.resolveClientLocale = function () {
      return getLocale();
    };

    /**
     * @ngdoc function
     * @name pascalprecht.translate.$translate#storageKey
     * @methodOf pascalprecht.translate.$translate
     *
     * @description
     * Returns the key for the storage.
     *
     * @return {string} storage key
     */
    $translate.storageKey = function () {
      return storageKey();
    };

    /**
     * @ngdoc function
     * @name pascalprecht.translate.$translate#isPostCompilingEnabled
     * @methodOf pascalprecht.translate.$translate
     *
     * @description
     * Returns whether post compiling is enabled or not
     *
     * @return {bool} storage key
     */
    $translate.isPostCompilingEnabled = function () {
      return $postCompilingEnabled;
    };

    /**
     * @ngdoc function
     * @name pascalprecht.translate.$translate#isForceAsyncReloadEnabled
     * @methodOf pascalprecht.translate.$translate
     *
     * @description
     * Returns whether force async reload is enabled or not
     *
     * @return {boolean} forceAsyncReload value
     */
    $translate.isForceAsyncReloadEnabled = function () {
      return $forceAsyncReloadEnabled;
    };

    /**
     * @ngdoc function
     * @name pascalprecht.translate.$translate#isKeepContent
     * @methodOf pascalprecht.translate.$translate
     *
     * @description
     * Returns whether keepContent or not
     *
     * @return {boolean} keepContent value
     */
    $translate.isKeepContent = function () {
      return $keepContent;
    };

    /**
     * @ngdoc function
     * @name pascalprecht.translate.$translate#refresh
     * @methodOf pascalprecht.translate.$translate
     *
     * @description
     * Refreshes a translation table pointed by the given langKey. If langKey is not specified,
     * the module will drop all existent translation tables and load new version of those which
     * are currently in use.
     *
     * Refresh means that the module will drop target translation table and try to load it again.
     *
     * In case there are no loaders registered the refresh() method will throw an Error.
     *
     * If the module is able to refresh translation tables refresh() method will broadcast
     * $translateRefreshStart and $translateRefreshEnd events.
     *
     * @example
     * // this will drop all currently existent translation tables and reload those which are
     * // currently in use
     * $translate.refresh();
     * // this will refresh a translation table for the en_US language
     * $translate.refresh('en_US');
     *
     * @param {string} langKey A language key of the table, which has to be refreshed
     *
     * @return {promise} Promise, which will be resolved in case a translation tables refreshing
     * process is finished successfully, and reject if not.
     */
    $translate.refresh = function (langKey) {
      if (!$loaderFactory) {
        throw new Error('Couldn\'t refresh translation table, no loader registered!');
      }

      $rootScope.$emit('$translateRefreshStart', {language : langKey});

      var deferred = $q.defer(), updatedLanguages = {};

      //private helper
      function loadNewData(languageKey) {
        var promise = loadAsync(languageKey);
        //update the load promise cache for this language
        langPromises[languageKey] = promise;
        //register a data handler for the promise
        promise.then(function (data) {
            //clear the cache for this language
            $translationTable[languageKey] = {};
            //add the new data for this language
            translations(languageKey, data.table);
            //track that we updated this language
            updatedLanguages[languageKey] = true;
          },
          //handle rejection to appease the $q validation
          angular.noop);
        return promise;
      }

      //set up post-processing
      deferred.promise.then(
        function () {
          for (var key in $translationTable) {
            if ($translationTable.hasOwnProperty(key)) {
              //delete cache entries that were not updated
              if (!(key in updatedLanguages)) {
                delete $translationTable[key];
              }
            }
          }
          if ($uses) {
            useLanguage($uses);
          }
        },
        //handle rejection to appease the $q validation
        angular.noop
      ).finally(
        function () {
          $rootScope.$emit('$translateRefreshEnd', {language : langKey});
        }
      );

      if (!langKey) {
        // if there's no language key specified we refresh ALL THE THINGS!
        var languagesToReload = $fallbackLanguage && $fallbackLanguage.slice() || [];
        if ($uses && languagesToReload.indexOf($uses) === -1) {
          languagesToReload.push($uses);
        }
        $q.all(languagesToReload.map(loadNewData)).then(deferred.resolve, deferred.reject);

      } else if ($translationTable[langKey]) {
        //just refresh the specified language cache
        loadNewData(langKey).then(deferred.resolve, deferred.reject);

      } else {
        deferred.reject();
      }

      return deferred.promise;
    };

    /**
     * @ngdoc function
     * @name pascalprecht.translate.$translate#instant
     * @methodOf pascalprecht.translate.$translate
     *
     * @description
     * Returns a translation instantly from the internal state of loaded translation. All rules
     * regarding the current language, the preferred language of even fallback languages will be
     * used except any promise handling. If a language was not found, an asynchronous loading
     * will be invoked in the background.
     *
     * @param {string|array} translationId A token which represents a translation id
     *                                     This can be optionally an array of translation ids which
     *                                     results that the function's promise returns an object where
     *                                     each key is the translation id and the value the translation.
     * @param {object} interpolateParams Params
     * @param {string} interpolationId The id of the interpolation to use
     * @param {string} forceLanguage A language to be used instead of the current language
     * @param {string} sanitizeStrategy force sanitize strategy for this call instead of using the configured one
     *
     * @return {string|object} translation
     */
    $translate.instant = function (translationId, interpolateParams, interpolationId, forceLanguage, sanitizeStrategy) {

      // we don't want to re-negotiate $uses
      var uses = (forceLanguage && forceLanguage !== $uses) ? // we don't want to re-negotiate $uses
        (negotiateLocale(forceLanguage) || forceLanguage) : $uses;

      // Detect undefined and null values to shorten the execution and prevent exceptions
      if (translationId === null || angular.isUndefined(translationId)) {
        return translationId;
      }

      // Check forceLanguage is present
      if (forceLanguage) {
        loadTranslationsIfMissing(forceLanguage);
      }

      // Duck detection: If the first argument is an array, a bunch of translations was requested.
      // The result is an object.
      if (angular.isArray(translationId)) {
        var results = {};
        for (var i = 0, c = translationId.length; i < c; i++) {
          results[translationId[i]] = $translate.instant(translationId[i], interpolateParams, interpolationId, forceLanguage, sanitizeStrategy);
        }
        return results;
      }

      // We discarded unacceptable values. So we just need to verify if translationId is empty String
      if (angular.isString(translationId) && translationId.length < 1) {
        return translationId;
      }

      // trim off any whitespace
      if (translationId) {
        translationId = trim.apply(translationId);
      }

      var result, possibleLangKeys = [];
      if ($preferredLanguage) {
        possibleLangKeys.push($preferredLanguage);
      }
      if (uses) {
        possibleLangKeys.push(uses);
      }
      if ($fallbackLanguage && $fallbackLanguage.length) {
        possibleLangKeys = possibleLangKeys.concat($fallbackLanguage);
      }
      for (var j = 0, d = possibleLangKeys.length; j < d; j++) {
        var possibleLangKey = possibleLangKeys[j];
        if ($translationTable[possibleLangKey]) {
          if (typeof $translationTable[possibleLangKey][translationId] !== 'undefined') {
            result = determineTranslationInstant(translationId, interpolateParams, interpolationId, uses, sanitizeStrategy);
          }
        }
        if (typeof result !== 'undefined') {
          break;
        }
      }

      if (!result && result !== '') {
        if ($notFoundIndicatorLeft || $notFoundIndicatorRight) {
          result = applyNotFoundIndicators(translationId);
        } else {
          // Return translation of default interpolator if not found anything.
          result = defaultInterpolator.interpolate(translationId, interpolateParams, 'filter', sanitizeStrategy);

          // looks like the requested translation id doesn't exists.
          // Now, if there is a registered handler for missing translations and no
          // asyncLoader is pending, we execute the handler
          var missingTranslationHandlerTranslation;
          if ($missingTranslationHandlerFactory && !pendingLoader) {
            missingTranslationHandlerTranslation = translateByHandler(translationId, interpolateParams, sanitizeStrategy);
          }

          if ($missingTranslationHandlerFactory && !pendingLoader && missingTranslationHandlerTranslation) {
            result = missingTranslationHandlerTranslation;
          }
        }
      }

      return result;
    };

    /**
     * @ngdoc function
     * @name pascalprecht.translate.$translate#versionInfo
     * @methodOf pascalprecht.translate.$translate
     *
     * @description
     * Returns the current version information for the angular-translate library
     *
     * @return {string} angular-translate version
     */
    $translate.versionInfo = function () {
      return version;
    };

    /**
     * @ngdoc function
     * @name pascalprecht.translate.$translate#loaderCache
     * @methodOf pascalprecht.translate.$translate
     *
     * @description
     * Returns the defined loaderCache.
     *
     * @return {boolean|string|object} current value of loaderCache
     */
    $translate.loaderCache = function () {
      return loaderCache;
    };

    // internal purpose only
    $translate.directivePriority = function () {
      return directivePriority;
    };

    // internal purpose only
    $translate.statefulFilter = function () {
      return statefulFilter;
    };

    /**
     * @ngdoc function
     * @name pascalprecht.translate.$translate#isReady
     * @methodOf pascalprecht.translate.$translate
     *
     * @description
     * Returns whether the service is "ready" to translate (i.e. loading 1st language).
     *
     * See also {@link pascalprecht.translate.$translate#methods_onReady onReady()}.
     *
     * @return {boolean} current value of ready
     */
    $translate.isReady = function () {
      return $isReady;
    };

    var $onReadyDeferred = $q.defer();
    $onReadyDeferred.promise.then(function () {
      $isReady = true;
    });

    /**
     * @ngdoc function
     * @name pascalprecht.translate.$translate#onReady
     * @methodOf pascalprecht.translate.$translate
     *
     * @description
     * Calls the function provided or resolved the returned promise after the service is "ready" to translate (i.e. loading 1st language).
     *
     * See also {@link pascalprecht.translate.$translate#methods_isReady isReady()}.
     *
     * @param {Function=} fn Function to invoke when service is ready
     * @return {object} Promise resolved when service is ready
     */
    $translate.onReady = function (fn) {
      var deferred = $q.defer();
      if (angular.isFunction(fn)) {
        deferred.promise.then(fn);
      }
      if ($isReady) {
        deferred.resolve();
      } else {
        $onReadyDeferred.promise.then(deferred.resolve);
      }
      return deferred.promise;
    };

    /**
     * @ngdoc function
     * @name pascalprecht.translate.$translate#getAvailableLanguageKeys
     * @methodOf pascalprecht.translate.$translate
     *
     * @description
     * This function simply returns the registered language keys being defined before in the config phase
     * With this, an application can use the array to provide a language selection dropdown or similar
     * without any additional effort
     *
     * @returns {object} returns the list of possibly registered language keys and mapping or null if not defined
     */
    $translate.getAvailableLanguageKeys = function () {
      if ($availableLanguageKeys.length > 0) {
        return $availableLanguageKeys;
      }
      return null;
    };

    /**
     * @ngdoc function
     * @name pascalprecht.translate.$translate#getTranslationTable
     * @methodOf pascalprecht.translate.$translate
     *
     * @description
     * Returns translation table by the given language key.
     *
     * Unless a language is provided it returns a translation table of the current one.
     * Note: If translation dictionary is currently downloading or in progress
     * it will return null.
     *
     * @param {string} langKey A token which represents a translation id
     *
     * @return {object} a copy of angular-translate $translationTable
     */
    $translate.getTranslationTable = function (langKey) {
      langKey = langKey || $translate.use();
      if (langKey && $translationTable[langKey]) {
        return angular.copy($translationTable[langKey]);
      }
      return null;
    };

    // Whenever $translateReady is being fired, this will ensure the state of $isReady
    var globalOnReadyListener = $rootScope.$on('$translateReady', function () {
      $onReadyDeferred.resolve();
      globalOnReadyListener(); // one time only
      globalOnReadyListener = null;
    });
    var globalOnChangeListener = $rootScope.$on('$translateChangeEnd', function () {
      $onReadyDeferred.resolve();
      globalOnChangeListener(); // one time only
      globalOnChangeListener = null;
    });

    if ($loaderFactory) {

      // If at least one async loader is defined and there are no
      // (default) translations available we should try to load them.
      if (angular.equals($translationTable, {})) {
        if ($translate.use()) {
          $translate.use($translate.use());
        }
      }

      // Also, if there are any fallback language registered, we start
      // loading them asynchronously as soon as we can.
      if ($fallbackLanguage && $fallbackLanguage.length) {
        var processAsyncResult = function (translation) {
          translations(translation.key, translation.table);
          $rootScope.$emit('$translateChangeEnd', {language : translation.key});
          return translation;
        };
        for (var i = 0, len = $fallbackLanguage.length; i < len; i++) {
          var fallbackLanguageId = $fallbackLanguage[i];
          if ($forceAsyncReloadEnabled || !$translationTable[fallbackLanguageId]) {
            langPromises[fallbackLanguageId] = loadAsync(fallbackLanguageId).then(processAsyncResult);
          }
        }
      }
    } else {
      $rootScope.$emit('$translateReady', {language : $translate.use()});
    }

    return $translate;
  }];
}

$translate.displayName = 'displayName';

/**
 * @ngdoc object
 * @name pascalprecht.translate.$translateDefaultInterpolation
 * @requires $interpolate
 *
 * @description
 * Uses angular's `$interpolate` services to interpolate strings against some values.
 *
 * Be aware to configure a proper sanitization strategy.
 *
 * See also:
 * * {@link pascalprecht.translate.$translateSanitization}
 *
 * @return {object} $translateDefaultInterpolation Interpolator service
 */
angular.module('pascalprecht.translate').factory('$translateDefaultInterpolation', $translateDefaultInterpolation);

function $translateDefaultInterpolation ($interpolate, $translateSanitization) {

  'use strict';

  var $translateInterpolator = {},
      $locale,
      $identifier = 'default';

  /**
   * @ngdoc function
   * @name pascalprecht.translate.$translateDefaultInterpolation#setLocale
   * @methodOf pascalprecht.translate.$translateDefaultInterpolation
   *
   * @description
   * Sets current locale (this is currently not use in this interpolation).
   *
   * @param {string} locale Language key or locale.
   */
  $translateInterpolator.setLocale = function (locale) {
    $locale = locale;
  };

  /**
   * @ngdoc function
   * @name pascalprecht.translate.$translateDefaultInterpolation#getInterpolationIdentifier
   * @methodOf pascalprecht.translate.$translateDefaultInterpolation
   *
   * @description
   * Returns an identifier for this interpolation service.
   *
   * @returns {string} $identifier
   */
  $translateInterpolator.getInterpolationIdentifier = function () {
    return $identifier;
  };

  /**
   * @deprecated will be removed in 3.0
   * @see {@link pascalprecht.translate.$translateSanitization}
   */
  $translateInterpolator.useSanitizeValueStrategy = function (value) {
    $translateSanitization.useStrategy(value);
    return this;
  };

  /**
   * @ngdoc function
   * @name pascalprecht.translate.$translateDefaultInterpolation#interpolate
   * @methodOf pascalprecht.translate.$translateDefaultInterpolation
   *
   * @description
   * Interpolates given value agains given interpolate params using angulars
   * `$interpolate` service.
   *
   * Since AngularJS 1.5, `value` must not be a string but can be anything input.
   *
   * @param {string} value translation
   * @param {object} interpolationParams interpolation params
   * @param {string} context current context (filter, directive, service)
   * @param {string} sanitizeStrategy sanitize strategy
   * @param {string} translationId current translationId
   *
   * @returns {string} interpolated string
   */
  $translateInterpolator.interpolate = function (value, interpolationParams, context, sanitizeStrategy, translationId) { // jshint ignore:line
    interpolationParams = interpolationParams || {};
    interpolationParams = $translateSanitization.sanitize(interpolationParams, 'params', sanitizeStrategy, context);

    var interpolatedText;
    if (angular.isNumber(value)) {
      // numbers are safe
      interpolatedText = '' + value;
    } else if (angular.isString(value)) {
      // strings must be interpolated (that's the job here)
      interpolatedText = $interpolate(value)(interpolationParams);
      interpolatedText = $translateSanitization.sanitize(interpolatedText, 'text', sanitizeStrategy, context);
    } else {
      // neither a number or a string, cant interpolate => empty string
      interpolatedText = '';
    }

    return interpolatedText;
  };

  return $translateInterpolator;
}

$translateDefaultInterpolation.displayName = '$translateDefaultInterpolation';

angular.module('pascalprecht.translate').constant('$STORAGE_KEY', 'NG_TRANSLATE_LANG_KEY');

angular.module('pascalprecht.translate')
/**
 * @ngdoc directive
 * @name pascalprecht.translate.directive:translate
 * @requires $interpolate, 
 * @requires $compile, 
 * @requires $parse, 
 * @requires $rootScope
 * @restrict AE
 *
 * @description
 * Translates given translation id either through attribute or DOM content.
 * Internally it uses $translate service to translate the translation id. It possible to
 * pass an optional `translate-values` object literal as string into translation id.
 *
 * @param {string=} translate Translation id which could be either string or interpolated string.
 * @param {string=} translate-values Values to pass into translation id. Can be passed as object literal string or interpolated object.
 * @param {string=} translate-attr-ATTR translate Translation id and put it into ATTR attribute.
 * @param {string=} translate-default will be used unless translation was successful
 * @param {boolean=} translate-compile (default true if present) defines locally activation of {@link pascalprecht.translate.$translateProvider#methods_usePostCompiling}
 * @param {boolean=} translate-keep-content (default true if present) defines that in case a KEY could not be translated, that the existing content is left in the innerHTML}
 *
 * @example
   <example module="ngView">
    <file name="index.html">
      <div ng-controller="TranslateCtrl">

        <pre translate="TRANSLATION_ID"></pre>
        <pre translate>TRANSLATION_ID</pre>
        <pre translate translate-attr-title="TRANSLATION_ID"></pre>
        <pre translate="{{translationId}}"></pre>
        <pre translate>{{translationId}}</pre>
        <pre translate="WITH_VALUES" translate-values="{value: 5}"></pre>
        <pre translate translate-values="{value: 5}">WITH_VALUES</pre>
        <pre translate="WITH_VALUES" translate-values="{{values}}"></pre>
        <pre translate translate-values="{{values}}">WITH_VALUES</pre>
        <pre translate translate-attr-title="WITH_VALUES" translate-values="{{values}}"></pre>
        <pre translate="WITH_CAMEL_CASE_KEY" translate-value-camel-case-key="Hi"></pre>

      </div>
    </file>
    <file name="script.js">
      angular.module('ngView', ['pascalprecht.translate'])

      .config(function ($translateProvider) {

        $translateProvider.translations('en',{
          'TRANSLATION_ID': 'Hello there!',
          'WITH_VALUES': 'The following value is dynamic: {{value}}',
          'WITH_CAMEL_CASE_KEY': 'The interpolation key is camel cased: {{camelCaseKey}}'
        }).preferredLanguage('en');

      });

      angular.module('ngView').controller('TranslateCtrl', function ($scope) {
        $scope.translationId = 'TRANSLATION_ID';

        $scope.values = {
          value: 78
        };
      });
    </file>
    <file name="scenario.js">
      it('should translate', function () {
        inject(function ($rootScope, $compile) {
          $rootScope.translationId = 'TRANSLATION_ID';

          element = $compile('<p translate="TRANSLATION_ID"></p>')($rootScope);
          $rootScope.$digest();
          expect(element.text()).toBe('Hello there!');

          element = $compile('<p translate="{{translationId}}"></p>')($rootScope);
          $rootScope.$digest();
          expect(element.text()).toBe('Hello there!');

          element = $compile('<p translate>TRANSLATION_ID</p>')($rootScope);
          $rootScope.$digest();
          expect(element.text()).toBe('Hello there!');

          element = $compile('<p translate>{{translationId}}</p>')($rootScope);
          $rootScope.$digest();
          expect(element.text()).toBe('Hello there!');

          element = $compile('<p translate translate-attr-title="TRANSLATION_ID"></p>')($rootScope);
          $rootScope.$digest();
          expect(element.attr('title')).toBe('Hello there!');

          element = $compile('<p translate="WITH_CAMEL_CASE_KEY" translate-value-camel-case-key="Hello"></p>')($rootScope);
          $rootScope.$digest();
          expect(element.text()).toBe('The interpolation key is camel cased: Hello');
        });
      });
    </file>
   </example>
 */
.directive('translate', translateDirective);
function translateDirective($translate, $interpolate, $compile, $parse, $rootScope) {

  'use strict';

  /**
   * @name trim
   * @private
   *
   * @description
   * trim polyfill
   *
   * @returns {string} The string stripped of whitespace from both ends
   */
  var trim = function() {
    return this.toString().replace(/^\s+|\s+$/g, '');
  };

  return {
    restrict: 'AE',
    scope: true,
    priority: $translate.directivePriority(),
    compile: function (tElement, tAttr) {

      var translateValuesExist = (tAttr.translateValues) ?
        tAttr.translateValues : undefined;

      var translateInterpolation = (tAttr.translateInterpolation) ?
        tAttr.translateInterpolation : undefined;

      var translateValueExist = tElement[0].outerHTML.match(/translate-value-+/i);

      var interpolateRegExp = '^(.*)(' + $interpolate.startSymbol() + '.*' + $interpolate.endSymbol() + ')(.*)',
          watcherRegExp = '^(.*)' + $interpolate.startSymbol() + '(.*)' + $interpolate.endSymbol() + '(.*)';

      return function linkFn(scope, iElement, iAttr) {

        scope.interpolateParams = {};
        scope.preText = '';
        scope.postText = '';
        scope.translateNamespace = getTranslateNamespace(scope);
        var translationIds = {};

        var initInterpolationParams = function (interpolateParams, iAttr, tAttr) {
          // initial setup
          if (iAttr.translateValues) {
            angular.extend(interpolateParams, $parse(iAttr.translateValues)(scope.$parent));
          }
          // initially fetch all attributes if existing and fill the params
          if (translateValueExist) {
            for (var attr in tAttr) {
              if (Object.prototype.hasOwnProperty.call(iAttr, attr) && attr.substr(0, 14) === 'translateValue' && attr !== 'translateValues') {
                var attributeName = angular.lowercase(attr.substr(14, 1)) + attr.substr(15);
                interpolateParams[attributeName] = tAttr[attr];
              }
            }
          }
        };

        // Ensures any change of the attribute "translate" containing the id will
        // be re-stored to the scope's "translationId".
        // If the attribute has no content, the element's text value (white spaces trimmed off) will be used.
        var observeElementTranslation = function (translationId) {

          // Remove any old watcher
          if (angular.isFunction(observeElementTranslation._unwatchOld)) {
            observeElementTranslation._unwatchOld();
            observeElementTranslation._unwatchOld = undefined;
          }

          if (angular.equals(translationId , '') || !angular.isDefined(translationId)) {
            var iElementText = trim.apply(iElement.text());

            // Resolve translation id by inner html if required
            var interpolateMatches = iElementText.match(interpolateRegExp);
            // Interpolate translation id if required
            if (angular.isArray(interpolateMatches)) {
              scope.preText = interpolateMatches[1];
              scope.postText = interpolateMatches[3];
              translationIds.translate = $interpolate(interpolateMatches[2])(scope.$parent);
              var watcherMatches = iElementText.match(watcherRegExp);
              if (angular.isArray(watcherMatches) && watcherMatches[2] && watcherMatches[2].length) {
                observeElementTranslation._unwatchOld = scope.$watch(watcherMatches[2], function (newValue) {
                  translationIds.translate = newValue;
                  updateTranslations();
                });
              }
            } else {
              // do not assigne the translation id if it is empty.
              translationIds.translate = !iElementText ? undefined : iElementText;
            }
          } else {
            translationIds.translate = translationId;
          }
          updateTranslations();
        };

        var observeAttributeTranslation = function (translateAttr) {
          iAttr.$observe(translateAttr, function (translationId) {
            translationIds[translateAttr] = translationId;
            updateTranslations();
          });
        };

        // initial setup with values
        initInterpolationParams(scope.interpolateParams, iAttr, tAttr);

        var firstAttributeChangedEvent = true;
        iAttr.$observe('translate', function (translationId) {
          if (typeof translationId === 'undefined') {
            // case of element "<translate>xyz</translate>"
            observeElementTranslation('');
          } else {
            // case of regular attribute
            if (translationId !== '' || !firstAttributeChangedEvent) {
              translationIds.translate = translationId;
              updateTranslations();
            }
          }
          firstAttributeChangedEvent = false;
        });

        for (var translateAttr in iAttr) {
          if (iAttr.hasOwnProperty(translateAttr) && translateAttr.substr(0, 13) === 'translateAttr' && translateAttr.length > 13) {
            observeAttributeTranslation(translateAttr);
          }
        }

        iAttr.$observe('translateDefault', function (value) {
          scope.defaultText = value;
          updateTranslations();
        });

        if (translateValuesExist) {
          iAttr.$observe('translateValues', function (interpolateParams) {
            if (interpolateParams) {
              scope.$parent.$watch(function () {
                angular.extend(scope.interpolateParams, $parse(interpolateParams)(scope.$parent));
              });
            }
          });
        }

        if (translateValueExist) {
          var observeValueAttribute = function (attrName) {
            iAttr.$observe(attrName, function (value) {
              var attributeName = angular.lowercase(attrName.substr(14, 1)) + attrName.substr(15);
              scope.interpolateParams[attributeName] = value;
            });
          };
          for (var attr in iAttr) {
            if (Object.prototype.hasOwnProperty.call(iAttr, attr) && attr.substr(0, 14) === 'translateValue' && attr !== 'translateValues') {
              observeValueAttribute(attr);
            }
          }
        }

        // Master update function
        var updateTranslations = function () {
          for (var key in translationIds) {
            if (translationIds.hasOwnProperty(key) && translationIds[key] !== undefined) {
              updateTranslation(key, translationIds[key], scope, scope.interpolateParams, scope.defaultText, scope.translateNamespace);
            }
          }
        };

        // Put translation processing function outside loop
        var updateTranslation = function(translateAttr, translationId, scope, interpolateParams, defaultTranslationText, translateNamespace) {
          if (translationId) {
            // if translation id starts with '.' and translateNamespace given, prepend namespace
            if (translateNamespace && translationId.charAt(0) === '.') {
              translationId = translateNamespace + translationId;
            }

            $translate(translationId, interpolateParams, translateInterpolation, defaultTranslationText, scope.translateLanguage)
              .then(function (translation) {
                applyTranslation(translation, scope, true, translateAttr);
              }, function (translationId) {
                applyTranslation(translationId, scope, false, translateAttr);
              });
          } else {
            // as an empty string cannot be translated, we can solve this using successful=false
            applyTranslation(translationId, scope, false, translateAttr);
          }
        };

        var applyTranslation = function (value, scope, successful, translateAttr) {
          if (!successful) {
            if (typeof scope.defaultText !== 'undefined') {
              value = scope.defaultText;
            }
          }
          if (translateAttr === 'translate') {
            // default translate into innerHTML
            if (successful || (!successful && !$translate.isKeepContent() && typeof iAttr.translateKeepContent === 'undefined')) {
              iElement.empty().append(scope.preText + value + scope.postText);
            }
            var globallyEnabled = $translate.isPostCompilingEnabled();
            var locallyDefined = typeof tAttr.translateCompile !== 'undefined';
            var locallyEnabled = locallyDefined && tAttr.translateCompile !== 'false';
            if ((globallyEnabled && !locallyDefined) || locallyEnabled) {
              $compile(iElement.contents())(scope);
            }
          } else {
            // translate attribute
            var attributeName = iAttr.$attr[translateAttr];
            if (attributeName.substr(0, 5) === 'data-') {
              // ensure html5 data prefix is stripped
              attributeName = attributeName.substr(5);
            }
            attributeName = attributeName.substr(15);
            iElement.attr(attributeName, value);
          }
        };

        if (translateValuesExist || translateValueExist || iAttr.translateDefault) {
          scope.$watch('interpolateParams', updateTranslations, true);
        }

        // Replaced watcher on translateLanguage with event listener
        scope.$on('translateLanguageChanged', updateTranslations);

        // Ensures the text will be refreshed after the current language was changed
        // w/ $translate.use(...)
        var unbind = $rootScope.$on('$translateChangeSuccess', updateTranslations);

        // ensure translation will be looked up at least one
        if (iElement.text().length) {
          if (iAttr.translate) {
            observeElementTranslation(iAttr.translate);
          } else {
            observeElementTranslation('');
          }
        } else if (iAttr.translate) {
          // ensure attribute will be not skipped
          observeElementTranslation(iAttr.translate);
        }
        updateTranslations();
        scope.$on('$destroy', unbind);
      };
    }
  };
}

/**
 * Returns the scope's namespace.
 * @private
 * @param scope
 * @returns {string}
 */
function getTranslateNamespace(scope) {
  'use strict';
  if (scope.translateNamespace) {
    return scope.translateNamespace;
  }
  if (scope.$parent) {
    return getTranslateNamespace(scope.$parent);
  }
}

translateDirective.displayName = 'translateDirective';

angular.module('pascalprecht.translate')
/**
 * @ngdoc directive
 * @name pascalprecht.translate.directive:translate-attr
 * @restrict A
 *
 * @description
 * Translates attributes like translate-attr-ATTR, but with an object like ng-class.
 * Internally it uses `translate` service to translate translation id. It possible to
 * pass an optional `translate-values` object literal as string into translation id.
 *
 * @param {string=} translate-attr Object literal mapping attributes to translation ids.
 * @param {string=} translate-values Values to pass into the translation ids. Can be passed as object literal string.
 *
 * @example
   <example module="ngView">
    <file name="index.html">
      <div ng-controller="TranslateCtrl">

        <input translate-attr="{ placeholder: translationId, title: 'WITH_VALUES' }" translate-values="{value: 5}" />

      </div>
    </file>
    <file name="script.js">
      angular.module('ngView', ['pascalprecht.translate'])

      .config(function ($translateProvider) {

        $translateProvider.translations('en',{
          'TRANSLATION_ID': 'Hello there!',
          'WITH_VALUES': 'The following value is dynamic: {{value}}',
        }).preferredLanguage('en');

      });

      angular.module('ngView').controller('TranslateCtrl', function ($scope) {
        $scope.translationId = 'TRANSLATION_ID';

        $scope.values = {
          value: 78
        };
      });
    </file>
    <file name="scenario.js">
      it('should translate', function () {
        inject(function ($rootScope, $compile) {
          $rootScope.translationId = 'TRANSLATION_ID';

          element = $compile('<input translate-attr="{ placeholder: translationId, title: 'WITH_VALUES' }" translate-values="{ value: 5 }" />')($rootScope);
          $rootScope.$digest();
          expect(element.attr('placeholder)).toBe('Hello there!');
          expect(element.attr('title)).toBe('The following value is dynamic: 5');
        });
      });
    </file>
   </example>
 */
.directive('translateAttr', translateAttrDirective);
function translateAttrDirective($translate, $rootScope) {

  'use strict';

  return {
    restrict: 'A',
    priority: $translate.directivePriority(),
    link: function linkFn(scope, element, attr) {

      var translateAttr,
          translateValues,
          previousAttributes = {};

      // Main update translations function
      var updateTranslations = function () {
        angular.forEach(translateAttr, function (translationId, attributeName) {
          if (!translationId) {
            return;
          }
          previousAttributes[attributeName] = true;

          // if translation id starts with '.' and translateNamespace given, prepend namespace
          if (scope.translateNamespace && translationId.charAt(0) === '.') {
            translationId = scope.translateNamespace + translationId;
          }
          $translate(translationId, translateValues, attr.translateInterpolation, undefined, scope.translateLanguage)
            .then(function (translation) {
              element.attr(attributeName, translation);
            }, function (translationId) {
              element.attr(attributeName, translationId);
            });
        });

        // Removing unused attributes that were previously used
        angular.forEach(previousAttributes, function (flag, attributeName) {
          if (!translateAttr[attributeName]) {
            element.removeAttr(attributeName);
            delete previousAttributes[attributeName];
          }
        });
      };

      // Watch for attribute changes
      watchAttribute(
        scope,
        attr.translateAttr,
        function (newValue) { translateAttr = newValue; },
        updateTranslations
      );
      // Watch for value changes
      watchAttribute(
        scope,
        attr.translateValues,
        function (newValue) { translateValues = newValue; },
        updateTranslations
      );

      if (attr.translateValues) {
        scope.$watch(attr.translateValues, updateTranslations, true);
      }

      // Replaced watcher on translateLanguage with event listener
      scope.$on('translateLanguageChanged', updateTranslations);

      // Ensures the text will be refreshed after the current language was changed
      // w/ $translate.use(...)
      var unbind = $rootScope.$on('$translateChangeSuccess', updateTranslations);

      updateTranslations();
      scope.$on('$destroy', unbind);
    }
  };
}

function watchAttribute(scope, attribute, valueCallback, changeCallback) {
  'use strict';
  if (!attribute) {
    return;
  }
  if (attribute.substr(0, 2) === '::') {
    attribute = attribute.substr(2);
  } else {
    scope.$watch(attribute, function(newValue) {
      valueCallback(newValue);
      changeCallback();
    }, true);
  }
  valueCallback(scope.$eval(attribute));
}

translateAttrDirective.displayName = 'translateAttrDirective';

angular.module('pascalprecht.translate')
/**
 * @ngdoc directive
 * @name pascalprecht.translate.directive:translateCloak
 * @requires $translate
 * @restrict A
 *
 * $description
 * Adds a `translate-cloak` class name to the given element where this directive
 * is applied initially and removes it, once a loader has finished loading.
 *
 * This directive can be used to prevent initial flickering when loading translation
 * data asynchronously.
 *
 * The class name is defined in
 * {@link pascalprecht.translate.$translateProvider#cloakClassName $translate.cloakClassName()}.
 *
 * @param {string=} translate-cloak If a translationId is provided, it will be used for showing
 *                                  or hiding the cloak. Basically it relies on the translation
 *                                  resolve.
 */
.directive('translateCloak', translateCloakDirective);

function translateCloakDirective($translate, $rootScope) {

  'use strict';

  return {
    compile : function (tElement) {
      var applyCloak = function (element) {
          element.addClass($translate.cloakClassName());
        },
        removeCloak = function (element) {
          element.removeClass($translate.cloakClassName());
        };
      applyCloak(tElement);

      return function linkFn(scope, iElement, iAttr) {
        //Create bound functions that incorporate the active DOM element.
        var iRemoveCloak = removeCloak.bind(this, iElement), iApplyCloak = applyCloak.bind(this, iElement);
        if (iAttr.translateCloak && iAttr.translateCloak.length) {
          // Register a watcher for the defined translation allowing a fine tuned cloak
          iAttr.$observe('translateCloak', function (translationId) {
            $translate(translationId).then(iRemoveCloak, iApplyCloak);
          });
          $rootScope.$on('$translateChangeSuccess', function () {
            $translate(iAttr.translateCloak).then(iRemoveCloak, iApplyCloak);
          });
        } else {
          $translate.onReady(iRemoveCloak);
        }
      };
    }
  };
}

translateCloakDirective.displayName = 'translateCloakDirective';

angular.module('pascalprecht.translate')
/**
 * @ngdoc directive
 * @name pascalprecht.translate.directive:translateNamespace
 * @restrict A
 *
 * @description
 * Translates given translation id either through attribute or DOM content.
 * Internally it uses `translate` filter to translate translation id. It possible to
 * pass an optional `translate-values` object literal as string into translation id.
 *
 * @param {string=} translate namespace name which could be either string or interpolated string.
 *
 * @example
   <example module="ngView">
    <file name="index.html">
      <div translate-namespace="CONTENT">

        <div>
            <h1 translate>.HEADERS.TITLE</h1>
            <h1 translate>.HEADERS.WELCOME</h1>
        </div>

        <div translate-namespace=".HEADERS">
            <h1 translate>.TITLE</h1>
            <h1 translate>.WELCOME</h1>
        </div>

      </div>
    </file>
    <file name="script.js">
      angular.module('ngView', ['pascalprecht.translate'])

      .config(function ($translateProvider) {

        $translateProvider.translations('en',{
          'TRANSLATION_ID': 'Hello there!',
          'CONTENT': {
            'HEADERS': {
                TITLE: 'Title'
            }
          },
          'CONTENT.HEADERS.WELCOME': 'Welcome'
        }).preferredLanguage('en');

      });

    </file>
   </example>
 */
.directive('translateNamespace', translateNamespaceDirective);

function translateNamespaceDirective() {

  'use strict';

  return {
    restrict: 'A',
    scope: true,
    compile: function () {
      return {
        pre: function (scope, iElement, iAttrs) {
          scope.translateNamespace = getTranslateNamespace(scope);

          if (scope.translateNamespace && iAttrs.translateNamespace.charAt(0) === '.') {
            scope.translateNamespace += iAttrs.translateNamespace;
          } else {
            scope.translateNamespace = iAttrs.translateNamespace;
          }
        }
      };
    }
  };
}

/**
 * Returns the scope's namespace.
 * @private
 * @param scope
 * @returns {string}
 */
function getTranslateNamespace(scope) {
  'use strict';
  if (scope.translateNamespace) {
    return scope.translateNamespace;
  }
  if (scope.$parent) {
    return getTranslateNamespace(scope.$parent);
  }
}

translateNamespaceDirective.displayName = 'translateNamespaceDirective';

angular.module('pascalprecht.translate')
/**
 * @ngdoc directive
 * @name pascalprecht.translate.directive:translateLanguage
 * @restrict A
 *
 * @description
 * Forces the language to the directives in the underlying scope.
 *
 * @param {string=} translate language that will be negotiated.
 *
 * @example
   <example module="ngView">
    <file name="index.html">
      <div>

        <div>
            <h1 translate>HELLO</h1>
        </div>

        <div translate-language="de">
            <h1 translate>HELLO</h1>
        </div>

      </div>
    </file>
    <file name="script.js">
      angular.module('ngView', ['pascalprecht.translate'])

      .config(function ($translateProvider) {

        $translateProvider
          .translations('en',{
            'HELLO': 'Hello world!'
          })
          .translations('de',{
            'HELLO': 'Hallo Welt!'
          })
          .preferredLanguage('en');

      });

    </file>
   </example>
 */
.directive('translateLanguage', translateLanguageDirective);

function translateLanguageDirective() {

  'use strict';

  return {
    restrict: 'A',
    scope: true,
    compile: function () {
      return function linkFn(scope, iElement, iAttrs) {

        iAttrs.$observe('translateLanguage', function (newTranslateLanguage) {
          scope.translateLanguage = newTranslateLanguage;
        });

        scope.$watch('translateLanguage', function(){
          scope.$broadcast('translateLanguageChanged');
        });
      };
    }
  };
}

translateLanguageDirective.displayName = 'translateLanguageDirective';

angular.module('pascalprecht.translate')
/**
 * @ngdoc filter
 * @name pascalprecht.translate.filter:translate
 * @requires $parse
 * @requires pascalprecht.translate.$translate
 * @function
 *
 * @description
 * Uses `$translate` service to translate contents. Accepts interpolate parameters
 * to pass dynamized values though translation.
 *
 * @param {string} translationId A translation id to be translated.
 * @param {*=} interpolateParams Optional object literal (as hash or string) to pass values into translation.
 *
 * @returns {string} Translated text.
 *
 * @example
   <example module="ngView">
    <file name="index.html">
      <div ng-controller="TranslateCtrl">

        <pre>{{ 'TRANSLATION_ID' | translate }}</pre>
        <pre>{{ translationId | translate }}</pre>
        <pre>{{ 'WITH_VALUES' | translate:'{value: 5}' }}</pre>
        <pre>{{ 'WITH_VALUES' | translate:values }}</pre>

      </div>
    </file>
    <file name="script.js">
      angular.module('ngView', ['pascalprecht.translate'])

      .config(function ($translateProvider) {

        $translateProvider.translations('en', {
          'TRANSLATION_ID': 'Hello there!',
          'WITH_VALUES': 'The following value is dynamic: {{value}}'
        });
        $translateProvider.preferredLanguage('en');

      });

      angular.module('ngView').controller('TranslateCtrl', function ($scope) {
        $scope.translationId = 'TRANSLATION_ID';

        $scope.values = {
          value: 78
        };
      });
    </file>
   </example>
 */
.filter('translate', translateFilterFactory);

function translateFilterFactory($parse, $translate) {

  'use strict';

  var translateFilter = function (translationId, interpolateParams, interpolation, forceLanguage) {
    if (!angular.isObject(interpolateParams)) {
      var ctx = this || {
        '__SCOPE_IS_NOT_AVAILABLE': 'More info at https://github.com/angular/angular.js/commit/8863b9d04c722b278fa93c5d66ad1e578ad6eb1f'
        };
      interpolateParams = $parse(interpolateParams)(ctx);
    }

    return $translate.instant(translationId, interpolateParams, interpolation, forceLanguage);
  };

  if ($translate.statefulFilter()) {
    translateFilter.$stateful = true;
  }

  return translateFilter;
}

translateFilterFactory.displayName = 'translateFilterFactory';

angular.module('pascalprecht.translate')

/**
 * @ngdoc object
 * @name pascalprecht.translate.$translationCache
 * @requires $cacheFactory
 *
 * @description
 * The first time a translation table is used, it is loaded in the translation cache for quick retrieval. You
 * can load translation tables directly into the cache by consuming the
 * `$translationCache` service directly.
 *
 * @return {object} $cacheFactory object.
 */
  .factory('$translationCache', $translationCache);

function $translationCache($cacheFactory) {

  'use strict';

  return $cacheFactory('translations');
}

$translationCache.displayName = '$translationCache';
return 'pascalprecht.translate';

}));
