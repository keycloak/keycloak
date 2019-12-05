beforeEach(function() {
  jasmine.addMatchers({
    toHaveClass: function(util, customEqualityTesters) {
      return {
        compare: function(actual, cls) {
          var pass = actual.hasClass(cls);
          return {
            pass: pass,
            message: "Expected '" + actual + "'" + (pass ? ' not ' : ' ') + "to have class '" + cls + "'."
          };
        }
      };
    }
  });
});
