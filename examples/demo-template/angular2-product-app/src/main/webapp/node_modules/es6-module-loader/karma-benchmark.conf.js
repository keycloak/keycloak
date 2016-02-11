module.exports = function (config) {

  config.set({
    basePath: '',
    frameworks: ['benchmark'],
    files: [
      'dist/es6-module-loader.src.js',
      'test/perf.js'
    ],
    reporters: ['benchmark'],
    browsers: ['Chrome', 'Firefox'],

    browserDisconnectTimeout: 10000,
    browserDisconnectTolerance: 2,
    browserNoActivityTimeout: 30000,
    captureTimeout: 120000
  });

  if(process.env.TRAVIS){
    config.set({
      customLaunchers: {
        'TR_Chrome': {
          base: 'Chrome',
          flags: ['--no-sandbox']
        }
      },
      browsers: ['TR_Chrome', 'Firefox']
    });
  }

};
