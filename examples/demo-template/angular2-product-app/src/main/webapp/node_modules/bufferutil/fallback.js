'use strict';

/*!
 * bufferutil: WebSocket buffer utils
 * Copyright(c) 2015 Einar Otto Stangvik <einaros@gmail.com>
 * MIT Licensed
 */

module.exports.BufferUtil = {
  merge: function(mergedBuffer, buffers) {
    for (var i = 0, offset = 0, l = buffers.length; i < l; ++i) {
      var buf = buffers[i];

      buf.copy(mergedBuffer, offset);
      offset += buf.length;
    }
  },

  mask: function(source, mask, output, offset, length) {
    var maskNum = mask.readUInt32LE(0, true)
      , i = 0
      , num;

    for (; i < length - 3; i += 4) {
      num = maskNum ^ source.readUInt32LE(i, true);

      if (num < 0) num = 4294967296 + num;
      output.writeUInt32LE(num, offset + i, true);
    }

    switch (length % 4) {
      case 3: output[offset + i + 2] = source[i + 2] ^ mask[2];
      case 2: output[offset + i + 1] = source[i + 1] ^ mask[1];
      case 1: output[offset + i] = source[i] ^ mask[0];
    }
  },

  unmask: function(data, mask) {
    var maskNum = mask.readUInt32LE(0, true)
      , length = data.length
      , i = 0
      , num;

    for (; i < length - 3; i += 4) {
      num = maskNum ^ data.readUInt32LE(i, true);

      if (num < 0) num = 4294967296 + num;
      data.writeUInt32LE(num, i, true);
    }

    switch (length % 4) {
      case 3: data[i + 2] = data[i + 2] ^ mask[2];
      case 2: data[i + 1] = data[i + 1] ^ mask[1];
      case 1: data[i] = data[i] ^ mask[0];
    }
  }
};
