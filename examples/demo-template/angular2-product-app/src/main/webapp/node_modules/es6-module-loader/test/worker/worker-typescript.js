importScripts("../../node_modules/when/es6-shim/Promise.js",
             "../../dist/es6-module-loader-dev.src.js",
             "../../node_modules/typescript/lib/typescript.js");
System.transpiler = 'typescript';
System['import']('es6.js').then(function(m) {
  postMessage(m.p);
}, function(err) {
  console.error(err, err.stack);
});