/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

//----------------------------------------------------------------------------
// an implementation of the require() function as specified for use with
// CommonJS Modules - see http://commonjs.org/specs/modules/1.0.html
//----------------------------------------------------------------------------

//----------------------------------------------------------------------------
// inspired from David Flanagan's require() function documented here:
// http://www.davidflanagan.com/2009/11/a-module-loader.html
//----------------------------------------------------------------------------

//----------------------------------------------------------------------------
// only supports "preloaded" modules ala define() (AMD)
//    http://wiki.commonjs.org/wiki/Modules/AsynchronousDefinition
// but the id parameter is required
//----------------------------------------------------------------------------

//----------------------------------------------------------------------------
// function wrapper
//----------------------------------------------------------------------------
(function(){

//----------------------------------------------------------------------------
// some constants
//----------------------------------------------------------------------------
var PROGRAM = "modjewel"
var VERSION = "2.0.0"
var global  = this

//----------------------------------------------------------------------------
// if require() is already defined, leave
//----------------------------------------------------------------------------
if (global.modjewel) {
    log("modjewel global variable already defined")
    return
}

global.modjewel = null

//----------------------------------------------------------------------------
// "globals" (local to this function scope though)
//----------------------------------------------------------------------------
var ModuleStore
var ModulePreloadStore
var MainModule
var WarnOnRecursiveRequire = false

//----------------------------------------------------------------------------
// the require function
//----------------------------------------------------------------------------
function get_require(currentModule) {
    var result = function require(moduleId) {

        if (moduleId.match(/^\.{1,2}\//)) {
            moduleId = normalize(currentModule, moduleId)
        }

        if (hop(ModuleStore, moduleId)) {
            var module = ModuleStore[moduleId]
            if (module.__isLoading) {
                if (WarnOnRecursiveRequire) {
                    var fromModule = currentModule ? currentModule.id : "<root>"
                    console.log("module '" + moduleId + "' recursively require()d from '" + fromModule + "', problem?")
                }
            }

            currentModule.moduleIdsRequired.push(moduleId)

            return module.exports
        }

        if (!hop(ModulePreloadStore, moduleId)) {
            var fromModule = currentModule ? currentModule.id : "<root>"
            error("module '" + moduleId + "' not found from '" + fromModule + "', must be define()'d first")
        }

        var factory = ModulePreloadStore[moduleId][0]
        var prereqs = ModulePreloadStore[moduleId][1]

        var module = create_module(moduleId)

        var newRequire = get_require(module)

        ModuleStore[moduleId] = module

        module.__isLoading = true
        try {
            currentModule.moduleIdsRequired.push(moduleId)

            var prereqModules = []
            for (var i=0; i<prereqs.length; i++) {
                var prereqId = prereqs[i]
                var prereqModule

                if      (prereqId == "require") prereqModule = newRequire
                else if (prereqId == "exports") prereqModule = module.exports
                else if (prereqId == "module")  prereqModule = module
                else                            prereqModule = newRequire(prereqId)

                prereqModules.push(prereqModule)
            }

            if (typeof factory == "function") {
                var result = factory.apply(null, prereqModules)
                if (result) {
                    module.exports = result
                }
            }
            else {
                module.exports = factory
            }
        }
        finally {
            module.__isLoading = false
        }

        return module.exports
    }

    result.define         = require_define
    result.implementation = PROGRAM
    result.version        = VERSION

    return result
}

//----------------------------------------------------------------------------
// shorter version of hasOwnProperty
//----------------------------------------------------------------------------
function hop(object, name) {
    return Object.prototype.hasOwnProperty.call(object, name)
}

//----------------------------------------------------------------------------
// create a new module
//----------------------------------------------------------------------------
function create_module(id) {
    return {
        id:                id,
        uri:               id,
        exports:           {},
        prereqIds:         [],
        moduleIdsRequired: []
    }
}

//----------------------------------------------------------------------------
// reset the stores
//----------------------------------------------------------------------------
function require_reset() {
    ModuleStore        = {}
    ModulePreloadStore = {}
    MainModule         = create_module(null)

    var require = get_require(MainModule)
    var define  = require_define
    
    define("modjewel", modjewel_module)

    global.modjewel            = require("modjewel")
    global.modjewel.require    = require
    global.modjewel.define     = define
    global.modjewel.define.amd = {implementation: PROGRAM, version: VERSION}
}

//----------------------------------------------------------------------------
// used by pre-built modules that can be included via <script src=>
// a simplification of
//    http://wiki.commonjs.org/wiki/Modules/AsynchronousDefinition
// where id is required
//----------------------------------------------------------------------------
function require_define(moduleId, prereqs, factory) {
    var rem = ["require", "exports", "module"]

    if (typeof moduleId != "string") {
        console.log("modjewel.define(): first parameter must be a string; was: " + moduleId)
        return
    }

    if (arguments.length == 2) {
        factory = prereqs
        prereqs = null
    }

    if (!prereqs || prereqs.length == 0) {
        prereqs = rem
    }

    if (typeof factory != "function") {
        if (factory) {
            ModulePreloadStore[moduleId] = [factory, prereqs]
            return
        }

        console.log("modjewel.define(): factory was falsy: " + factory)
        return
    }

    if (moduleId.match(/^\./)) {
        console.log("modjewel.define(): moduleId must not start with '.': '" + moduleName + "'")
        return
    }

    if (hop(ModulePreloadStore, moduleId)) {
        console.log("modjewel.define(): module '" + moduleId + "' has already been defined")
        return
    }

    ModulePreloadStore[moduleId] = [factory, prereqs]
}

//----------------------------------------------------------------------------
// get the path of a module
//----------------------------------------------------------------------------
function getModulePath(module) {
    if (!module || !module.id) return ""

    var parts = module.id.split("/")

    return parts.slice(0, parts.length-1).join("/")
}

//----------------------------------------------------------------------------
// normalize a 'file name' with . and .. with a 'directory name'
//----------------------------------------------------------------------------
function normalize(module, file) {
    var modulePath = getModulePath(module)
    var dirParts   = ("" == modulePath) ? [] : modulePath.split("/")
    var fileParts  = file.split("/")

    for (var i=0; i<fileParts.length; i++) {
        var filePart = fileParts[i]

        if (filePart == ".") {
        }

        else if (filePart == "..") {
            if (dirParts.length > 0) {
                dirParts.pop()
            }
            else {
                // error("error normalizing '" + module + "' and '" + file + "'")
                // eat non-valid .. paths
            }
        }

        else {
            dirParts.push(filePart)
        }
    }

    return dirParts.join("/")
}

//----------------------------------------------------------------------------
// throw an error
//----------------------------------------------------------------------------
function error(message) {
    throw new Error(PROGRAM + ": " + message)
}

//----------------------------------------------------------------------------
// get a list of loaded modules
//----------------------------------------------------------------------------
function modjewel_getLoadedModuleIds() {
    var result = []

    for (moduleId in ModuleStore) {
        result.push(moduleId)
    }

    return result
}

//----------------------------------------------------------------------------
// get a list of the preloaded module ids
//----------------------------------------------------------------------------
function modjewel_getPreloadedModuleIds() {
    var result = []

    for (moduleId in ModulePreloadStore) {
        result.push(moduleId)
    }

    return result
}

//----------------------------------------------------------------------------
// get a module by module id
//----------------------------------------------------------------------------
function modjewel_getModule(moduleId) {
    if (null == moduleId) return MainModule

    return ModuleStore[moduleId]
}

//----------------------------------------------------------------------------
// get a list of module ids which have been required by the specified module id
//----------------------------------------------------------------------------
function modjewel_getModuleIdsRequired(moduleId) {
    var module = modjewel_getModule(moduleId)
    if (null == module) return null

    return module.moduleIdsRequired.slice()
}

//----------------------------------------------------------------------------
// set the WarnOnRecursiveRequireFlag
// - if you make use of "module.exports =" in your code, you will want this on
//----------------------------------------------------------------------------
function modjewel_warnOnRecursiveRequire(value) {
    if (arguments.length == 0) return WarnOnRecursiveRequire
    WarnOnRecursiveRequire = !!value
}

//----------------------------------------------------------------------------
// the modjewel module
//----------------------------------------------------------------------------
function modjewel_module(require, exports, module) {
    exports.VERSION                = VERSION
    exports.require                = null // filled in later
    exports.define                 = null // filled in later
    exports.getLoadedModuleIds     = modjewel_getLoadedModuleIds
    exports.getPreloadedModuleIds  = modjewel_getPreloadedModuleIds
    exports.getModule              = modjewel_getModule
    exports.getModuleIdsRequired   = modjewel_getModuleIdsRequired
    exports.warnOnRecursiveRequire = modjewel_warnOnRecursiveRequire
}

//----------------------------------------------------------------------------
// log a message
//----------------------------------------------------------------------------
function log(message) {
    console.log("modjewel: " + message)
}

//----------------------------------------------------------------------------
// make the require function a global
//----------------------------------------------------------------------------
require_reset()

//----------------------------------------------------------------------------
})();
