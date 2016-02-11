/** @license MIT License (c) copyright 2011-2013 original author or authors */

/**
 * Licensed under the MIT License at:
 * http://www.opensource.org/licenses/mit-license.php
 *
 * @author Brian Cavalier
 * @author John Hann
 */
(function(define) { 'use strict';
define(function(require) {

	var when = require('./when');
	var Promise = when.Promise;
	var toPromise = when.resolve;

	return {
		all: when.lift(all),
		map: map,
		settle: settle
	};

	/**
	 * Resolve all the key-value pairs in the supplied object or promise
	 * for an object.
	 * @param {Promise|object} object or promise for object whose key-value pairs
	 *  will be resolved
	 * @returns {Promise} promise for an object with the fully resolved key-value pairs
	 */
	function all(object) {
		var p = Promise._defer();
		var resolver = Promise._handler(p);

		var results = {};
		var keys = Object.keys(object);
		var pending = keys.length;

		for(var i=0, k; i<keys.length; ++i) {
			k = keys[i];
			Promise._handler(object[k]).fold(settleKey, k, results, resolver);
		}

		if(pending === 0) {
			resolver.resolve(results);
		}

		return p;

		function settleKey(k, x, resolver) {
			/*jshint validthis:true*/
			this[k] = x;
			if(--pending === 0) {
				resolver.resolve(results);
			}
		}
	}

	/**
	 * Map values in the supplied object's keys
	 * @param {Promise|object} object or promise for object whose key-value pairs
	 *  will be reduced
	 * @param {function(value:*, key:String):*} f mapping function which may
	 *  return either a promise or a value
	 * @returns {Promise} promise for an object with the mapped and fully
	 *  resolved key-value pairs
	 */
	function map(object, f) {
		return toPromise(object).then(function(object) {
			return all(Object.keys(object).reduce(function(o, k) {
				o[k] = toPromise(object[k]).fold(mapWithKey, k);
				return o;
			}, {}));
		});

		function mapWithKey(k, x) {
			return f(x, k);
		}
	}

	/**
	 * Resolve all key-value pairs in the supplied object and return a promise
	 * that will always fulfill with the outcome states of all input promises.
	 * @param {object} object whose key-value pairs will be settled
	 * @returns {Promise} promise for an object with the mapped and fully
	 *  settled key-value pairs
	 */
	function settle(object) {
		var keys = Object.keys(object);
		var results = {};

		if(keys.length === 0) {
			return toPromise(results);
		}

		var p = Promise._defer();
		var resolver = Promise._handler(p);
		var promises = keys.map(function(k) { return object[k]; });

		when.settle(promises).then(function(states) {
			populateResults(keys, states, results, resolver);
		});

		return p;
	}

	function populateResults(keys, states, results, resolver) {
		for(var i=0; i<keys.length; i++) {
			results[keys[i]] = states[i];
		}
		resolver.resolve(results);
	}

});
})(typeof define === 'function' && define.amd ? define : function (factory) { module.exports = factory(require); });
