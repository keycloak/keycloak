  function firstOrDefaultAsync(source, hasDefault, defaultValue) {
    return new AnonymousObservable(function (o) {
      return source.subscribe(function (x) {
        o.onNext(x);
        o.onCompleted();
      }, function (e) { o.onError(e); }, function () {
        if (!hasDefault) {
          o.onError(new Error(sequenceContainsNoElements));
        } else {
          o.onNext(defaultValue);
          o.onCompleted();
        }
      });
    }, source);
  }
