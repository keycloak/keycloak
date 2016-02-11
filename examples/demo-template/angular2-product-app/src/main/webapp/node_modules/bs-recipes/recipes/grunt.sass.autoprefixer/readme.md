#Browsersync - Grunt, SASS &amp; Autoprefixer

## Installation/Usage:

To try this example, follow these 4 simple steps. 

**Step 1**: Clone this entire repo
```bash
$ git clone https://github.com/Browsersync/recipes.git bs-recipes
```

**Step 2**: Move into the directory containing this example
```bash
$ cd bs-recipes/recipes/grunt.sass.autoprefixer
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



This example shows how you can chain potentially slow-running tasks, but still achieve CSS
Injection. The trick, as seen below, is to use the `bsReload` task that now comes 
bundled with `grunt-browser-sync` since `2.1.0`

Don't forget the `spawn: false` option for the watch task - it's a requirement
that allows Browsersync to work correctly

```js
watch: {
    options: {
        spawn: false // Important, don't remove this!
    },
    files: 'app/**/*.scss',
    tasks: ['sass', 'autoprefixer', 'bsReload:css']
},
```


### Preview of `Gruntfile.js`:
```js
module.exports = function (grunt) {
    grunt.initConfig({
        dirs: {
            css:  "app/css",
            scss: "app/scss"
        },
        watch: {
            options: {
                spawn: false
            },
            sass: {
                files: '<%= dirs.scss %>/**/*.scss',
                tasks: ['sass', 'autoprefixer', 'bsReload:css']
            },
            html: {
                files: 'app/*.html',
                tasks: ['bsReload:all']
            }
        },
        sass: {
            dev: {
                files: {
                    '<%= dirs.css %>/main.css': '<%= dirs.scss %>/main.scss'
                }
            }
        },
        autoprefixer: {
            options: {
                browsers: ['last 5 versions', 'ie 8']
            },
            css: {
                src: '<%= dirs.css %>/main.css',
                dest: '<%= dirs.css %>/main.css'
            }
        },
        browserSync: {
            dev: {
                options: {
                    server: "./app",
                    background: true
                }
            }
        },
        bsReload: {
            css: {
                reload: "main.css"
            },
            all: {
                reload: true
            }
        }
    });

    // load npm tasks
    grunt.loadNpmTasks('grunt-contrib-sass');
    grunt.loadNpmTasks('grunt-autoprefixer');
    grunt.loadNpmTasks('grunt-browser-sync');
    grunt.loadNpmTasks('grunt-contrib-watch');

    // define default task
    grunt.registerTask('default', ['browserSync', 'watch']);
};
```

