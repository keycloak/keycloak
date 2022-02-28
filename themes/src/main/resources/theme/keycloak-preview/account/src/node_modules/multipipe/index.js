
/**
 * Module dependencies.
 */

var duplexer = require('duplexer2');
var PassThrough = require('stream').PassThrough;
var Readable = require('stream').PassThrough;
var objectAssign = require('object-assign');

/**
 * Slice reference.
 */

var slice = [].slice;

/**
 * Duplexer options.
 */

var defaultOpts = {
  bubbleErrors: false,
  objectMode: true
};

/**
 * Expose `pipe`.
 */

module.exports = pipe;

/**
 * Pipe.
 *
 * @param streams Array[Stream,...]
 * @param opts [Object]
 * @param cb [Function]
 * @return {Stream}
 * @api public
 */

function pipe(streams, opts, cb){
  if (!Array.isArray(streams)) {
    streams = slice.call(arguments);
    opts = null;
    cb = null;
  }

  var lastArg = streams[streams.length - 1];
  if ('function' == typeof lastArg) {
    cb = streams.splice(-1)[0];
    lastArg = streams[streams.length - 1];
  }
  if ('object' == typeof lastArg && typeof lastArg.pipe != 'function') {
    opts = streams.splice(-1)[0];
  }
  var first = streams[0];
  var last = streams[streams.length - 1];
  var ret;
  opts = objectAssign({}, defaultOpts, opts)

  if (!first) {
    if (cb) process.nextTick(cb);
    return new PassThrough(opts);
  }
  
  if (first.writable && last.readable) ret = duplexer(opts, first, last);
  else if (streams.length == 1) ret = new Readable(opts).wrap(streams[0]);
  else if (first.writable) ret = first;
  else if (last.readable) ret = last;
  else ret = new PassThrough(opts);
  
  streams.forEach(function(stream, i){
    var next = streams[i+1];
    if (next) stream.pipe(next);
    if (stream != ret) stream.on('error', ret.emit.bind(ret, 'error'));
  });

  if (cb) {
    var ended = false;
    ret.on('error', end);
    last.on('finish', function(){ end() });
    last.on('close', function(){ end() });
    function end(err){
      if (ended) return;
      ended = true;
      cb(err);
    }
  }

  return ret;
}

