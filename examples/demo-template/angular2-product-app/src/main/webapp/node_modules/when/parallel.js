/** @license MIT License (c) copyright 2011-2013 original author or authors */

/**
 * parallel.js
 *
 * Run a set of task functions in parallel.  All tasks will
 * receive the same args
 *
 * @author Brian Cavalier
 * @author John Hann
 */

(function(define) {
define(function(require) {

	var when = require('./when');
	var all = when.Promise.all;
	var slice = Array.prototype.slice;

	/**
	 * Run array of tasks in parallel
	 * @param tasks {Array|Promise} array or promiseForArray of task functions
	 * @param [args] {*} arguments to be passed to all tasks
	 * @return {Promise} promise for array containing the
	 * result of each task in the array position corresponding
	 * to position of the task in the tasks array
	 */
	return function parallel(tasks /*, args... */) {
		return all(slice.call(arguments, 1)).then(function(args) {
			return when.map(tasks, function(task) {
				return task.apply(void 0, args);
			});
		});
	};

});
})(typeof define === 'function' && define.amd ? define : function (factory) { module.exports = factory(require); });


