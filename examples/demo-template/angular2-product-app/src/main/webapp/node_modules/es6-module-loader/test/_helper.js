

(function (__global){
  'use strict';

  if(!__global.console){
    __global.console = { log : __global.dump || function (){} };
  }

  var isWindows = typeof process != 'undefined' && process.platform.match(/^win/);

  var baseURI;
  // environent baseURI detection
  if (typeof document != 'undefined' && document.getElementsByTagName) {
    baseURI = document.baseURI;

    if (!baseURI) {
      var bases = document.getElementsByTagName('base');
      baseURI = bases[0] && bases[0].href || window.location.href;
    }

    // sanitize out the hash and querystring
    baseURI = baseURI.split('#')[0].split('?')[0];
    baseURI = baseURI.substr(0, baseURI.lastIndexOf('/') + 1);
  }
  else if (typeof process != 'undefined' && process.cwd) {
    baseURI = 'file://' + (isWindows ? '/' : '') + process.cwd() + '/';
    if (isWindows)
      baseURI = baseURI.replace(/\\/g, '/');
  }
  else if (typeof location != 'undefined') {
    baseURI = __global.location.href;
  }
  else {
    throw new TypeError('No environment baseURI');
  }

  // baseURI - the current path, for standard relative normalization (./x)
  __global.baseURI = baseURI;

  // baseURL - the base path, for plain relative normalization (x)
  __global.baseURL = baseURI;

  /**
   * Describe a block if the bool is true.
   * Will skip it otherwise.
   * @param bool
   * @returns {Function} describe or describe.skip
   */
  function describeIf(bool) {
    return (bool ? describe : describe.skip)
      .apply(null, Array.prototype.slice.call(arguments, 1));
  }

  __global.describeIf = describeIf;

}(typeof window != 'undefined' ? window : global));


