/*
  @license
	Rollup.js v1.32.1
	Fri, 06 Mar 2020 09:32:05 GMT - commit f458cbf6cb8cfcc1678593d8dc595e4b8757eb6d


	https://github.com/rollup/rollup

	Released under the MIT License.
*/
'use strict';

var path = require('path');
var module$1 = require('module');

/*! *****************************************************************************
Copyright (c) Microsoft Corporation. All rights reserved.
Licensed under the Apache License, Version 2.0 (the "License"); you may not use
this file except in compliance with the License. You may obtain a copy of the
License at http://www.apache.org/licenses/LICENSE-2.0

THIS CODE IS PROVIDED ON AN *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION ANY IMPLIED
WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A PARTICULAR PURPOSE,
MERCHANTABLITY OR NON-INFRINGEMENT.

See the Apache Version 2.0 License for specific language governing permissions
and limitations under the License.
***************************************************************************** */
function __awaiter(thisArg, _arguments, P, generator) {
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try {
            step(generator.next(value));
        }
        catch (e) {
            reject(e);
        } }
        function rejected(value) { try {
            step(generator["throw"](value));
        }
        catch (e) {
            reject(e);
        } }
        function step(result) { result.done ? resolve(result.value) : new P(function (resolve) { resolve(result.value); }).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
}

var version = "1.32.1";

function createCommonjsModule(fn, module) {
	return module = { exports: {} }, fn(module, module.exports), module.exports;
}

const absolutePath = /^(?:\/|(?:[A-Za-z]:)?[\\|/])/;
const relativePath = /^\.?\.\//;
function isAbsolute(path) {
    return absolutePath.test(path);
}
function isRelative(path) {
    return relativePath.test(path);
}
function normalize(path) {
    if (path.indexOf('\\') == -1)
        return path;
    return path.replace(/\\/g, '/');
}

function sanitizeFileName(name) {
    return name.replace(/[\0?*]/g, '_');
}

function getAliasName(id) {
    const base = path.basename(id);
    return base.substr(0, base.length - path.extname(id).length);
}
function relativeId(id) {
    if (typeof process === 'undefined' || !isAbsolute(id))
        return id;
    return path.relative(process.cwd(), id);
}
function isPlainPathFragment(name) {
    // not starting with "/", "./", "../"
    return (name[0] !== '/' &&
        !(name[0] === '.' && (name[1] === '/' || name[1] === '.')) &&
        sanitizeFileName(name) === name);
}

const createGetOption = (config, command) => (name, defaultValue) => command[name] !== undefined
    ? command[name]
    : config[name] !== undefined
        ? config[name]
        : defaultValue;
const normalizeObjectOptionValue = (optionValue) => {
    if (!optionValue) {
        return optionValue;
    }
    if (typeof optionValue !== 'object') {
        return {};
    }
    return optionValue;
};
const getObjectOption = (config, command, name) => {
    const commandOption = normalizeObjectOptionValue(command[name]);
    const configOption = normalizeObjectOptionValue(config[name]);
    if (commandOption !== undefined) {
        return commandOption && configOption ? Object.assign(Object.assign({}, configOption), commandOption) : commandOption;
    }
    return configOption;
};
function ensureArray(items) {
    if (Array.isArray(items)) {
        return items.filter(Boolean);
    }
    if (items) {
        return [items];
    }
    return [];
}
const defaultOnWarn = warning => {
    if (typeof warning === 'string') {
        console.warn(warning);
    }
    else {
        console.warn(warning.message);
    }
};
const getOnWarn = (config, defaultOnWarnHandler = defaultOnWarn) => config.onwarn
    ? warning => config.onwarn(warning, defaultOnWarnHandler)
    : defaultOnWarnHandler;
const getExternal = (config, command) => {
    const configExternal = config.external;
    return typeof configExternal === 'function'
        ? (id, ...rest) => configExternal(id, ...rest) || command.external.indexOf(id) !== -1
        : (typeof config.external === 'string'
            ? [configExternal]
            : Array.isArray(configExternal)
                ? configExternal
                : []).concat(command.external);
};
const commandAliases = {
    c: 'config',
    d: 'dir',
    e: 'external',
    f: 'format',
    g: 'globals',
    h: 'help',
    i: 'input',
    m: 'sourcemap',
    n: 'name',
    o: 'file',
    p: 'plugin',
    v: 'version',
    w: 'watch'
};
function mergeOptions({ config = {}, command: rawCommandOptions = {}, defaultOnWarnHandler }) {
    const command = getCommandOptions(rawCommandOptions);
    const inputOptions = getInputOptions(config, command, defaultOnWarnHandler);
    if (command.output) {
        Object.assign(command, command.output);
    }
    const output = config.output;
    const normalizedOutputOptions = Array.isArray(output) ? output : output ? [output] : [];
    if (normalizedOutputOptions.length === 0)
        normalizedOutputOptions.push({});
    const outputOptions = normalizedOutputOptions.map(singleOutputOptions => getOutputOptions(singleOutputOptions, command));
    const unknownOptionErrors = [];
    const validInputOptions = Object.keys(inputOptions);
    addUnknownOptionErrors(unknownOptionErrors, Object.keys(config), validInputOptions, 'input option', /^output$/);
    const validOutputOptions = Object.keys(outputOptions[0]);
    addUnknownOptionErrors(unknownOptionErrors, outputOptions.reduce((allKeys, options) => allKeys.concat(Object.keys(options)), []), validOutputOptions, 'output option');
    const validCliOutputOptions = validOutputOptions.filter(option => option !== 'sourcemapPathTransform');
    addUnknownOptionErrors(unknownOptionErrors, Object.keys(command), validInputOptions.concat(validCliOutputOptions, Object.keys(commandAliases), 'config', 'environment', 'plugin', 'silent', 'stdin'), 'CLI flag', /^_|output|(config.*)$/);
    return {
        inputOptions,
        optionError: unknownOptionErrors.length > 0 ? unknownOptionErrors.join('\n') : null,
        outputOptions
    };
}
function addUnknownOptionErrors(errors, options, validOptions, optionType, ignoredKeys = /$./) {
    const validOptionSet = new Set(validOptions);
    const unknownOptions = options.filter(key => !validOptionSet.has(key) && !ignoredKeys.test(key));
    if (unknownOptions.length > 0)
        errors.push(`Unknown ${optionType}: ${unknownOptions.join(', ')}. Allowed options: ${Array.from(validOptionSet)
            .sort()
            .join(', ')}`);
}
function getCommandOptions(rawCommandOptions) {
    const external = rawCommandOptions.external && typeof rawCommandOptions.external === 'string'
        ? rawCommandOptions.external.split(',')
        : [];
    return Object.assign(Object.assign({}, rawCommandOptions), { external, globals: typeof rawCommandOptions.globals === 'string'
            ? rawCommandOptions.globals.split(',').reduce((globals, globalDefinition) => {
                const [id, variableName] = globalDefinition.split(':');
                globals[id] = variableName;
                if (external.indexOf(id) === -1) {
                    external.push(id);
                }
                return globals;
            }, Object.create(null))
            : undefined });
}
function getInputOptions(config, command = { external: [], globals: undefined }, defaultOnWarnHandler) {
    const getOption = createGetOption(config, command);
    const inputOptions = {
        acorn: config.acorn,
        acornInjectPlugins: config.acornInjectPlugins,
        cache: getOption('cache'),
        chunkGroupingSize: getOption('chunkGroupingSize', 5000),
        context: getOption('context'),
        experimentalCacheExpiry: getOption('experimentalCacheExpiry', 10),
        experimentalOptimizeChunks: getOption('experimentalOptimizeChunks'),
        external: getExternal(config, command),
        inlineDynamicImports: getOption('inlineDynamicImports', false),
        input: getOption('input', []),
        manualChunks: getOption('manualChunks'),
        moduleContext: config.moduleContext,
        onwarn: getOnWarn(config, defaultOnWarnHandler),
        perf: getOption('perf', false),
        plugins: ensureArray(config.plugins),
        preserveModules: getOption('preserveModules'),
        preserveSymlinks: getOption('preserveSymlinks'),
        shimMissingExports: getOption('shimMissingExports'),
        strictDeprecations: getOption('strictDeprecations', false),
        treeshake: getObjectOption(config, command, 'treeshake'),
        watch: config.watch
    };
    // support rollup({ cache: prevBuildObject })
    if (inputOptions.cache && inputOptions.cache.cache)
        inputOptions.cache = inputOptions.cache.cache;
    return inputOptions;
}
function getOutputOptions(config, command = {}) {
    const getOption = createGetOption(config, command);
    let format = getOption('format');
    // Handle format aliases
    switch (format) {
        case undefined:
        case 'esm':
        case 'module':
            format = 'es';
            break;
        case 'commonjs':
            format = 'cjs';
    }
    return {
        amd: Object.assign(Object.assign({}, config.amd), command.amd),
        assetFileNames: getOption('assetFileNames'),
        banner: getOption('banner'),
        chunkFileNames: getOption('chunkFileNames'),
        compact: getOption('compact', false),
        dir: getOption('dir'),
        dynamicImportFunction: getOption('dynamicImportFunction'),
        entryFileNames: getOption('entryFileNames'),
        esModule: getOption('esModule', true),
        exports: getOption('exports'),
        extend: getOption('extend'),
        externalLiveBindings: getOption('externalLiveBindings', true),
        file: getOption('file'),
        footer: getOption('footer'),
        format,
        freeze: getOption('freeze', true),
        globals: getOption('globals'),
        hoistTransitiveImports: getOption('hoistTransitiveImports', true),
        indent: getOption('indent', true),
        interop: getOption('interop', true),
        intro: getOption('intro'),
        name: getOption('name'),
        namespaceToStringTag: getOption('namespaceToStringTag', false),
        noConflict: getOption('noConflict'),
        outro: getOption('outro'),
        paths: getOption('paths'),
        plugins: ensureArray(config.plugins),
        preferConst: getOption('preferConst'),
        sourcemap: getOption('sourcemap'),
        sourcemapExcludeSources: getOption('sourcemapExcludeSources'),
        sourcemapFile: getOption('sourcemapFile'),
        sourcemapPathTransform: getOption('sourcemapPathTransform'),
        strict: getOption('strict', true)
    };
}

var modules = {};
var getModule = function (dir) {
    var rootPath = dir ? path.resolve(dir) : process.cwd();
    var rootName = path.join(rootPath, '@root');
    var root = modules[rootName];
    if (!root) {
        root = new module$1(rootName);
        root.filename = rootName;
        root.paths = module$1._nodeModulePaths(rootPath);
        modules[rootName] = root;
    }
    return root;
};
var requireRelative = function (requested, relativeTo) {
    var root = getModule(relativeTo);
    return root.require(requested);
};
requireRelative.resolve = function (requested, relativeTo) {
    var root = getModule(relativeTo);
    return module$1._resolveFilename(requested, root);
};
var requireRelative_1 = requireRelative;

exports.__awaiter = __awaiter;
exports.commandAliases = commandAliases;
exports.createCommonjsModule = createCommonjsModule;
exports.ensureArray = ensureArray;
exports.getAliasName = getAliasName;
exports.isAbsolute = isAbsolute;
exports.isPlainPathFragment = isPlainPathFragment;
exports.isRelative = isRelative;
exports.mergeOptions = mergeOptions;
exports.normalize = normalize;
exports.path = path;
exports.relative = requireRelative_1;
exports.relativeId = relativeId;
exports.sanitizeFileName = sanitizeFileName;
exports.version = version;
//# sourceMappingURL=index.js.map
