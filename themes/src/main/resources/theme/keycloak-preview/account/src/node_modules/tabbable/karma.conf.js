module.exports = function(config) {
  config.set({
    basePath: '',
    frameworks: ['browserify', 'mocha'],
    files: [
      'test/**/*.test.js',
    ],
    preprocessors: {
      'test/**/*.test.js': ['browserify'],
    },
    browserify: {
      debug: true,
      transform: ['brfs'],
    },
    mochaReporter: {
      showDiff: true,
    },
    reporters: ['mocha'],
    port: 9876,
    colors: true,
    logLevel: config.LOG_INFO,
    autoWatch: true,
    browsers: ['Firefox'],
    singleRun: true,
    concurrency: Infinity,
  });
}
