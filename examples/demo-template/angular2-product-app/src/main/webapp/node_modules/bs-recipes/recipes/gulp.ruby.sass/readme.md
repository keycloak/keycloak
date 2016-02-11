#Browsersync - Gulp &amp; Ruby SASS

## Installation/Usage:

To try this example, follow these 4 simple steps. 

**Step 1**: Clone this entire repo
```bash
$ git clone https://github.com/Browsersync/recipes.git bs-recipes
```

**Step 2**: Move into the directory containing this example
```bash
$ cd bs-recipes/recipes/gulp.ruby.sass
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



This example highlights both the stream support for injecting CSS, aswell
as the support for calling `reload` directly following html changes. 

We also need to filter out any source maps created by ruby-sass.

### Preview of `gulpfile.js`:
```js
var gulp         = require('gulp');
var browserSync  = require('browser-sync');
var filter       = require('gulp-filter');
var sass         = require('gulp-ruby-sass');
var sourcemaps   = require('gulp-sourcemaps');
var reload       = browserSync.reload;

var src = {
    scss: 'app/scss/*.scss',
    css:  'app/css',
    html: 'app/*.html'
};

/**
 * Kick off the sass stream with source maps + error handling
 */
function sassStream () {
    return sass('app/scss', {sourcemap: true})
        .on('error', function (err) {
            console.error('Error!', err.message);
        })
        .pipe(sourcemaps.write('./', {
            includeContent: false,
            sourceRoot: '/app/scss'
        }));
}

/**
 * Start the Browsersync Static Server + Watch files
 */
gulp.task('serve', ['sass'], function() {

    browserSync({
        server: "./app"
    });

    gulp.watch(src.scss, ['sass']);
    gulp.watch(src.html).on('change', reload);
});

/**
 * Compile sass, filter the results, inject CSS into all browsers
 */
gulp.task('sass', function() {
    return sassStream()
        .pipe(gulp.dest(src.css))
        .pipe(filter("**/*.css"))
        .pipe(reload({stream: true}));
});

/**
 * Default task
 */
gulp.task('default', ['serve']);
```

