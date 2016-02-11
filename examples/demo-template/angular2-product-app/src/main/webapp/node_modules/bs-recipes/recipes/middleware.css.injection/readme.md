#Browsersync - Middleware + CSS example

## Installation/Usage:

To try this example, follow these 4 simple steps. 

**Step 1**: Clone this entire repo
```bash
$ git clone https://github.com/Browsersync/recipes.git bs-recipes
```

**Step 2**: Move into the directory containing this example
```bash
$ cd bs-recipes/recipes/middleware.css.injection
```

**Step 3**: Install dependencies
```bash
$ npm install
```

**Step 4**: Run the example
```bash
$ npm start
```

### Additional Info:

- Perform changes to `app/css/main.less` to see live css injection

### Preview of `app.js`:
```js
/**
 * Require Browsersync
 */
var browserSync = require("browser-sync");

/**
 * Run the middleware on files that contain .less
 */
function lessMiddleware (req, res, next) {
    var parsed = require("url").parse(req.url);
    if (parsed.pathname.match(/\.less$/)) {
        return less(parsed.pathname).then(function (o) {
            res.setHeader('Content-Type', 'text/css');
            res.end(o.css);
        });
    }
    next();
}

/**
 * Compile less
 */
function less(src) {
    var f = require('fs').readFileSync('app' + src).toString();
    return require('less').render(f);
}

/**
 * Run Browsersync with less middleware
 */
browserSync({
    files: "app/css/*.less",
    server: "app",
    injectFileTypes: ["less"],
    /**
     * Catch all requests, if any are for .less files, recompile on the fly and
     * send back a CSS response
     */
    middleware: lessMiddleware
});

```

