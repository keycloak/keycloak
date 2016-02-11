(function(){
  "use strict";
  var base64_arraybuffer = require('../lib/base64-arraybuffer.js');

  /*
  ======== A Handy Little Nodeunit Reference ========
  https://github.com/caolan/nodeunit

  Test methods:
    test.expect(numAssertions)
    test.done()
  Test assertions:
    test.ok(value, [message])
    test.equal(actual, expected, [message])
    test.notEqual(actual, expected, [message])
    test.deepEqual(actual, expected, [message])
    test.notDeepEqual(actual, expected, [message])
    test.strictEqual(actual, expected, [message])
    test.notStrictEqual(actual, expected, [message])
    test.throws(block, [error], [message])
    test.doesNotThrow(block, [error], [message])
    test.ifError(value)
*/


  function stringArrayBuffer(str) {
    var buffer = new ArrayBuffer(str.length);
    var bytes = new Uint8Array(buffer);

    str.split('').forEach(function(str, i) {
      bytes[i] = str.charCodeAt(0);
    });

    return buffer;
  }

  function testArrayBuffers(buffer1, buffer2) {
    var len1 = buffer1.byteLength,
    len2 = buffer2.byteLength;
    if (len1 !== len2) {
      console.log(buffer1, buffer2);
      return false;
    }

    for (var i = 0; i < len1; i++) {
      if (buffer1[i] !== buffer1[i]) {
        console.log(i, buffer1, buffer2);
        return false;
      }
    }
    return true;
  }

  exports['base64tests'] = {
    'encode': function(test) {
      test.expect(4);

      test.equal(base64_arraybuffer.encode(stringArrayBuffer("Hello world")), "SGVsbG8gd29ybGQ=", 'encode "Hello world"');
      test.equal(base64_arraybuffer.encode(stringArrayBuffer("Man")), 'TWFu', 'encode "Man"');
      test.equal(base64_arraybuffer.encode(stringArrayBuffer("Ma")), "TWE=", 'encode "Ma"');
      test.equal(base64_arraybuffer.encode(stringArrayBuffer("Hello worlds!")), "SGVsbG8gd29ybGRzIQ==", 'encode "Hello worlds!"');
      test.done();
    },
    'decode': function(test) {
      test.expect(3);
      test.ok(testArrayBuffers(base64_arraybuffer.decode("TWFu"), stringArrayBuffer("Man")), 'decode "Man"');
      test.ok(testArrayBuffers(base64_arraybuffer.decode("SGVsbG8gd29ybGQ="), stringArrayBuffer("Hello world")), 'decode "Hello world"');
      test.ok(testArrayBuffers(base64_arraybuffer.decode("SGVsbG8gd29ybGRzIQ=="), stringArrayBuffer("Hello worlds!")), 'decode "Hello worlds!"');
      test.done();
    }
  };
})();
