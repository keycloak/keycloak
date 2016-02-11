/** @license MIT License (c) copyright B Cavalier & J Hann */

(function(define) {
define(function(require) {

	var unfold = require('../when').unfold;

	/**
	 * @deprecated
	 * Given a seed and generator, produces an Array.  Effectively the
	 * dual (opposite) of when.reduce()
	 * @param {function} generator function that generates a value (or promise
	 *  for a value) to be placed in the resulting array
	 * @param {function} condition given a seed, must return truthy if the unfold
	 *  should continue, or falsey if it should terminate
	 * @param {*|Promise} seed any value or promise
	 * @return {Promise} resulting array
	 */
	return function list(generator, condition, seed) {
		var result = [];

		return unfold(generator, condition, append, seed)['yield'](result);

		function append(value, newSeed) {
			result.push(value);
			return newSeed;
		}
	};

});
})(typeof define === 'function' && define.amd ? define : function (factory) { module.exports = factory(require); });

