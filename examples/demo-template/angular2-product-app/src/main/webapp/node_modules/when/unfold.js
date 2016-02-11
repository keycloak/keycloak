/** @license MIT License (c) copyright B Cavalier & J Hann */

/**
 * unfold
 * @author: brian@hovercraftstudios.com
 */
(function(define) {
define(function(require) {

	/**
	 * @deprecated Use when.unfold
	 */
	return require('./when').unfold;

});
})(typeof define === 'function' && define.amd ? define : function (factory) { module.exports = factory(require); } );

