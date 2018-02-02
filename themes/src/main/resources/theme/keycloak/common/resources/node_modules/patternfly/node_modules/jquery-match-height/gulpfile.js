// TODO: add more test specs (see matchHeight.spec.js)
// TODO: travis CI

var gulp = require('gulp');
var uglify = require('gulp-uglify');
var rename = require('gulp-rename');
var header = require('gulp-header');
var eslint = require('gulp-eslint');
var gulpBump = require('gulp-bump');
var changelog = require('gulp-conventional-changelog');
var tag = require('gulp-tag-version');
var release = require('gulp-github-release');
var sequence = require('run-sequence');
var gutil = require('gulp-util');
var replace = require('gulp-replace');
var webdriver = require('gulp-webdriver');
var webserver = require('gulp-webserver');
var selenium = require('selenium-standalone');
var ngrok = require('ngrok');
var staticTransform = require('connect-static-transform');
var pkg = require('./package.json');
var extend = require('util')._extend;
var exec = require('child_process').exec;
var fs = require('fs');
var buildDirectory = 'dist';
var server;

gulp.task('release', function(callback) {
    var type = process.argv[4] || 'minor';
    sequence('lint', 'test', 'build', 'bump:' + type, 'changelog', 'tag', callback);
});

gulp.task('release:push', function(callback) {
    sequence('release:push:git', 'release:push:github', 'release:push:npm', callback);
});

gulp.task('release:push:github', function(callback) {
    return gulp.src([
            'CHANGELOG.md',
            'LICENSE', 
            buildDirectory + '/jquery.matchHeight-min.js', 
            buildDirectory + '/jquery.matchHeight.js'
        ])
        .pipe(release({
          owner: 'liabru',
          repo: pkg.name,
          tag: pkg.version,
          name: 'jquery.matchHeight.js ' + pkg.version
        }));
});

gulp.task('release:push:git', function(callback) {
    shell('git push', callback);
});

gulp.task('release:push:npm', function(callback) {
    shell('npm publish', callback);
});

gulp.task('build', function() {
    var build = extend(pkg);
    build.version = process.argv[4] || pkg.version;

    gulp.src('jquery.matchHeight.js')
        .pipe(replace("jquery-match-height master", "jquery-match-height " + build.version))
        .pipe(replace("version = 'master'", "version = '" + build.version + "'"))
        .pipe(gulp.dest(buildDirectory));

    return gulp.src('jquery.matchHeight.js')
        .pipe(replace("version = 'master'", "version = '" + build.version + "'"))
        .pipe(uglify({ output: { max_line_len: 500 } }))
        .pipe(header(banner, { build: build }))
        .pipe(rename({ suffix: '-min' }))
        .pipe(gulp.dest(buildDirectory));
});

gulp.task('lint', function() {
    return gulp.src(pkg.main)
        .pipe(eslint())
        .pipe(eslint.format())
        .pipe(eslint.failAfterError());
});

var bump = function(options) {
    return gulp.src(['package.json', 'bower.json'])
        .pipe(gulpBump(options))
        .pipe(gulp.dest('.'));
};

gulp.task('bump:patch', function() {
    return bump({ type: 'patch' });
});

gulp.task('bump:minor', function() {
    return bump({ type: 'minor' });
});

gulp.task('bump:major', function() {
    return bump({ type: 'major' });
});

gulp.task('tag', function() {
    return gulp.src('package.json')
        .pipe(tag({ prefix: '' }));
});

gulp.task('changelog', function () {
    return gulp.src('CHANGELOG.md')
        .pipe(changelog())
        .pipe(gulp.dest('.'));
});

gulp.task('serve', function() {
    serve(false);
});

gulp.task('serve:test', function() {
    serve(true);
});

gulp.task('serve:stop', function() {
    if (server) {
        try {
            server.emit('kill');
        } catch (e) {} // eslint-disable-line no-empty
        gutil.log('Web server stopped');
    }
});

gulp.task('selenium', function(done) {
    var start = function(err) {
        gutil.log('Starting Selenium server...');
        selenium.start(function(err, child) {
            gutil.log('Selenium server started');
            selenium.child = child;
            done(err);
        });
    };

    try {
        fs.statSync('node_modules/selenium-standalone/.selenium');
        start();
    } catch (e) {
        gutil.log('Setting up Selenium server...');
        selenium.install({
            logger: function(message) { gutil.log(message); }
        }, function(err) {
            start(err);
        });
    }
});

gulp.task('test', function(done) {
    sequence('lint', 'serve:test', 'selenium', function() {
        var error;
        gutil.log('Starting webdriver...');

        var finish = function(err) {
            gutil.log('Webdriver stopped');
            selenium.child.kill();
            gutil.log('Selenium server stopped');
            gulp.start('serve:stop');
            done(error || err);
        };

        gulp.src('test/conf/local.conf.js')
            .pipe(webdriver({
                baseUrl: 'http://localhost:8000'
            }))
            .on('error', function(err) { 
                console.error(err);
                error = err; 
            })
            .on('finish', finish);
    });
});

gulp.task('test:cloud', ['lint', 'serve:test'], function(done) {
    ngrok.connect({
        authtoken: null,
        port: 8000
    }, function (err, url) {
        gutil.log('Tunnel started', url);
        gulp.src('test/conf/cloud.conf.js')
        .pipe(webdriver({
            baseUrl: url
        }))
        .on('finish', function(err) {
            if (server) {
                ngrok.disconnect();
                ngrok.kill();
                gutil.log('Tunnel stopped');
                gulp.start('serve:stop');
            }
            done(err);
        });
    });
});

gulp.task('test:cloud:all', ['lint', 'serve:test'], function(done) {
    ngrok.connect({
        authtoken: null,
        port: 8000
    }, function (err, url) {
        gutil.log('Tunnel started', url);
        gulp.src('test/conf/cloud-all.conf.js')
        .pipe(webdriver({
            baseUrl: url
        }))
        .on('finish', function(err) {
            if (server) {
                ngrok.disconnect();
                ngrok.kill();
                gutil.log('Tunnel stopped');
                gulp.start('serve:stop');
            }
            done(err);
        });
    });
});

var serve = function(isTest) {
    process.on('uncaughtException', function(err) {
        if (err.errno === 'EADDRINUSE') {
            gutil.log('Server already running (or port is otherwise in use)');
        }
    });

    server = gulp.src('.')
        .pipe(webserver({
            host: '0.0.0.0',
            livereload: !isTest,
            middleware: function(req, res, next) {
                var ieMode = (req._parsedUrl.query || '').replace('=','');
                if (ieMode in emulateIEMiddleware) {
                    emulateIEMiddleware[ieMode](req, res, next);
                } else {
                    next();
                }
            },
            open: isTest ? false : 'http://localhost:8000/test/page/test.html',
            directoryListing: true
        }));
};

var banner = [
  '/*',
  '* <%= build.name %> <%= build.version %> by @liabru',
  '* <%= build.homepage %>',
  '* License <%= build.license %>',
  '*/',
  ''
].join('\n');

var emulateIEMiddlewareFactory = function(version) {
    return staticTransform({
        root: __dirname,
        match: /(.+)\.html/,
        transform: function (path, text, send) {
            send(text.replace('content="IE=edge,chrome=1"', 'content="IE=' + version + '"'));
        }
    });
};

var emulateIEMiddleware = {
    'ie8': emulateIEMiddlewareFactory(8),
    'ie9': emulateIEMiddlewareFactory(9),
    'ie10': emulateIEMiddlewareFactory(10)
};

var shell = function(command, callback) {
    var args = process.argv.slice(3).join(' '),
        proc = exec(command + ' ' + args, function(err) {
            callback(err);
        });

    proc.stdout.on('data', function(data) {
        process.stdout.write(data);
    });

    proc.stderr.on('data', function(data) {
        process.stderr.write(data);
    });
};