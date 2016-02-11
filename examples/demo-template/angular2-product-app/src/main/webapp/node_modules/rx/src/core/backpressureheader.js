  // References
  var Observable = Rx.Observable,
    observableProto = Observable.prototype,
    AnonymousObservable = Rx.AnonymousObservable,
    AbstractObserver = Rx.internals.AbstractObserver,
    CompositeDisposable = Rx.CompositeDisposable,
    Subject = Rx.Subject,
    Observer = Rx.Observer,
    disposableEmpty = Rx.Disposable.empty,
    disposableCreate = Rx.Disposable.create,
    inherits = Rx.internals.inherits,
    addProperties = Rx.internals.addProperties,
    timeoutScheduler = Rx.Scheduler.timeout,
    currentThreadScheduler = Rx.Scheduler.currentThread,
    identity = Rx.helpers.identity;

  var objectDisposed = 'Object has been disposed';
  function checkDisposed() { if (this.isDisposed) { throw new Error(objectDisposed); } }
