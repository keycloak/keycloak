/*
 * ES6 Module Declarative Linking Code - Dev Build Only
 */

  // 15.2.5.3 Module Linking Groups

  // 15.2.5.3.2 BuildLinkageGroups alternative implementation
  // Adjustments (also see https://bugs.ecmascript.org/show_bug.cgi?id=2755)
  // 1. groups is an already-interleaved array of group kinds
  // 2. load.groupIndex is set when this function runs
  // 3. load.groupIndex is the interleaved index ie 0 declarative, 1 dynamic, 2 declarative, ... (or starting with dynamic)
  function buildLinkageGroups(load, loads, groups) {
    groups[load.groupIndex] = groups[load.groupIndex] || [];

    // if the load already has a group index and its in its group, its already been done
    // this logic naturally handles cycles
    if (indexOf.call(groups[load.groupIndex], load) != -1)
      return;

    // now add it to the group to indicate its been seen
    groups[load.groupIndex].push(load);

    for (var i = 0, l = loads.length; i < l; i++) {
      var loadDep = loads[i];

      // dependencies not found are already linked
      for (var j = 0; j < load.dependencies.length; j++) {
        if (loadDep.name == load.dependencies[j].value) {
          // by definition all loads in linkset are loaded, not linked
          console.assert(loadDep.status == 'loaded', 'Load in linkSet not loaded!');

          // if it is a group transition, the index of the dependency has gone up
          // otherwise it is the same as the parent
          var loadDepGroupIndex = load.groupIndex + (loadDep.isDeclarative != load.isDeclarative);

          // the group index of an entry is always the maximum
          if (loadDep.groupIndex === undefined || loadDep.groupIndex < loadDepGroupIndex) {

            // if already in a group, remove from the old group
            if (loadDep.groupIndex !== undefined) {
              groups[loadDep.groupIndex].splice(indexOf.call(groups[loadDep.groupIndex], loadDep), 1);

              // if the old group is empty, then we have a mixed depndency cycle
              if (groups[loadDep.groupIndex].length == 0)
                throw new TypeError("Mixed dependency cycle detected");
            }

            loadDep.groupIndex = loadDepGroupIndex;
          }

          buildLinkageGroups(loadDep, loads, groups);
        }
      }
    }
  }

  // 15.2.5.4
  // declarative linking implementation
  function link(linkSet, linkError) {

    var loader = linkSet.loader;

    if (!linkSet.loads.length)
      return;

    // console.log('linking {' + logloads(linkSet.loads) + '}');
    // snapshot(loader);

    // 15.2.5.3.1 LinkageGroups alternative implementation

    // build all the groups
    // because the first load represents the top of the tree
    // for a given linkset, we can work down from there
    var groups = [];
    var startingLoad = linkSet.loads[0];
    startingLoad.groupIndex = 0;
    buildLinkageGroups(startingLoad, linkSet.loads, groups);

    // determine the kind of the bottom group
    var curGroupDeclarative = startingLoad.isDeclarative == groups.length % 2;

    // run through the groups from bottom to top
    for (var i = groups.length - 1; i >= 0; i--) {
      var group = groups[i];
      for (var j = 0; j < group.length; j++) {
        var load = group[j];

        // 15.2.5.5 LinkDeclarativeModules adjusted
        if (curGroupDeclarative) {
          linkDeclarativeModule(load, linkSet.loads, loader);
        }
        // 15.2.5.6 LinkDynamicModules adjusted
        else {
          var module = doDynamicExecute(linkSet, load, linkError);
          if (!module)
            return;
          load.module = {
            name: load.name,
            module: module
          };
          load.status = 'linked';
        }
        finishLoad(loader, load);
      }

      // alternative current kind for next loop
      curGroupDeclarative = !curGroupDeclarative;
    }
  }


  // custom module records for binding graph
  // store linking module records in a separate table
  function getOrCreateModuleRecord(name, loader) {
    var moduleRecords = loader.moduleRecords;
    return moduleRecords[name] || (moduleRecords[name] = {
      name: name,
      dependencies: [],
      module: new Module(), // start from an empty module and extend
      importers: []
    });
  }

  // custom declarative linking function
  function linkDeclarativeModule(load, loads, loader) {
    if (load.module)
      return;

    var module = load.module = getOrCreateModuleRecord(load.name, loader);
    var moduleObj = load.module.module;

    var registryEntry = load.declare.call(__global, function(name, value) {
      // NB This should be an Object.defineProperty, but that is very slow.
      //    By disaling this module write-protection we gain performance.
      //    It could be useful to allow an option to enable or disable this.
      module.locked = true;

      // export({name: value})
      if (typeof name == 'object') {
        for (var p in name)
          moduleObj[p] = name[p];
      }
      // export(name, value)
      else {
        moduleObj[name] = value;
      }

      for (var i = 0, l = module.importers.length; i < l; i++) {
        var importerModule = module.importers[i];
        if (!importerModule.locked) {
          for (var j = 0; j < importerModule.dependencies.length; ++j) {
            if (importerModule.dependencies[j] === module) {
              importerModule.setters[j](moduleObj);
            }
          }
        }
      }

      module.locked = false;
      return value;
    }, { id: load.name });

    // setup our setters and execution function
    module.setters = registryEntry.setters;
    module.execute = registryEntry.execute;

    // now link all the module dependencies
    // amending the depMap as we go
    for (var i = 0, l = load.dependencies.length; i < l; i++) {
      var depName = load.dependencies[i].value;
      var depModule = loader.modules[depName];

      // if dependency not already in the module registry
      // then try and link it now
      if (!depModule) {
        // get the dependency load record
        for (var j = 0; j < loads.length; j++) {
          if (loads[j].name != depName)
            continue;

          // only link if already not already started linking (stops at circular / dynamic)
          if (!loads[j].module) {
            linkDeclarativeModule(loads[j], loads, loader);
            depModule = loads[j].module;
          }
          // if circular, create the module record
          else {
            depModule = getOrCreateModuleRecord(depName, loader);
          }
        }
      }

      // only declarative modules have dynamic bindings
      if (depModule.importers) {
        module.dependencies.push(depModule);
        depModule.importers.push(module);
      }
      else {
        // track dynamic records as null module records as already linked
        module.dependencies.push(null);
      }

      // run the setter for this dependency
      if (module.setters[i])
        module.setters[i](depModule.module);
    }

    load.status = 'linked';
  }

  /*
   * Module Object non-exotic for ES5:
   *
   * module.module        bound module object
   * module.execute       execution function for module
   * module.dependencies  list of module objects for dependencies
   * See getOrCreateModuleRecord for all properties
   *
   */
  function doExecute(module) {
    try {
      module.execute.call(__global);
    }
    catch(e) {
      return e;
    }
  }

  // 15.2.5.5.1 LinkImports not implemented
  // 15.2.5.7 ResolveExportEntries not implemented
  // 15.2.5.8 ResolveExports not implemented
  // 15.2.5.9 ResolveExport not implemented
  // 15.2.5.10 ResolveImportEntries not implemented

  // 15.2.6.1
  function evaluateLoadedModule(loader, load) {
    console.assert(load.status == 'linked', 'is linked ' + load.name);

    doEnsureEvaluated(load.module, [], loader);
    return load.module.module;
  }

  // propogate execution errors
  // see https://bugs.ecmascript.org/show_bug.cgi?id=2993
  function doEnsureEvaluated(module, seen, loader) {
    var err = ensureEvaluated(module, seen, loader);
    if (err)
      throw err;
  }
  // 15.2.6.2 EnsureEvaluated adjusted
  function ensureEvaluated(module, seen, loader) {
    if (module.evaluated || !module.dependencies)
      return;

    seen.push(module);

    var deps = module.dependencies;
    var err;

    for (var i = 0, l = deps.length; i < l; i++) {
      var dep = deps[i];
      // dynamic dependencies are empty in module.dependencies
      // as they are already linked
      if (!dep)
        continue;
      if (indexOf.call(seen, dep) == -1) {
        err = ensureEvaluated(dep, seen, loader);
        // stop on error, see https://bugs.ecmascript.org/show_bug.cgi?id=2996
        if (err) {
          err = addToError(err, 'Error evaluating ' + dep.name);
          return err;
        }
      }
    }

    if (module.failed)
      return new Error('Module failed execution.');

    if (module.evaluated)
      return;

    module.evaluated = true;
    err = doExecute(module);
    if (err) {
      module.failed = true;
    }
    else if (Object.preventExtensions) {
      // spec variation
      // we don't create a new module here because it was created and ammended
      // we just disable further extensions instead
      Object.preventExtensions(module.module);
    }

    module.execute = undefined;
    return err;
  }
})();
