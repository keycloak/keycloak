  function observableTimerDateAndPeriod(dueTime, period, scheduler) {
    return new AnonymousObservable(function (observer) {
      var count = 0, d = dueTime, p = normalizeTime(period);
      return scheduler.scheduleRecursiveWithAbsolute(d, function (self) {
        if (p > 0) {
          var now = scheduler.now();
          d = d + p;
          d <= now && (d = now + p);
        }
        observer.onNext(count++);
        self(d);
      });
    });
  }
