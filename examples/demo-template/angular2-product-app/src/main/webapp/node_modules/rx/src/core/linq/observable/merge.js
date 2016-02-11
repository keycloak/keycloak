  /**
   * Merges all the observable sequences into a single observable sequence.
   * The scheduler is optional and if not specified, the immediate scheduler is used.
   * @returns {Observable} The observable sequence that merges the elements of the observable sequences.
   */
  var observableMerge = Observable.merge = function () {
    var scheduler, sources;
    if (!arguments[0]) {
      scheduler = immediateScheduler;
      sources = slice.call(arguments, 1);
    } else if (isScheduler(arguments[0])) {
      scheduler = arguments[0];
      sources = slice.call(arguments, 1);
    } else {
      scheduler = immediateScheduler;
      sources = slice.call(arguments, 0);
    }
    if (Array.isArray(sources[0])) {
      sources = sources[0];
    }
    return observableOf(scheduler, sources).mergeAll();
  };
