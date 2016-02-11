  /**
   *  Time shifts the observable sequence based on a subscription delay and a delay selector function for each element.
   *
   * @example
   *  1 - res = source.delayWithSelector(function (x) { return Rx.Scheduler.timer(5000); }); // with selector only
   *  1 - res = source.delayWithSelector(Rx.Observable.timer(2000), function (x) { return Rx.Observable.timer(x); }); // with delay and selector
   *
   * @param {Observable} [subscriptionDelay]  Sequence indicating the delay for the subscription to the source.
   * @param {Function} delayDurationSelector Selector function to retrieve a sequence indicating the delay for each given element.
   * @returns {Observable} Time-shifted sequence.
   */
  observableProto.delayWithSelector = function (subscriptionDelay, delayDurationSelector) {
      var source = this, subDelay, selector;
      if (typeof subscriptionDelay === 'function') {
        selector = subscriptionDelay;
      } else {
        subDelay = subscriptionDelay;
        selector = delayDurationSelector;
      }
      return new AnonymousObservable(function (observer) {
        var delays = new CompositeDisposable(), atEnd = false, done = function () {
            if (atEnd && delays.length === 0) { observer.onCompleted(); }
        }, subscription = new SerialDisposable(), start = function () {
          subscription.setDisposable(source.subscribe(function (x) {
              var delay;
              try {
                delay = selector(x);
              } catch (error) {
                observer.onError(error);
                return;
              }
              var d = new SingleAssignmentDisposable();
              delays.add(d);
              d.setDisposable(delay.subscribe(function () {
                observer.onNext(x);
                delays.remove(d);
                done();
              }, observer.onError.bind(observer), function () {
                observer.onNext(x);
                delays.remove(d);
                done();
              }));
          }, observer.onError.bind(observer), function () {
            atEnd = true;
            subscription.dispose();
            done();
          }));
      };

      if (!subDelay) {
        start();
      } else {
        subscription.setDisposable(subDelay.subscribe(start, observer.onError.bind(observer), start));
      }

      return new CompositeDisposable(subscription, delays);
    }, this);
  };
