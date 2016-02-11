'use strict';

var util = require('util');
var pkg = require('./package.json');
var extend = util._extend;
var geSaLaKaCuLa = require('gesalakacula');

// No Karma options are passed after the double dash option (`--`)
// Example : karma start --single-run -- --polyfill
//        >> { _: [], polyfill: true }

var _argv = process.argv;
var argv = require('minimist')(_argv.slice(_argv.indexOf('--') + 1));


var options = extend({
  travis: process.env.TRAVIS,
  polyfill: false,
  saucelabs: false,
  ie8: false,
  coverage: false
}, argv);

if (options.ie8) {
  console.log('IE8 Mode !\n - polyfill required\n');
  options.polyfill = true;
}
if (options.saucelabs) {
  options.polyfill = true;
}

////
module.exports = function(config) {

  var files = [
    'test/_helper.js',
    [options['babel'] ? 'node_modules/regenerator/runtime.js' : ''],

    [!options.ie8 
        ? (options['babel'] 
            ? 'node_modules/babel-core/browser.js' 
            : options['typescript']  
                ? 'node_modules/typescript/lib/typescript.js' 
                : 'node_modules/traceur/bin/traceur.js') 
        : ''],

    [options.polyfill ? 'node_modules/when/es6-shim/Promise.js' : ''],
    [options.polyfill ? 'dist/es6-module-loader-dev.js' : 'dist/es6-module-loader-dev.src.js'],

    'test/_browser.js',
    'test/browser-script-type-module.js',
    'test/custom-loader.js',

    [!options.ie8 ? 'test/*.spec.js' : ['test/system.normalize.spec.js', 'test/custom-loader.spec.js']],

    {pattern: 'test/{loader,loads,syntax,worker}/**/*', included: false},
    {pattern: 'node_modules/traceur/bin/traceur.js', included: false},
    {pattern: 'node_modules/babel-core/browser.js', included: false},
    {pattern: 'node_modules/typescript/lib/typescript.js', included: false},
    {pattern: 'node_modules/when/es6-shim/Promise.js', included: false},
    {pattern: 'dist/es6-module-loader*.js', included: false}
  ];
  // Default Config
  config.set({
    basePath: '',
    frameworks: ['mocha', 'expect'],
    files: flatten(files),
    reporters: ['mocha'],
    browsers: ['Chrome', 'Firefox'],
    client: {
      mocha: {
        reporter: 'html',
        timeout: 8000
      },
      system: {
        ie8: options.ie8,
        transpiler: options.babel
            ? 'babel'
            : options.typescript
                ? 'typescript'
                : 'traceur'
      }
    }
  });

  if (options.coverage) {
    config.set({
      reporters: ['mocha', 'coverage'],
      preprocessors: {
        'dist/es6-module-loader*.src.js': ['coverage']
      },
      coverageReporter: {
        type : 'html',
        dir : 'coverage/'
      },
      browsers: ['Chrome']
    });
  }

  if (options.travis) {
    // TRAVIS config overwrite
    config.set({
      singleRun: true,
      reporters: ['dots'],
      customLaunchers: {
        'TR_Chrome': {
          base: 'Chrome',
          flags: ['--no-sandbox']
        }
      },
      browsers: ['TR_Chrome', 'Firefox']
    });
  }

  if (options.saucelabs) {

    var customLaunchers = geSaLaKaCuLa({
      'Windows 7': {
        'internet explorer': '9..11'
      }
    });

    if (options.ie8) {
      customLaunchers = geSaLaKaCuLa({
        'Windows 7': {
          'internet explorer': '8'
        }
      });
    }

    // saucelabs still fail sporadically
    customLaunchers = undefined;

    var now = new Date();
    var buildData = options.travis ?
    {
      location: 'TRAVIS',
      name: process.env.TRAVIS_JOB_NUMBER,
      id: process.env.TRAVIS_BUILD_ID
    }
      :
    {
      location: 'LOCAL',
      name: now.toString(),
      id: +now
    };
    var build = util.format('%s #%s (%s)',
      buildData.location, buildData.name, buildData.id);

    console.log('SauceLabs Run\n- Build : ' + build + '\n');

    config.set({
      reporters: ['dots', 'saucelabs'],

      browserDisconnectTimeout: 10000,
      browserDisconnectTolerance: 2,
      browserNoActivityTimeout: 30000,
      captureTimeout: 120000,

      sauceLabs: {
        testName: pkg.name,
        recordScreenshots: false,
        build: build,
        tunnelIdentifier: options.travis ?
          process.env.TRAVIS_JOB_NUMBER : Math.floor(Math.random() * 1000)
      }
    });

    if (customLaunchers)
      config.set({
        browsers: Object.keys(customLaunchers),
        customLaunchers: customLaunchers
      });
  }
};

function flatten(arr) {
  return arr.reduce(function(memo, val) {
    return memo.concat(util.isArray(val) ? flatten(val) : val ? [val] : []);
  }, []);
}
