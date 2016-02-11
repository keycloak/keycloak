  /**
   * Concatenates all the observable sequences.  This takes in either an array or variable arguments to concatenate.
   * @returns {Observable} An observable sequence that contains the elements of each given sequence, in sequential order.
   */
  observableProto.concat = function () {
    var items = slice.call(arguments, 0);
    items.unshift(this);
    return observableConcat.apply(this, items);
  };
