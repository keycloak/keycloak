'use strict';

/* PARAMETERS */
var NUM_MODULES = 1000;


    /*
      TREE LOOKS LIKE -

                           MODULE NUM_MODULES
                                   |
                                   |
      -------------------------------------  ... -----
      |                            |                 |
    MODULE 0              MODULE TREE_DEPTH         ...
      |                            |
    MODULE 1              MODULE TREE_DEPTH + 1
     ...                         ...
 MODULE TREE_DEPTH - 1   MODULE TREE_DEPTH * 2 - 1

THAT IS -
  DEPTH   =  TREE_DEPTH
  BREADTH =  FLOOR(NUM_MODULES / TREE_DEPTH)

    */

    /*

      PERF RESULTS
      ------------

      WITH 1000 modules,

      TREE_DEPTH    FULL LOAD TIME PER MODULE (including evaluation and defining)
          1          0.25ms
          10         0.27ms
          50         0.30ms
         100         0.35ms
         500         0.90ms
        1000         1.90ms

    */

////

System.set('module0', System.newModule({}));

function instantiateAtDepth(treeDepth) {
  return function (load) {
    var num = parseInt(load.name.substr(6));

    var deps = [];

    // the last module depends on all of them
    if (num == NUM_MODULES) {
      for (var i = 0; i < NUM_MODULES; i += treeDepth)
        deps.push('module' + i);
    }
    else if (num % treeDepth != treeDepth - 1)
      deps = ['module' + (num + 1)];

    return {
      deps: deps,
      execute: function () {
        eval(load.source);
        return System.newModule({});
      }
    };
  };
}

////

function hundredModulesBench(treeDepth) {

  benchmark('hundred modules depth ' + treeDepth, function (deferred) {

    // Generate range 0 to NUM_MODULES
    var range = Array.apply(null, { length: NUM_MODULES });

    Promise
      // Define all the modules
      .all(range.map(function (_, i) {
        if (!i) return;
        return System.define('module' + i, "function q() {} var p = 5;\n // non-trivial code");
      }))

      .then(function () {
        // Import the last one
        return System.import('module' + (NUM_MODULES - 1));
      })

      .then(function () {
        return Promise
          // Remove all the modules
          .all(range.map(function (_, i) {
            if (!i) return;
            return System.delete('module' + i);
          })
        );
      })

      // End of the bench
      .then(function () {
        deferred.resolve();
      })
      .catch(function (e) {
        setTimeout(function () {
          throw e;
        })
      })
    ;

  }, {
    defer: true,
    timeout: false,
    setup: function(){

      System.instantiate = instantiateAtDepth(treeDepth);

    }
  });

}

////

suite('System', function () {

  hundredModulesBench(1);
  hundredModulesBench(10);
  hundredModulesBench(50);
  hundredModulesBench(100);
  hundredModulesBench(500);
  hundredModulesBench(1000);

});
