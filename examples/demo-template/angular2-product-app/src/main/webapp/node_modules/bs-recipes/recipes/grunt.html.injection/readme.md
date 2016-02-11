#Browsersync - Grunt, SASS, HTML/CSS injection example

## Installation/Usage:

To try this example, follow these 4 simple steps. 

**Step 1**: Clone this entire repo
```bash
$ git clone https://github.com/Browsersync/recipes.git bs-recipes
```

**Step 2**: Move into the directory containing this example
```bash
$ cd bs-recipes/recipes/grunt.html.injection
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

### Preview of `Gruntfile.js`:
```js
// This shows a full config file!
module.exports = function (grunt) {
    grunt.initConfig({
        watch: {
            files: 'app/scss/**/*.scss',
            tasks: ['bsReload:css']
        },
        sass: {
            dev: {
                files: {
                    'app/css/main.css': 'app/scss/main.scss'
                }
            }
        },
        browserSync: {
            dev: {
                options: {
                    watchTask: true,
                    server: './app',
                    plugins: [
                        {
                            module: "bs-html-injector",
                            options: {
                                files: "./app/*.html"
                            }
                        }
                    ]
                }
            }
        },
        bsReload: {
            css: "main.css"
        }
    });

    // load npm tasks
    grunt.loadNpmTasks('grunt-contrib-sass');
    grunt.loadNpmTasks('grunt-contrib-watch');
    grunt.loadNpmTasks('grunt-browser-sync');

    // define default task
    grunt.registerTask('default', ['browserSync', 'watch']);
};
```

