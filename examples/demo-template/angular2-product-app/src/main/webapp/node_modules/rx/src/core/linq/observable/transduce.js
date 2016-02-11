  /**
   * Executes a transducer to transform the observable sequence
   * @param {Transducer} transducer A transducer to execute
   * @returns {Observable} An Observable sequence containing the results from the transducer.
   */
  observableProto.transduce = function(transducer) {
    var source = this;

    function transformForObserver(observer) {
      return {
        init: function() {
          return observer;
        },
        step: function(obs, input) {
          return obs.onNext(input);
        },
        result: function(obs) {
          return obs.onCompleted();
        }
      };
    }

    return new AnonymousObservable(function(observer) {
      var xform = transducer(transformForObserver(observer));
      return source.subscribe(
        function(v) {
          try {
            xform.step(observer, v);
          } catch (e) {
            observer.onError(e);
          }
        },
        observer.onError.bind(observer),
        function() { xform.result(observer); }
      );
    }, source);
  };
