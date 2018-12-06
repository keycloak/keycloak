/**
 * System configuration for Angular samples
 * Adjust as necessary for your application needs.
 */
(function (global) {
  System.config({
    paths: {
      // paths serve as alias
      'npm:': resourceUrl + '/node_modules/'
    },
    // map tells the System loader where to look for things
    map: {
      // our app is within the app folder
      'app': resourceUrl + '/app',
      'resources': resourceUrl,

      'react': 'npm:react/umd/react.development.js',
      'react-dom': 'npm:react-dom/umd/react-dom.development.js',
      'react-router-dom': 'npm:react-router-dom/umd/react-router-dom.js',
      
      'moment': 'npm:moment/min/moment-with-locales.min.js',
      
      'axios': 'npm:axios/dist/axios.min.js',
    },
    
    bundles: {
        "npm:rxjs-system-bundle/Rx.system.min.js": [
          "rxjs",
          "rxjs/*",
          "rxjs/operator/*",
          "rxjs/observable/*",
          "rxjs/scheduler/*",
          "rxjs/symbol/*",
          "rxjs/add/operator/*",
          "rxjs/add/observable/*",
          "rxjs/util/*"
        ]
      },
      
    // packages tells the System loader how to load when no filename and/or no extension
    packages: {
      app: {
        defaultExtension: 'js',
        meta: {
          './*.js': {
          }
        }
      },
      
      rxjs: {
        defaultExtension: false
      },
      
    }
  });
})(this);
