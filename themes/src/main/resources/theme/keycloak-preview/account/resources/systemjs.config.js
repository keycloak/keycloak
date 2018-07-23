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

      // angular bundles
      '@angular/core': 'npm:@angular/core/bundles/core.umd.min.js',
      '@angular/common': 'npm:@angular/common/bundles/common.umd.min.js',
      '@angular/compiler': 'npm:@angular/compiler/bundles/compiler.umd.min.js',
      '@angular/platform-browser': 'npm:@angular/platform-browser/bundles/platform-browser.umd.min.js',
      '@angular/platform-browser-dynamic': 'npm:@angular/platform-browser-dynamic/bundles/platform-browser-dynamic.umd.min.js',
      '@angular/http': 'npm:@angular/http/bundles/http.umd.min.js',
      '@angular/router': 'npm:@angular/router/bundles/router.umd.min.js',
      '@angular/forms': 'npm:@angular/forms/bundles/forms.umd.min.js',
      
      // other libraries
      '@ngx-translate/core':       'npm:@ngx-translate/core/bundles/core.umd.min.js',
      'angular-in-memory-web-api': 'npm:angular-in-memory-web-api/bundles/in-memory-web-api.umd.js',
      'lodash': 'npm:lodash/lodash.js',
      
      // patternfly-ng
      'patternfly-ng/navigation': 'npm:patternfly-ng/bundles/patternfly-ng.umd.min.js',
      'patternfly-ng/utilities': 'npm:patternfly-ng/bundles/patternfly-ng.umd.min.js',
      'patternfly-ng/notification': 'npm:patternfly-ng/bundles/patternfly-ng.umd.min.js',
      'patternfly-ng/notification/inline-notification': 'npm:patternfly-ng/bundles/patternfly-ng.umd.min.js',
      'patternfly-ng/notification/notification-service': 'npm:patternfly-ng/bundles/patternfly-ng.umd.min.js',
      
      // unused patternfly-ng dependencies
      'angular-tree-component': '@empty',
      'ng2-dragula': '@empty',
      '@swimlane/ngx-datatable': '@empty',
      'd3': '@empty',
      'c3': '@empty',
  
      // ngx-bootstrap
      'ngx-bootstrap': 'npm:ngx-bootstrap/bundles/ngx-bootstrap.umd.min.js',
      'ngx-bootstrap/dropdown': 'npm:ngx-bootstrap/bundles/ngx-bootstrap.umd.min.js',
      'ngx-bootstrap/popover': 'npm:ngx-bootstrap/bundles/ngx-bootstrap.umd.min.js',
      'ngx-bootstrap/tooltip': 'npm:ngx-bootstrap/bundles/ngx-bootstrap.umd.min.js',
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
            loader: resourceUrl + '/systemjs-angular-loader.js'
          }
        }
      },
      '@ngx-translate/core': {
          defaultExtension: 'js'
      },
      rxjs: {
        defaultExtension: false
      },
      
      'lodash': { defaultExtension: 'js' },
    }
  });
})(this);
