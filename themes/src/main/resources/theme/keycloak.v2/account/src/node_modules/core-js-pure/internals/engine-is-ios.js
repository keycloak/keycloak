var userAgent = require('../internals/engine-user-agent');

module.exports = /(?:ipad|iphone|ipod).*applewebkit/i.test(userAgent);
