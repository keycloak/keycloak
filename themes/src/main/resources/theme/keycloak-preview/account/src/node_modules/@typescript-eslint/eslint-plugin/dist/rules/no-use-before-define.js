"use strict";
var __importStar = (this && this.__importStar) || function (mod) {
    if (mod && mod.__esModule) return mod;
    var result = {};
    if (mod != null) for (var k in mod) if (Object.hasOwnProperty.call(mod, k)) result[k] = mod[k];
    result["default"] = mod;
    return result;
};
Object.defineProperty(exports, "__esModule", { value: true });
const typescript_estree_1 = require("@typescript-eslint/typescript-estree");
const util = __importStar(require("../util"));
const SENTINEL_TYPE = /^(?:(?:Function|Class)(?:Declaration|Expression)|ArrowFunctionExpression|CatchClause|ImportDeclaration|ExportNamedDeclaration)$/;
/**
 * Parses a given value as options.
 */
function parseOptions(options) {
    let functions = true;
    let classes = true;
    let variables = true;
    let typedefs = true;
    if (typeof options === 'string') {
        functions = options !== 'nofunc';
    }
    else if (typeof options === 'object' && options !== null) {
        functions = options.functions !== false;
        classes = options.classes !== false;
        variables = options.variables !== false;
        typedefs = options.typedefs !== false;
    }
    return { functions, classes, variables, typedefs };
}
/**
 * Checks whether or not a given scope is a top level scope.
 */
function isTopLevelScope(scope) {
    return scope.type === 'module' || scope.type === 'global';
}
/**
 * Checks whether or not a given variable is a function declaration.
 */
function isFunction(variable) {
    return variable.defs[0].type === 'FunctionName';
}
/**
 * Checks whether or not a given variable is a class declaration in an upper function scope.
 */
function isOuterClass(variable, reference) {
    if (variable.defs[0].type !== 'ClassName') {
        return false;
    }
    if (variable.scope.variableScope === reference.from.variableScope) {
        // allow the same scope only if it's the top level global/module scope
        if (!isTopLevelScope(variable.scope.variableScope)) {
            return false;
        }
    }
    return true;
}
/**
 * Checks whether or not a given variable is a variable declaration in an upper function scope.
 */
function isOuterVariable(variable, reference) {
    if (variable.defs[0].type !== 'Variable') {
        return false;
    }
    if (variable.scope.variableScope === reference.from.variableScope) {
        // allow the same scope only if it's the top level global/module scope
        if (!isTopLevelScope(variable.scope.variableScope)) {
            return false;
        }
    }
    return true;
}
/**
 * Checks whether or not a given location is inside of the range of a given node.
 */
function isInRange(node, location) {
    return !!node && node.range[0] <= location && location <= node.range[1];
}
/**
 * Checks whether or not a given reference is inside of the initializers of a given variable.
 *
 * @returns `true` in the following cases:
 * - var a = a
 * - var [a = a] = list
 * - var {a = a} = obj
 * - for (var a in a) {}
 * - for (var a of a) {}
 */
function isInInitializer(variable, reference) {
    if (variable.scope !== reference.from) {
        return false;
    }
    let node = variable.identifiers[0].parent;
    const location = reference.identifier.range[1];
    while (node) {
        if (node.type === typescript_estree_1.AST_NODE_TYPES.VariableDeclarator) {
            if (isInRange(node.init, location)) {
                return true;
            }
            if (node.parent &&
                node.parent.parent &&
                (node.parent.parent.type === typescript_estree_1.AST_NODE_TYPES.ForInStatement ||
                    node.parent.parent.type === typescript_estree_1.AST_NODE_TYPES.ForOfStatement) &&
                isInRange(node.parent.parent.right, location)) {
                return true;
            }
            break;
        }
        else if (node.type === typescript_estree_1.AST_NODE_TYPES.AssignmentPattern) {
            if (isInRange(node.right, location)) {
                return true;
            }
        }
        else if (SENTINEL_TYPE.test(node.type)) {
            break;
        }
        node = node.parent;
    }
    return false;
}
exports.default = util.createRule({
    name: 'no-use-before-define',
    meta: {
        type: 'problem',
        docs: {
            description: 'Disallow the use of variables before they are defined',
            category: 'Variables',
            recommended: 'error',
        },
        messages: {
            noUseBeforeDefine: "'{{name}}' was used before it was defined.",
        },
        schema: [
            {
                oneOf: [
                    {
                        enum: ['nofunc'],
                    },
                    {
                        type: 'object',
                        properties: {
                            functions: { type: 'boolean' },
                            classes: { type: 'boolean' },
                            variables: { type: 'boolean' },
                            typedefs: { type: 'boolean' },
                        },
                        additionalProperties: false,
                    },
                ],
            },
        ],
    },
    defaultOptions: [
        {
            functions: true,
            classes: true,
            variables: true,
            typedefs: true,
        },
    ],
    create(context, optionsWithDefault) {
        const options = parseOptions(optionsWithDefault[0]);
        /**
         * Determines whether a given use-before-define case should be reported according to the options.
         * @param variable The variable that gets used before being defined
         * @param reference The reference to the variable
         */
        function isForbidden(variable, reference) {
            if (isFunction(variable)) {
                return !!options.functions;
            }
            if (isOuterClass(variable, reference)) {
                return !!options.classes;
            }
            if (isOuterVariable(variable, reference)) {
                return !!options.variables;
            }
            return true;
        }
        /**
         * Finds and validates all variables in a given scope.
         */
        function findVariablesInScope(scope) {
            scope.references.forEach(reference => {
                const variable = reference.resolved;
                // Skips when the reference is:
                // - initialization's.
                // - referring to an undefined variable.
                // - referring to a global environment variable (there're no identifiers).
                // - located preceded by the variable (except in initializers).
                // - allowed by options.
                if (reference.init ||
                    !variable ||
                    variable.identifiers.length === 0 ||
                    (variable.identifiers[0].range[1] < reference.identifier.range[1] &&
                        !isInInitializer(variable, reference)) ||
                    !isForbidden(variable, reference)) {
                    return;
                }
                // Reports.
                context.report({
                    node: reference.identifier,
                    messageId: 'noUseBeforeDefine',
                    data: reference.identifier,
                });
            });
            scope.childScopes.forEach(findVariablesInScope);
        }
        return {
            Program() {
                findVariablesInScope(context.getScope());
            },
        };
    },
});
//# sourceMappingURL=no-use-before-define.js.map