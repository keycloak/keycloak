"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const traverser_1 = __importDefault(require("eslint/lib/util/traverser"));
const typescript_estree_1 = require("@typescript-eslint/typescript-estree");
const analyze_scope_1 = require("./analyze-scope");
const visitor_keys_1 = require("./visitor-keys");
// note - cannot migrate this to an import statement because it will make TSC copy the package.json to the dist folder
const packageJSON = require('../package.json');
function validateBoolean(value, fallback = false) {
    if (typeof value !== 'boolean') {
        return fallback;
    }
    return value;
}
//------------------------------------------------------------------------------
// Public
//------------------------------------------------------------------------------
exports.version = packageJSON.version;
exports.Syntax = Object.freeze(typescript_estree_1.AST_NODE_TYPES);
function parse(code, options) {
    return parseForESLint(code, options).ast;
}
exports.parse = parse;
function parseForESLint(code, options) {
    if (!options || typeof options !== 'object') {
        options = {};
    }
    // https://eslint.org/docs/user-guide/configuring#specifying-parser-options
    // if sourceType is not provided by default eslint expect that it will be set to "script"
    if (options.sourceType !== 'module' && options.sourceType !== 'script') {
        options.sourceType = 'script';
    }
    if (typeof options.ecmaFeatures !== 'object') {
        options.ecmaFeatures = {};
    }
    const parserOptions = {};
    Object.assign(parserOptions, options, {
        useJSXTextNode: validateBoolean(options.useJSXTextNode, true),
        jsx: validateBoolean(options.ecmaFeatures.jsx),
    });
    if (typeof options.filePath === 'string') {
        const tsx = options.filePath.endsWith('.tsx');
        if (tsx || options.filePath.endsWith('.ts')) {
            parserOptions.jsx = tsx;
        }
    }
    /**
     * Allow the user to suppress the warning from typescript-estree if they are using an unsupported
     * version of TypeScript
     */
    const warnOnUnsupportedTypeScriptVersion = validateBoolean(options.warnOnUnsupportedTypeScriptVersion, true);
    if (!warnOnUnsupportedTypeScriptVersion) {
        parserOptions.loggerFn = false;
    }
    const { ast, services } = typescript_estree_1.parseAndGenerateServices(code, parserOptions);
    ast.sourceType = options.sourceType;
    traverser_1.default.traverse(ast, {
        enter(node) {
            switch (node.type) {
                // Function#body cannot be null in ESTree spec.
                case 'FunctionExpression':
                    if (!node.body) {
                        node.type = `TSEmptyBody${node.type}`;
                    }
                    break;
                // no default
            }
        },
    });
    const scopeManager = analyze_scope_1.analyzeScope(ast, options);
    return { ast, services, scopeManager, visitorKeys: visitor_keys_1.visitorKeys };
}
exports.parseForESLint = parseForESLint;
//# sourceMappingURL=parser.js.map