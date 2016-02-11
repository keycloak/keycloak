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