
/*!
 * Connect - bodyParser
 * Copyright(c) 2010 Sencha Inc.
 * Copyright(c) 2011 TJ Holowaychuk
 * MIT Licensed
 */

/**
 * Module dependencies.
 */

var qs = require('qs')
  , formidable = require('formidable');

/**
 * Extract the mime type from the given request's
 * _Content-Type_ header.
 *
 * @param  {IncomingMessage} req
 * @return {String}
 * @api private
 */

function mime(req) {
  var str = req.headers['content-type'] || '';
  return str.split(';')[0];
}

/**
 * Parse request bodies.
 *
 * By default _application/json_, _application/x-www-form-urlencoded_,
 * and _multipart/form-data_ are supported, however you may map `connect.bodyParser.parse[contentType]`
 * to a function receiving `(req, options, callback)`.
 *
 * Examples:
 *
 *      connect.createServer(
 *          connect.bodyParser()
 *        , function(req, res) {
 *          res.end('viewing user ' + req.body.user.name);
 *        }
 *      );
 *
 *      $ curl -d 'user[name]=tj' http://localhost/
 *      $ curl -d '{"user":{"name":"tj"}}' -H "Content-Type: application/json" http://localhost/
 *
 * Multipart req.files:
 *
 *   As a security measure files are stored in a separate object, stored
 *   as `req.files`. This prevents attacks that may potentially alter
 *   filenames, and depending on the application gain access to restricted files.
 *
 * Multipart configuration:
 *
 *  The `options` passed are provided to each parser function.
 *  The _multipart/form-data_ parser merges these with formidable's
 *  IncomingForm object, allowing you to tweak the upload directory,
 *  size limits, etc. For example you may wish to retain the file extension
 *  and change the upload directory:
 *
 *      server.use(bodyParser({ uploadDir: '/www/mysite.com/uploads' }));
 *
 *  View [node-formidable](https://github.com/felixge/node-formidable) for more information.
 *
 *  If you wish to use formidable directly within your app, and do not
 *  desire this behaviour for multipart requests simply remove the
 *  parser: 
 *
 *     delete connect.bodyParser.parse['multipart/form-data'];
 *
 *  Or
 *
 *     delete express.bodyParser.parse['multipart/form-data'];
 *
 * @param {Object} options
 * @return {Function}
 * @api public
 */

exports = module.exports = function bodyParser(options){
  options = options || {};
  return function bodyParser(req, res, next) {
    if (req.body) return next();
    req.body = {};

    if ('GET' == req.method || 'HEAD' == req.method) return next();
    var parser = exports.parse[mime(req)];
    if (parser) {
      parser(req, options, next);
    } else {
      next();
    }
  }
};

/**
 * Parsers.
 */

exports.parse = {};

/**
 * Parse application/x-www-form-urlencoded.
 */

exports.parse['application/x-www-form-urlencoded'] = function(req, options, fn){
  var buf = '';
  req.setEncoding('utf8');
  req.on('data', function(chunk){ buf += chunk });
  req.on('end', function(){
    try {
      req.body = buf.length
        ? qs.parse(buf)
        : {};
      fn();
    } catch (err){
      fn(err);
    }
  });
};

/**
 * Parse application/json.
 */

exports.parse['application/json'] = function(req, options, fn){
  var buf = '';
  req.setEncoding('utf8');
  req.on('data', function(chunk){ buf += chunk });
  req.on('end', function(){
    try {
      req.body = buf.length
        ? JSON.parse(buf)
        : {};
      fn();
    } catch (err){
      fn(err);
    }
  });
};

/**
 * Parse multipart/form-data.
 *
 * TODO: make multiple support optional
 * TODO: revisit "error" flag if it's a formidable bug
 */

exports.parse['multipart/form-data'] = function(req, options, fn){
  var form = new formidable.IncomingForm
    , data = {}
    , files = {}
    , done;

  Object.keys(options).forEach(function(key){
    form[key] = options[key];
  });

  function ondata(name, val, data){
    if (Array.isArray(data[name])) {
      data[name].push(val);
    } else if (data[name]) {
      data[name] = [data[name], val];
    } else {
      data[name] = val;
    }
  }

  form.on('field', function(name, val){
    ondata(name, val, data);
  });

  form.on('file', function(name, val){
    ondata(name, val, files);
  });

  form.on('error', function(err){
    fn(err);
    done = true;
  });

  form.on('end', function(){
    if (done) return;
    try {
      req.body = qs.parse(data);
      req.files = qs.parse(files);
      fn();
    } catch (err) {
      fn(err);
    }
  });

  form.parse(req);
};