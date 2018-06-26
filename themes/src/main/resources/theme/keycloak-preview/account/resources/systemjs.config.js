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
      
      // libraries that may be required by patternfly-ng, depending on which modules are loaded
      'lodash': 'npm:lodash/lodash.js',
      //  'angular-tree-component': 'npm:angular-tree-component/dist/angular-tree-component.umd.js',
      //  'ng2-dragula': 'npm:ng2-dragula/bundles/ng2-dragula.umd.js',
      //  '@swimlane/ngx-datatable': 'npm:@swimlane/ngx-datatable/release/index.js',
      //  'd3': 'npm:d3/dist/d3.js',
      //  'c3': 'npm:c3/c3.js',
      
      // We load only the needed submodules for better performance. Using the root 'patternfly-ng' would require all pf-ng depenencies.
      //'patternfly-ng': 'npm:patternfly-ng',
      //'patternfly-ng/empty-state': 'npm:patternfly-ng/empty-state',
      'patternfly-ng/navigation': 'npm:patternfly-ng/navigation',
      'patternfly-ng/utilities': 'npm:patternfly-ng/utilities',
  
      // ngx-bootstrap
      'ngx-bootstrap': 'npm:ngx-bootstrap/bundles/ngx-bootstrap.umd.min.js',
      'ngx-bootstrap/dropdown': 'npm:ngx-bootstrap/bundles/ngx-bootstrap.umd.min.js',
      'ngx-bootstrap/popover': 'npm:ngx-bootstrap/bundles/ngx-bootstrap.umd.min.js',
      'ngx-bootstrap/tooltip': 'npm:ngx-bootstrap/bundles/ngx-bootstrap.umd.min.js',
    
      // patternfly-ng currently requires us to install transpiler.  Need to get rid of this.
      'plugin-babel': 'npm:systemjs-plugin-babel/plugin-babel.js',
      'systemjs-babel-build': 'npm:systemjs-plugin-babel/systemjs-babel-browser.js'
    },
    
    transpiler: 'plugin-babel',
    
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
    
     // 'patternfly-ng': { main: './index.js', defaultExtension: 'js' },
     // 'patternfly-ng/empty-state': { main: './index.js', defaultExtension: 'js' },
      'patternfly-ng/navigation': { main: './index.js', defaultExtension: 'js' },
      'patternfly-ng/utilities': { main: './index.js', defaultExtension: 'js' },
    }
  });
})(this);
