  var Producer = Rx.internals.Producer = (function (__super__) {

    inherits(Producer, __super__);

    function subscribe(observer) {
      var sink = new SingleAssignmentDisposable(),
          subscription = new SingleAssignmentDisposable();

      function setDisposable(s) {
        sink.setDisposable(s);
      }

      if (currentThreadScheduler.scheduleRequired()) {
        currentThreadScheduler.scheduleWithState(this, function (_, me) {
          subscription.setDisposable(me.run(observer, subscription, setDisposable));
        });
      } else {
        subscription.setDisposable(this.run(observer, subscription, setDisposable));
      }

      return new CompositeDisposable(sink, subscription);
    }

    function Producer() {
      __super__.call(this, subscribe);
    }

    return Producer;

  }(Observable));
