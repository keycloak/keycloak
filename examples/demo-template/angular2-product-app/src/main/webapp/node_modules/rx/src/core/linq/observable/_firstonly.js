  function firstOnly(x) {
    if (x.length === 0) { throw new Error(sequenceContainsNoElements); }
    return x[0];
  }
