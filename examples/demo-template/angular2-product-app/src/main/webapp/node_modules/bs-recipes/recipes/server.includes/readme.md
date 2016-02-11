#Browsersync - Server includes example

## Installation/Usage:

To try this example, follow these 4 simple steps. 

**Step 1**: Clone this entire repo
```bash
$ git clone https://github.com/Browsersync/recipes.git bs-recipes
```

**Step 2**: Move into the directory containing this example
```bash
$ cd bs-recipes/recipes/server.includes
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



### Preview of `app.js`:
```js
/**
 * Require Browsersync
 */
var browserSync = require('browser-sync').create();
var fs = require('fs');

/**
 * Run Browsersync with server config
 */
browserSync.init({
    server: 'app',
    files: ['app/*.html', 'app/css/*.css'],
    rewriteRules: [
        {
            match: /@include\("(.+?)"\)/g,
            fn: function (match, filename) {
                if (fs.existsSync(filename)) {
                    return fs.readFileSync(filename);
                } else {
                    return '<span style="color: red">'+filename+' could not be found</span>';
                }
            }
        }
    ]
});
```

