  /**
   * Retrieves the value of a specified property from all elements in the Observable sequence.
   * @param {String} prop The property to pluck.
   * @returns {Observable} Returns a new Observable sequence of property values.
   */
  observableProto.pluck = function (prop) {
    return this.map(function (x) { return x[prop]; });
  };
