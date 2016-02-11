  /**
   *  Joins together the results from several patterns.
   *
   *  @param plans A series of plans (specified as an Array of as a series of arguments) created by use of the Then operator on patterns.
   *  @returns {Observable} Observable sequence with the results form matching several patterns.
   */
  Observable.when = function () {
    var plans = argsOrArray(arguments, 0);
    return new AnonymousObservable(function (observer) {
      var activePlans = [],
          externalSubscriptions = new Map();
      var outObserver = observerCreate(
        observer.onNext.bind(observer),
        function (err) {
          externalSubscriptions.forEach(function (v) { v.onError(err); });
          observer.onError(err);
        },
        observer.onCompleted.bind(observer)
      );
      try {
        for (var i = 0, len = plans.length; i < len; i++) {
          activePlans.push(plans[i].activate(externalSubscriptions, outObserver, function (activePlan) {
            var idx = activePlans.indexOf(activePlan);
            activePlans.splice(idx, 1);
            activePlans.length === 0 && observer.onCompleted();
          }));
        }
      } catch (e) {
        observableThrow(e).subscribe(observer);
      }
      var group = new CompositeDisposable();
      externalSubscriptions.forEach(function (joinObserver) {
        joinObserver.subscribe();
        group.add(joinObserver);
      });

      return group;
    });
  };
