  /**
   * Convert an object into an observable sequence of [key, value] pairs.
   * @param {Object} obj The object to inspect.
   * @param {Scheduler} [scheduler] Scheduler to run the enumeration of the input sequence on.
   * @returns {Observable} An observable sequence of [key, value] pairs from the object.
   */
  Observable.pairs = function (obj, scheduler) {
    scheduler || (scheduler = Rx.Scheduler.currentThread);
    return new AnonymousObservable(function (observer) {
      var idx = 0, keys = Object.keys(obj), len = keys.length;
      return scheduler.scheduleRecursive(function (self) {
        if (idx < len) {
          var key = keys[idx++];
          observer.onNext([key, obj[key]]);
          self();
        } else {
          observer.onCompleted();
        }
      });
    });
  };
