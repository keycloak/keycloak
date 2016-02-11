var gulp        = require('gulp');
var browserSync = require('browser-sync');
var sass        = require('gulp-sass');
var reload      = browserSync.reload;
var through     = require("through2");

/**
 * A slow task
 */
gulp.task('slow1', function () {
    return gulp.src('./app/*.html')
        .pipe(slowStream());
});

/**
 * Another Slow task
 */
gulp.task('slow2', function () {
    return gulp.src('./app/*.html')
        .pipe(slowStream());
});

/**
 * Separate task for the reaction to a file change
 */
gulp.task('html-watch', ['slow1', 'slow2'], reload);

/**
 * Sass task for live injecting into all browsers
 */
gulp.task('sass', function () {
    return gulp.src('./app/scss/*.scss')
        .pipe(sass())
        .pipe(gulp.dest('./app/css'))
        .pipe(reload({stream: true}));
});

/**
 * Serve and watch the html files for changes
 */
gulp.task('default', function () {

    browserSync({server: './app'});

    gulp.watch('./app/scss/*.scss', ['sass']);
    gulp.watch('./app/*.html',      ['html-watch']);
});

/**
 * Simulate a slow task
 */
function slowStream () {
    return through.obj(function (file, enc, cb) {
        this.push(file);
        setTimeout(cb, 2000);
    });
}