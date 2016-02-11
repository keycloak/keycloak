
/**
 * Module dependencies.
 */

var connect = require('./');

var app = connect()
  .use(connect.logger('dev'))
  .use(function(req, res){
    var body = Array(3222).join('hey');
    res.setHeader('Content-Length', body.length);
    res.end(body);
  })
  .listen(3000);