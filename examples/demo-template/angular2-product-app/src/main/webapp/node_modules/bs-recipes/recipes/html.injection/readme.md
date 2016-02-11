#Browsersync - HTML/CSS injection example

## Installation/Usage:

To try this example, follow these 4 simple steps. 

**Step 1**: Clone this entire repo
```bash
$ git clone https://github.com/Browsersync/recipes.git bs-recipes
```

**Step 2**: Move into the directory containing this example
```bash
$ cd bs-recipes/recipes/html.injection
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



To see the live HTML injecting, along with CSS injection, simply perform changes to either `index.html` or `css/main.css`

### Preview of `app.js`:
```js
/**
 * Require Browsersync
 */
var bs = require('browser-sync').create();

/**
 * Run Browsersync with server config
 */
bs.init({
    server: "app",
    files: ["app/css/*.css"],
    plugins: [
        {
            module: "bs-html-injector",
            options: {
                files: ["app/*.html"]
            }
        }
    ]
});
```

