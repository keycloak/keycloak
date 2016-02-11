/** @license MIT License (c) copyright 2013-2014 original author or authors */

/**
 * Collection of helper functions for interacting with 'traditional',
 * callback-taking functions using a promise interface.
 *
 * @author Renato Zannon
 * @contributor Brian Cavalier
 */

(function(define) {
define(function(require) {

	var when = require('./when');
	var Promise = when.Promise;
	var _liftAll = require('./lib/liftAll');
	var slice = Array.prototype.slice;

	var makeApply = require('./lib/apply');
	var _apply = makeApply(Promise, dispatch);

	return {
		lift: lift,
		liftAll: liftAll,
		apply: apply,
		call: call,
		promisify: promisify
	};

	/**
	 * Takes a `traditional` callback-taking function and returns a promise for its
	 * result, accepting an optional array of arguments (that might be values or
	 * promises). It assumes that the function takes its callback and errback as
	 * the last two arguments. The resolution of the promise depends on whether the
	 * function will call its callback or its errback.
	 *
	 * @example
	 *    var domIsLoaded = callbacks.apply($);
	 *    domIsLoaded.then(function() {
	 *		doMyDomStuff();
	 *	});
	 *
	 * @example
	 *    function existingAjaxyFunction(url, callback, errback) {
	 *		// Complex logic you'd rather not change
	 *	}
	 *
	 *    var promise = callbacks.apply(existingAjaxyFunction, ["/movies.json"]);
	 *
	 *    promise.then(function(movies) {
	 *		// Work with movies
	 *	}, function(reason) {
	 *		// Handle error
	 *	});
	 *
	 * @param {function} asyncFunction function to be called
	 * @param {Array} [extraAsyncArgs] array of arguments to asyncFunction
	 * @returns {Promise} promise for the callback value of asyncFunction
	 */
	function apply(asyncFunction, extraAsyncArgs) {
		return _apply(asyncFunction, this, extraAsyncArgs || []);
	}

	/**
	 * Apply helper that allows specifying thisArg
	 * @private
	 */
	function dispatch(f, thisArg, args, h) {
		args.push(alwaysUnary(h.resolve, h), alwaysUnary(h.reject, h));
		tryCatchResolve(f, thisArg, args, h);
	}

	function tryCatchResolve(f, thisArg, args, resolver) {
		try {
			f.apply(thisArg, args);
		} catch(e) {
			resolver.reject(e);
		}
	}

	/**
	 * Works as `callbacks.apply` does, with the difference that the arguments to
	 * the function are passed individually, instead of as an array.
	 *
	 * @example
	 *    function sumInFiveSeconds(a, b, callback) {
	 *		setTimeout(function() {
	 *			callback(a + b);
	 *		}, 5000);
	 *	}
	 *
	 *    var sumPromise = callbacks.call(sumInFiveSeconds, 5, 10);
	 *
	 *    // Logs '15' 5 seconds later
	 *    sumPromise.then(console.log);
	 *
	 * @param {function} asyncFunction function to be called
	 * @param {...*} args arguments that will be forwarded to the function
	 * @returns {Promise} promise for the callback value of asyncFunction
	 */
	function call(asyncFunction/*, arg1, arg2...*/) {
		return _apply(asyncFunction, this, slice.call(arguments, 1));
	}

	/**
	 * Takes a 'traditional' callback/errback-taking function and returns a function
	 * that returns a promise instead. The resolution/rejection of the promise
	 * depends on whether the original function will call its callback or its
	 * errback.
	 *
	 * If additional arguments are passed to the `lift` call, they will be prepended
	 * on the calls to the original function, much like `Function.prototype.bind`.
	 *
	 * The resulting function is also "promise-aware", in the sense that, if given
	 * promises as arguments, it will wait for their resolution before executing.
	 *
	 * @example
	 *    function traditionalAjax(method, url, callback, errback) {
	 *		var xhr = new XMLHttpRequest();
	 *		xhr.open(method, url);
	 *
	 *		xhr.onload = callback;
	 *		xhr.onerror = errback;
	 *
	 *		xhr.send();
	 *	}
	 *
	 *    var promiseAjax = callbacks.lift(traditionalAjax);
	 *    promiseAjax("GET", "/movies.json").then(console.log, console.error);
	 *
	 *    var promiseAjaxGet = callbacks.lift(traditionalAjax, "GET");
	 *    promiseAjaxGet("/movies.json").then(console.log, console.error);
	 *
	 * @param {Function} f traditional async function to be decorated
	 * @param {...*} [args] arguments to be prepended for the new function @deprecated
	 * @returns {Function} a promise-returning function
	 */
	function lift(f/*, args...*/) {
		var args = arguments.length > 1 ? slice.call(arguments, 1) : [];
		return function() {
			return _apply(f, this, args.concat(slice.call(arguments)));
		};
	}

	/**
	 * Lift all the functions/methods on src
	 * @param {object|function} src source whose functions will be lifted
	 * @param {function?} combine optional function for customizing the lifting
	 *  process. It is passed dst, the lifted function, and the property name of
	 *  the original function on src.
	 * @param {(object|function)?} dst option destination host onto which to place lifted
	 *  functions. If not provided, liftAll returns a new object.
	 * @returns {*} If dst is provided, returns dst with lifted functions as
	 *  properties.  If dst not provided, returns a new object with lifted functions.
	 */
	function liftAll(src, combine, dst) {
		return _liftAll(lift, combine, dst, src);
	}

	/**
	 * `promisify` is a version of `lift` that allows fine-grained control over the
	 * arguments that passed to the underlying function. It is intended to handle
	 * functions that don't follow the common callback and errback positions.
	 *
	 * The control is done by passing an object whose 'callback' and/or 'errback'
	 * keys, whose values are the corresponding 0-based indexes of the arguments on
	 * the function. Negative values are interpreted as being relative to the end
	 * of the arguments array.
	 *
	 * If arguments are given on the call to the 'promisified' function, they are
	 * intermingled with the callback and errback. If a promise is given among them,
	 * the execution of the function will only occur after its resolution.
	 *
	 * @example
	 *    var delay = callbacks.promisify(setTimeout, {
	 *		callback: 0
	 *	});
	 *
	 *    delay(100).then(function() {
	 *		console.log("This happens 100ms afterwards");
	 *	});
	 *
	 * @example
	 *    function callbackAsLast(errback, followsStandards, callback) {
	 *		if(followsStandards) {
	 *			callback("well done!");
	 *		} else {
	 *			errback("some programmers just want to watch the world burn");
	 *		}
	 *	}
	 *
	 *    var promisified = callbacks.promisify(callbackAsLast, {
	 *		callback: -1,
	 *		errback:   0,
	 *	});
	 *
	 *    promisified(true).then(console.log, console.error);
	 *    promisified(false).then(console.log, console.error);
	 *
	 * @param {Function} asyncFunction traditional function to be decorated
	 * @param {object} positions
	 * @param {number} [positions.callback] index at which asyncFunction expects to
	 *  receive a success callback
	 * @param {number} [positions.errback] index at which asyncFunction expects to
	 *  receive an error callback
	 *  @returns {function} promisified function that accepts
	 *
	 * @deprecated
	 */
	function promisify(asyncFunction, positions) {

		return function() {
			var thisArg = this;
			return Promise.all(arguments).then(function(args) {
				var p = Promise._defer();

				var callbackPos, errbackPos;

				if(typeof positions.callback === 'number') {
					callbackPos = normalizePosition(args, positions.callback);
				}

				if(typeof positions.errback === 'number') {
					errbackPos = normalizePosition(args, positions.errback);
				}

				if(errbackPos < callbackPos) {
					insertCallback(args, errbackPos, p._handler.reject, p._handler);
					insertCallback(args, callbackPos, p._handler.resolve, p._handler);
				} else {
					insertCallback(args, callbackPos, p._handler.resolve, p._handler);
					insertCallback(args, errbackPos, p._handler.reject, p._handler);
				}

				asyncFunction.apply(thisArg, args);

				return p;
			});
		};
	}

	function normalizePosition(args, pos) {
		return pos < 0 ? (args.length + pos + 2) : pos;
	}

	function insertCallback(args, pos, callback, thisArg) {
		if(typeof pos === 'number') {
			args.splice(pos, 0, alwaysUnary(callback, thisArg));
		}
	}

	function alwaysUnary(fn, thisArg) {
		return function() {
			if (arguments.length > 1) {
				fn.call(thisArg, slice.call(arguments));
			} else {
				fn.apply(thisArg, arguments);
			}
		};
	}
});
})(typeof define === 'function' && define.amd ? define : function (factory) { module.exports = factory(require); });
