##Foxy 
[![Travis branch](https://img.shields.io/travis/shakyShane/foxy/master.svg?style=flat-square&label=linux)](https://travis-ci.org/shakyShane/foxy)
[![AppVeyor branch](https://img.shields.io/appveyor/ci/shakyshane/foxy/master.svg?style=flat-square&label=windows)]()

Proxy with response moddin'

##cli
First, install globally
```bash
$ npm install -g foxy
```

Then, if you wanted to proxy a local vhost such as `http://magento.dev` run:
```bash
$ foxy http://magento.dev
```

A random port will be used, but if you want to provide your own:
```bash
$ foxy http://magento.dev --port 3000
```

##api
```js
var foxy = require("foxy");

var proxy = foxy("http://localhost:3000").listen(8000);

// Now access the site through http://localhost:8000
```

Built-in middleware will re-write html on the fly to update any urls & there'll also be the option
for additional rules for the re-writing.

## Additional re-write rules

Let's say you want to change the text `Home Page` to be `Homepage Rocks`, you can do that easily by 
providing additional `rules`

```js
var foxy = require("foxy");

var config = {
    rules: [
        {
            match: /Home Page/g,
            fn: function () {
                return "Homepage Rocks"
            }
        }
    ]
};

var proxy = foxy("http://localhost:3000", config).listen(8000);
```

## white/black lists

You can force some paths to always be processed by the middlewares.

```js
var proxy = foxy("http://wordpress.dev", {
    whitelist: ["/wp-admin/admin-ajax.php"], // whitelist checked first
    blacklist: ["/wp-admin/**"],
}).listen(8000);
```

## proxyResponse middleware

You can add middleware-like functions which process the proxy response.

```js
var foxy = require("foxy");

var config = {
    proxyRes: [
        function(proxyRes, req, config) {
            // do something with the proxyRes object which comes from node-http-proxy
        }
    ]
};

var proxy = foxy("http://localhost:3000", config).listen(8000);
```

#TODO

- [ ] https


