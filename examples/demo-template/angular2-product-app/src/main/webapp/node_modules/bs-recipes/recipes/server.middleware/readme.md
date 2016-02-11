#Browsersync - Server + Logging + History API fallback middlewares Example

## Installation/Usage:

To try this example, follow these 4 simple steps. 

**Step 1**: Clone this entire repo
```bash
$ git clone https://github.com/Browsersync/recipes.git bs-recipes
```

**Step 2**: Move into the directory containing this example
```bash
$ cd bs-recipes/recipes/server.middleware
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



This example adds the [connect-logger](https://www.npmjs.com/package/connect-logger) middleware

![Logger](http://f.cl.ly/items/3i2G451L3O3R182b3p14/Screen%20Shot%202015-02-18%20at%2016.02.59.png)

### Preview of `app.js`:
```js
/**
 * Require Browsersync
 */
var browserSync = require('browser-sync').create();
var historyApiFallback = require('connect-history-api-fallback')

/**
 * Run Browsersync with server config
 */
browserSync.init({
    server: "app",
    files: ["app/*.html", "app/css/*.css"],
    middleware: [require("connect-logger")(), historyApiFallback()]
});
```

