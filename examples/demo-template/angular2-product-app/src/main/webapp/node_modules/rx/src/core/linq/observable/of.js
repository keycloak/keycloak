  function observableOf (scheduler, array) {
    isScheduler(scheduler) || (scheduler = currentThreadScheduler);
    return new AnonymousObservable(function (observer) {
      var count = 0, len = array.length;
      return scheduler.scheduleRecursive(function (self) {
        if (count < len) {
          observer.onNext(array[count++]);
          self();
        } else {
          observer.onCompleted();
        }
      });
    });
  }

  /**
   *  This method creates a new Observable instance with a variable number of arguments, regardless of number or type of the arguments.
   * @returns {Observable} The observable sequence whose elements are pulled from the given arguments.
   */
  Observable.of = function () {
    return observableOf(null, arguments);
  };

  /**
   *  This method creates a new Observable instance with a variable number of arguments, regardless of number or type of the arguments.
   * @param {Scheduler} scheduler A scheduler to use for scheduling the arguments.
   * @returns {Observable} The observable sequence whose elements are pulled from the given arguments.
   */
  Observable.ofWithScheduler = function (scheduler) {
    return observableOf(scheduler, slice.call(arguments, 1));
  };
