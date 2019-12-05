var fs = require('fs');
var del = require('del');
var gulp = require('gulp');
var streamqueue = require('streamqueue');
var karma = require('karma').server;
var $ = require('gulp-load-plugins')();
var runSequence = require('run-sequence');
var conventionalRecommendedBump = require('conventional-recommended-bump');
var titleCase = require('title-case');

var config = {
  pkg : JSON.parse(fs.readFileSync('./package.json')),
  banner:
      '/*!\n' +
      ' * <%= pkg.name %>\n' +
      ' * <%= pkg.homepage %>\n' +
      ' * Version: <%= pkg.version %> - <%= timestamp %>\n' +
      ' * License: <%= pkg.license %>\n' +
      ' */\n\n\n'
};

gulp.task('default', ['build','test']);
gulp.task('build', ['scripts', 'styles']);
gulp.task('test', ['build', 'karma']);

gulp.task('watch', ['build','karma-watch'], function() {
  gulp.watch(['src/**/*.{js,html}'], ['build']);
});

gulp.task('clean', function(cb) {
  del(['dist', 'temp'], cb);
});

gulp.task('scripts', ['clean'], function() {

  var buildTemplates = function () {
    return gulp.src('src/**/*.html')
      .pipe($.minifyHtml({
             empty: true,
             spare: true,
             quotes: true
            }))
      .pipe($.angularTemplatecache({module: 'ui.select'}));
  };

  var buildLib = function(){
    return gulp.src(['src/common.js','src/*.js'])
      .pipe($.plumber({
        errorHandler: handleError
      }))
      .pipe($.concat('select_without_templates.js'))
      .pipe($.header('(function () { \n"use strict";\n'))
      .pipe($.footer('\n}());'))
      .pipe(gulp.dest('temp'))
      .pipe($.jshint())
      .pipe($.jshint.reporter('jshint-stylish'))
      .pipe($.jshint.reporter('fail'));
  };

  return streamqueue({objectMode: true }, buildLib(), buildTemplates())
    .pipe($.plumber({
      errorHandler: handleError
    }))
    .pipe($.concat('select.js'))
    .pipe($.header(config.banner, {
      timestamp: (new Date()).toISOString(), pkg: config.pkg
    }))
    .pipe(gulp.dest('dist'))
    .pipe($.sourcemaps.init())
    .pipe($.uglify({preserveComments: 'some'}))
    .pipe($.concat('select.min.js'))
    .pipe($.sourcemaps.write('./'))
    .pipe(gulp.dest('dist'));

});

gulp.task('styles', ['clean'], function() {

  return gulp.src(['src/common.css'], {base: 'src'})
    .pipe($.sourcemaps.init())
    .pipe($.header(config.banner, {
      timestamp: (new Date()).toISOString(), pkg: config.pkg
    }))
    .pipe($.concat('select.css'))
    .pipe(gulp.dest('dist'))
    .pipe($.minifyCss())
    .pipe($.concat('select.min.css'))
    .pipe($.sourcemaps.write('../dist', {debug: true}))
    .pipe(gulp.dest('dist'));

});

gulp.task('karma', ['build'], function() {
  karma.start({configFile : __dirname +'/karma.conf.js', singleRun: true});
});

gulp.task('karma-watch', ['build'], function() {
  karma.start({configFile :  __dirname +'/karma.conf.js', singleRun: false});
});

gulp.task('pull', function(done) {
  $.git.pull();
  done();
});

gulp.task('add', function(done) {
  $.git.add();
  done();
});

gulp.task('recommendedBump', function(done) {
  /**
   * Bumping version number and tagging the repository with it.
   * Please read http://semver.org/
   *
   * To bump the version numbers accordingly after you did a patch,
   * introduced a feature or made a backwards-incompatible release.
   */

  conventionalRecommendedBump({preset: 'angular'}, function(err, importance) {
    // Get all the files to bump version in
    gulp.src(['./package.json'])
      .pipe($.bump({type: importance}))
      .pipe(gulp.dest('./'));

    done();
  });
});

gulp.task('changelog', function() {

  return gulp.src('CHANGELOG.md', {buffer: false})
    .pipe($.conventionalChangelog({preset: 'angular'}))
    .pipe(gulp.dest('./'));
});

gulp.task('push', function(done) {
  $.git.push('origin', 'master', {args: '--follow-tags'});
  done();
});

gulp.task('commit', function() {
  return gulp.src('./')
    .pipe($.git.commit('chore(release): bump package version and update changelog', {emitData: true}))
    .on('data', function(data) {
      console.log(data);
    });
});

gulp.task('tag', function() {
  return gulp.src('package.json')
    .pipe($.tagVersion());
});

gulp.task('bump', function(done) {
  runSequence('recommendedBump', 'changelog', 'add', 'commit', 'tag', 'push', done);
});

gulp.task('docs', function (cb) {
  runSequence('docs:clean', 'docs:examples', 'docs:assets', 'docs:index', cb);
});

gulp.task('docs:clean', function (cb) {
  del(['docs-built'], cb)
});

gulp.task('docs:assets', function () {
  gulp.src('./dist/*').pipe(gulp.dest('./docs-built/dist'));
  return gulp.src('docs/assets/*').pipe(gulp.dest('./docs-built/assets'));
});

gulp.task('docs:examples', function () {
  // Need a way to reset filename list: $.filenames('exampleFiles',{overrideMode:true});
  return gulp.src(['docs/examples/*.html'])
    .pipe($.header(fs.readFileSync('docs/partials/_header.html')))
    .pipe($.footer(fs.readFileSync('docs/partials/_footer.html')))
    .pipe($.filenames('exampleFiles'))
    .pipe(gulp.dest('./docs-built/'));
});

gulp.task('docs:index', function () {

  var exampleFiles = $.filenames.get('exampleFiles');
  exampleFiles = exampleFiles.map(function (filename) {
    var cleaned = titleCase(filename.replace('demo-', '').replace('.html', ''));
    return '<h4><a href="./' + filename + '">' + cleaned + '</a> <plnkr-opener example-path="' + filename + '"></plnkr-opener></h4>';
  });

  return gulp.src('docs/index.html')
    .pipe($.replace('<!-- INSERT EXAMPLES HERE -->', exampleFiles.join("\n")))
    .pipe(gulp.dest('./docs-built/'));
});

gulp.task('docs:watch', ['docs'], function() {
  gulp.watch(['docs/**/*.{js,html}'], ['docs']);
});

var handleError = function (err) {
  console.log(err.toString());
  this.emit('end');
};
