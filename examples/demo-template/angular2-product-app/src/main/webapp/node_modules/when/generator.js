/** @license MIT License (c) copyright 2010-2014 original author or authors */
/** @author Brian Cavalier */
/** @author John Hann */

(function(define) { 'use strict';
define(function(require) {

	var when = require('./when');
	var slice = Array.prototype.slice;
	var Promise = when.Promise;
	var reject = Promise.reject;

	/**
	 * Lift a generator to create a function that can suspend and
	 * resume using the `yield` keyword to await promises.
	 * @param {function} generator
	 * @return {function}
	 */
	function lift(generator) {
		return function() {
			return run(generator, this, arguments);
		};
	}

	/**
	 * Immediately call a generator as a promise-aware coroutine
	 * that can suspend and resume using the `yield` keyword to
	 * await promises.  Additional arguments after the first will
	 * be passed through to the generator.
	 * @param {function} generator
	 * @returns {Promise} promise for the ultimate value returned
	 *  from the generator.
	 */
	function call(generator /*x, y, z...*/) {
		/*jshint validthis:true*/
		return run(generator, this, slice.call(arguments, 1));
	}

	/**
	 * Immediately apply a generator, with the supplied args array,
	 * as a promise-aware coroutine that can suspend and resume
	 * using the `yield` keyword to await promises.
	 * @param {function} generator
	 * @param {Array} args arguments with which to initialize the generator
	 * @returns {Promise} promise for the ultimate value returned
	 *  from the generator.
	 */
	function apply(generator, args) {
		/*jshint validthis:true*/
		return run(generator, this, args || []);
	}

	/**
	 * Helper to initiate the provided generator as a coroutine
	 * @returns {*}
	 */
	function run(generator, thisArg, args) {
		return runNext(void 0, generator.apply(thisArg, args));
	}

	function runNext(x, iterator) {
		try {
			return handle(iterator.next(x), iterator);
		} catch(e) {
			return reject(e);
		}
	}

	function next(x) {
		/*jshint validthis:true*/
		return runNext(x, this);
	}

	function error(e) {
		/*jshint validthis:true*/
		try {
			return handle(this.throw(e), this);
		} catch(e) {
			return reject(e);
		}
	}

	function handle(result, iterator) {
		if(result.done) {
			return result.value;
		}

		var h = Promise._handler(result.value);
		if(h.state() > 0) {
			return runNext(h.value, iterator);
		}

		var p = Promise._defer();
		h.chain(p._handler, iterator, next, error);
		return p;
	}

	return {
		lift: lift,
		call: call,
		apply: apply
	};

});
}(typeof define === 'function' && define.amd ? define : function(factory) { module.exports = factory(require); }));
