  var AutoDetachObserver = (function (__super__) {
    inherits(AutoDetachObserver, __super__);

    function AutoDetachObserver(observer) {
      __super__.call(this);
      this.observer = observer;
      this.m = new SingleAssignmentDisposable();
    }

    var AutoDetachObserverPrototype = AutoDetachObserver.prototype;

    AutoDetachObserverPrototype.next = function (value) {
      var noError = false;
      try {
        this.observer.onNext(value);
        noError = true;
      } catch (e) {
        throw e;
      } finally {
        !noError && this.dispose();
      }
    };

    AutoDetachObserverPrototype.error = function (err) {
      try {
        this.observer.onError(err);
      } catch (e) {
        throw e;
      } finally {
        this.dispose();
      }
    };

    AutoDetachObserverPrototype.completed = function () {
      try {
        this.observer.onCompleted();
      } catch (e) {
        throw e;
      } finally {
        this.dispose();
      }
    };

    AutoDetachObserverPrototype.setDisposable = function (value) { this.m.setDisposable(value); };
    AutoDetachObserverPrototype.getDisposable = function () { return this.m.getDisposable(); };

    AutoDetachObserverPrototype.dispose = function () {
      __super__.prototype.dispose.call(this);
      this.m.dispose();
    };

    return AutoDetachObserver;
  }(AbstractObserver));
