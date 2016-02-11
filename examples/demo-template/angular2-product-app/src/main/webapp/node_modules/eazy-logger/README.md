##eazy-logger [![Build Status](https://travis-ci.org/shakyShane/easy-logger.svg?branch=master)](https://travis-ci.org/shakyShane/easy-logger) [![Coverage Status](https://img.shields.io/coveralls/shakyShane/easy-logger.svg)](https://coveralls.io/r/shakyShane/easy-logger?branch=master)

[tFunk](https://github.com/shakyShane/tfunk) + [String Substitution](http://nodejs.org/docs/latest/api/console.html#console_console_log_data)

![preview](http://cl.ly/image/3K2E2d111l15/Screen%20Shot%202014-09-17%20at%2020.49.17.png)

##Install

```
$ npm install eazy-logger --save
```

##Usage

```js
var logger  = require("eazy-logger").Logger({
    prefix: "{blue:[}{magenta:easy-logger}{blue:] }",
    useLevelPrefixes: true
});
```

```js
/**
 * Standard loggers + prefixes
 */
logger.debug("Debugging Msg");
logger.info("Info statement");
logger.warn("A little warning with string %s", "substitution");
logger.error("an error occurred in file: {red:%s}", "/users/awesomedev/file.js");
```

```js
/**
 * Use string substitution + colours
 */
logger.log("error", "Use {green:built-in} %s", "String substitution");
```

```js
/**
 * Set an option for the next log statement only
 */
logger.setOnce("useLevelPrefixes", true).warn("Use {green:built-in} %s", "String substitution");
```