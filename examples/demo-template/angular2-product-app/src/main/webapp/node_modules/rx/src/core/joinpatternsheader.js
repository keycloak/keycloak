  // Aliases
  var Observable = Rx.Observable,
      observableProto = Observable.prototype,
      AnonymousObservable = Rx.AnonymousObservable,
      observableThrow = Observable.throwException,
      observerCreate = Rx.Observer.create,
      SingleAssignmentDisposable = Rx.SingleAssignmentDisposable,
      CompositeDisposable = Rx.CompositeDisposable,
      AbstractObserver = Rx.internals.AbstractObserver,
      noop = Rx.helpers.noop,
      defaultComparer = Rx.internals.isEqual,
      inherits = Rx.internals.inherits,
      Enumerable = Rx.internals.Enumerable,
      Enumerator = Rx.internals.Enumerator,
      $iterator$ = Rx.iterator,
      doneEnumerator = Rx.doneEnumerator,
      slice = Array.prototype.slice;

  // Utilities
  function argsOrArray(args, idx) {
    return args.length === 1 && Array.isArray(args[idx]) ?
      args[idx] :
      slice.call(args);
  }
