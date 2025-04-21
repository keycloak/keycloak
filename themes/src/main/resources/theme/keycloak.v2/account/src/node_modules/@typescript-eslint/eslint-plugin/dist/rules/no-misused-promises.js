"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || function (mod) {
    if (mod && mod.__esModule) return mod;
    var result = {};
    if (mod != null) for (var k in mod) if (k !== "default" && Object.prototype.hasOwnProperty.call(mod, k)) __createBinding(result, mod, k);
    __setModuleDefault(result, mod);
    return result;
};
Object.defineProperty(exports, "__esModule", { value: true });
const utils_1 = require("@typescript-eslint/utils");
const tsutils = __importStar(require("tsutils"));
const ts = __importStar(require("typescript"));
const util = __importStar(require("../util"));
function parseChecksVoidReturn(checksVoidReturn) {
    var _a, _b, _c, _d, _e;
    switch (checksVoidReturn) {
        case false:
            return false;
        case true:
        case undefined:
            return {
                arguments: true,
                attributes: true,
                properties: true,
                returns: true,
                variables: true,
            };
        default:
            return {
                arguments: (_a = checksVoidReturn.arguments) !== null && _a !== void 0 ? _a : true,
                attributes: (_b = checksVoidReturn.attributes) !== null && _b !== void 0 ? _b : true,
                properties: (_c = checksVoidReturn.properties) !== null && _c !== void 0 ? _c : true,
                returns: (_d = checksVoidReturn.returns) !== null && _d !== void 0 ? _d : true,
                variables: (_e = checksVoidReturn.variables) !== null && _e !== void 0 ? _e : true,
            };
    }
}
exports.default = util.createRule({
    name: 'no-misused-promises',
    meta: {
        docs: {
            description: 'Disallow Promises in places not designed to handle them',
            recommended: 'error',
            requiresTypeChecking: true,
        },
        messages: {
            voidReturnArgument: 'Promise returned in function argument where a void return was expected.',
            voidReturnVariable: 'Promise-returning function provided to variable where a void return was expected.',
            voidReturnProperty: 'Promise-returning function provided to property where a void return was expected.',
            voidReturnReturnValue: 'Promise-returning function provided to return value where a void return was expected.',
            voidReturnAttribute: 'Promise-returning function provided to attribute where a void return was expected.',
            conditional: 'Expected non-Promise value in a boolean conditional.',
            spread: 'Expected a non-Promise value to be spreaded in an object.',
        },
        schema: [
            {
                type: 'object',
                properties: {
                    checksConditionals: {
                        type: 'boolean',
                    },
                    checksVoidReturn: {
                        oneOf: [
                            { type: 'boolean' },
                            {
                                additionalProperties: false,
                                properties: {
                                    arguments: { type: 'boolean' },
                                    attributes: { type: 'boolean' },
                                    properties: { type: 'boolean' },
                                    returns: { type: 'boolean' },
                                    variables: { type: 'boolean' },
                                },
                                type: 'object',
                            },
                        ],
                    },
                    checksSpreads: {
                        type: 'boolean',
                    },
                },
            },
        ],
        type: 'problem',
    },
    defaultOptions: [
        {
            checksConditionals: true,
            checksVoidReturn: true,
            checksSpreads: true,
        },
    ],
    create(context, [{ checksConditionals, checksVoidReturn, checksSpreads }]) {
        const parserServices = util.getParserServices(context);
        const checker = parserServices.program.getTypeChecker();
        const checkedNodes = new Set();
        const conditionalChecks = {
            ConditionalExpression: checkTestConditional,
            DoWhileStatement: checkTestConditional,
            ForStatement: checkTestConditional,
            IfStatement: checkTestConditional,
            LogicalExpression: checkConditional,
            'UnaryExpression[operator="!"]'(node) {
                checkConditional(node.argument, true);
            },
            WhileStatement: checkTestConditional,
        };
        checksVoidReturn = parseChecksVoidReturn(checksVoidReturn);
        const voidReturnChecks = checksVoidReturn
            ? Object.assign(Object.assign(Object.assign(Object.assign(Object.assign({}, (checksVoidReturn.arguments && {
                CallExpression: checkArguments,
                NewExpression: checkArguments,
            })), (checksVoidReturn.attributes && {
                JSXAttribute: checkJSXAttribute,
            })), (checksVoidReturn.properties && {
                Property: checkProperty,
            })), (checksVoidReturn.returns && {
                ReturnStatement: checkReturnStatement,
            })), (checksVoidReturn.variables && {
                AssignmentExpression: checkAssignment,
                VariableDeclarator: checkVariableDeclaration,
            })) : {};
        const spreadChecks = {
            SpreadElement: checkSpread,
        };
        function checkTestConditional(node) {
            if (node.test) {
                checkConditional(node.test, true);
            }
        }
        /**
         * This function analyzes the type of a node and checks if it is a Promise in a boolean conditional.
         * It uses recursion when checking nested logical operators.
         * @param node The AST node to check.
         * @param isTestExpr Whether the node is a descendant of a test expression.
         */
        function checkConditional(node, isTestExpr = false) {
            // prevent checking the same node multiple times
            if (checkedNodes.has(node)) {
                return;
            }
            checkedNodes.add(node);
            if (node.type === utils_1.AST_NODE_TYPES.LogicalExpression) {
                // ignore the left operand for nullish coalescing expressions not in a context of a test expression
                if (node.operator !== '??' || isTestExpr) {
                    checkConditional(node.left, isTestExpr);
                }
                // we ignore the right operand when not in a context of a test expression
                if (isTestExpr) {
                    checkConditional(node.right, isTestExpr);
                }
                return;
            }
            const tsNode = parserServices.esTreeNodeToTSNodeMap.get(node);
            if (isAlwaysThenable(checker, tsNode)) {
                context.report({
                    messageId: 'conditional',
                    node,
                });
            }
        }
        function checkArguments(node) {
            const tsNode = parserServices.esTreeNodeToTSNodeMap.get(node);
            const voidParams = voidFunctionParams(checker, tsNode);
            if (voidParams.size === 0) {
                return;
            }
            for (const [index, argument] of node.arguments.entries()) {
                if (!voidParams.has(index)) {
                    continue;
                }
                const tsNode = parserServices.esTreeNodeToTSNodeMap.get(argument);
                if (returnsThenable(checker, tsNode)) {
                    context.report({
                        messageId: 'voidReturnArgument',
                        node: argument,
                    });
                }
            }
        }
        function checkAssignment(node) {
            const tsNode = parserServices.esTreeNodeToTSNodeMap.get(node);
            const varType = checker.getTypeAtLocation(tsNode.left);
            if (!isVoidReturningFunctionType(checker, tsNode.left, varType)) {
                return;
            }
            if (returnsThenable(checker, tsNode.right)) {
                context.report({
                    messageId: 'voidReturnVariable',
                    node: node.right,
                });
            }
        }
        function checkVariableDeclaration(node) {
            const tsNode = parserServices.esTreeNodeToTSNodeMap.get(node);
            if (tsNode.initializer === undefined || node.init === null) {
                return;
            }
            const varType = checker.getTypeAtLocation(tsNode.name);
            if (!isVoidReturningFunctionType(checker, tsNode.initializer, varType)) {
                return;
            }
            if (returnsThenable(checker, tsNode.initializer)) {
                context.report({
                    messageId: 'voidReturnVariable',
                    node: node.init,
                });
            }
        }
        function checkProperty(node) {
            const tsNode = parserServices.esTreeNodeToTSNodeMap.get(node);
            if (ts.isPropertyAssignment(tsNode)) {
                const contextualType = checker.getContextualType(tsNode.initializer);
                if (contextualType !== undefined &&
                    isVoidReturningFunctionType(checker, tsNode.initializer, contextualType) &&
                    returnsThenable(checker, tsNode.initializer)) {
                    context.report({
                        messageId: 'voidReturnProperty',
                        node: node.value,
                    });
                }
            }
            else if (ts.isShorthandPropertyAssignment(tsNode)) {
                const contextualType = checker.getContextualType(tsNode.name);
                if (contextualType !== undefined &&
                    isVoidReturningFunctionType(checker, tsNode.name, contextualType) &&
                    returnsThenable(checker, tsNode.name)) {
                    context.report({
                        messageId: 'voidReturnProperty',
                        node: node.value,
                    });
                }
            }
            else if (ts.isMethodDeclaration(tsNode)) {
                if (ts.isComputedPropertyName(tsNode.name)) {
                    return;
                }
                const obj = tsNode.parent;
                // Below condition isn't satisfied unless something goes wrong,
                // but is needed for type checking.
                // 'node' does not include class method declaration so 'obj' is
                // always an object literal expression, but after converting 'node'
                // to TypeScript AST, its type includes MethodDeclaration which
                // does include the case of class method declaration.
                if (!ts.isObjectLiteralExpression(obj)) {
                    return;
                }
                const objType = checker.getContextualType(obj);
                if (objType === undefined) {
                    return;
                }
                const propertySymbol = checker.getPropertyOfType(objType, tsNode.name.text);
                if (propertySymbol === undefined) {
                    return;
                }
                const contextualType = checker.getTypeOfSymbolAtLocation(propertySymbol, tsNode.name);
                if (isVoidReturningFunctionType(checker, tsNode.name, contextualType) &&
                    returnsThenable(checker, tsNode)) {
                    context.report({
                        messageId: 'voidReturnProperty',
                        node: node.value,
                    });
                }
                return;
            }
        }
        function checkReturnStatement(node) {
            const tsNode = parserServices.esTreeNodeToTSNodeMap.get(node);
            if (tsNode.expression === undefined || node.argument === null) {
                return;
            }
            const contextualType = checker.getContextualType(tsNode.expression);
            if (contextualType !== undefined &&
                isVoidReturningFunctionType(checker, tsNode.expression, contextualType) &&
                returnsThenable(checker, tsNode.expression)) {
                context.report({
                    messageId: 'voidReturnReturnValue',
                    node: node.argument,
                });
            }
        }
        function checkJSXAttribute(node) {
            const tsNode = parserServices.esTreeNodeToTSNodeMap.get(node);
            const value = tsNode.initializer;
            if (node.value === null ||
                value === undefined ||
                !ts.isJsxExpression(value) ||
                value.expression === undefined) {
                return;
            }
            const contextualType = checker.getContextualType(value);
            if (contextualType !== undefined &&
                isVoidReturningFunctionType(checker, value, contextualType) &&
                returnsThenable(checker, value.expression)) {
                context.report({
                    messageId: 'voidReturnAttribute',
                    node: node.value,
                });
            }
        }
        function checkSpread(node) {
            const tsNode = parserServices.esTreeNodeToTSNodeMap.get(node);
            if (isSometimesThenable(checker, tsNode.expression)) {
                context.report({
                    messageId: 'spread',
                    node: node.argument,
                });
            }
        }
        return Object.assign(Object.assign(Object.assign({}, (checksConditionals ? conditionalChecks : {})), (checksVoidReturn ? voidReturnChecks : {})), (checksSpreads ? spreadChecks : {}));
    },
});
function isSometimesThenable(checker, node) {
    const type = checker.getTypeAtLocation(node);
    for (const subType of tsutils.unionTypeParts(checker.getApparentType(type))) {
        if (tsutils.isThenableType(checker, node, subType)) {
            return true;
        }
    }
    return false;
}
// Variation on the thenable check which requires all forms of the type (read:
// alternates in a union) to be thenable. Otherwise, you might be trying to
// check if something is defined or undefined and get caught because one of the
// branches is thenable.
function isAlwaysThenable(checker, node) {
    const type = checker.getTypeAtLocation(node);
    for (const subType of tsutils.unionTypeParts(checker.getApparentType(type))) {
        const thenProp = subType.getProperty('then');
        // If one of the alternates has no then property, it is not thenable in all
        // cases.
        if (thenProp === undefined) {
            return false;
        }
        // We walk through each variation of the then property. Since we know it
        // exists at this point, we just need at least one of the alternates to
        // be of the right form to consider it thenable.
        const thenType = checker.getTypeOfSymbolAtLocation(thenProp, node);
        let hasThenableSignature = false;
        for (const subType of tsutils.unionTypeParts(thenType)) {
            for (const signature of subType.getCallSignatures()) {
                if (signature.parameters.length !== 0 &&
                    isFunctionParam(checker, signature.parameters[0], node)) {
                    hasThenableSignature = true;
                    break;
                }
            }
            // We only need to find one variant of the then property that has a
            // function signature for it to be thenable.
            if (hasThenableSignature) {
                break;
            }
        }
        // If no flavors of the then property are thenable, we don't consider the
        // overall type to be thenable
        if (!hasThenableSignature) {
            return false;
        }
    }
    // If all variants are considered thenable (i.e. haven't returned false), we
    // consider the overall type thenable
    return true;
}
function isFunctionParam(checker, param, node) {
    const type = checker.getApparentType(checker.getTypeOfSymbolAtLocation(param, node));
    for (const subType of tsutils.unionTypeParts(type)) {
        if (subType.getCallSignatures().length !== 0) {
            return true;
        }
    }
    return false;
}
// Get the positions of parameters which are void functions (and not also
// thenable functions). These are the candidates for the void-return check at
// the current call site.
function voidFunctionParams(checker, node) {
    const thenableReturnIndices = new Set();
    const voidReturnIndices = new Set();
    const type = checker.getTypeAtLocation(node.expression);
    // We can't use checker.getResolvedSignature because it prefers an early '() => void' over a later '() => Promise<void>'
    // See https://github.com/microsoft/TypeScript/issues/48077
    for (const subType of tsutils.unionTypeParts(type)) {
        // Standard function calls and `new` have two different types of signatures
        const signatures = ts.isCallExpression(node)
            ? subType.getCallSignatures()
            : subType.getConstructSignatures();
        for (const signature of signatures) {
            for (const [index, parameter] of signature.parameters.entries()) {
                const type = checker.getTypeOfSymbolAtLocation(parameter, node.expression);
                if (isThenableReturningFunctionType(checker, node.expression, type)) {
                    thenableReturnIndices.add(index);
                }
                else if (!thenableReturnIndices.has(index) &&
                    isVoidReturningFunctionType(checker, node.expression, type)) {
                    voidReturnIndices.add(index);
                }
            }
        }
    }
    for (const index of thenableReturnIndices) {
        voidReturnIndices.delete(index);
    }
    return voidReturnIndices;
}
/**
 * @returns Whether any call signature of the type has a thenable return type.
 */
function anySignatureIsThenableType(checker, node, type) {
    for (const signature of type.getCallSignatures()) {
        const returnType = signature.getReturnType();
        if (tsutils.isThenableType(checker, node, returnType)) {
            return true;
        }
    }
    return false;
}
/**
 * @returns Whether type is a thenable-returning function.
 */
function isThenableReturningFunctionType(checker, node, type) {
    for (const subType of tsutils.unionTypeParts(type)) {
        if (anySignatureIsThenableType(checker, node, subType)) {
            return true;
        }
    }
    return false;
}
/**
 * @returns Whether type is a void-returning function.
 */
function isVoidReturningFunctionType(checker, node, type) {
    let hadVoidReturn = false;
    for (const subType of tsutils.unionTypeParts(type)) {
        for (const signature of subType.getCallSignatures()) {
            const returnType = signature.getReturnType();
            // If a certain positional argument accepts both thenable and void returns,
            // a promise-returning function is valid
            if (tsutils.isThenableType(checker, node, returnType)) {
                return false;
            }
            hadVoidReturn || (hadVoidReturn = tsutils.isTypeFlagSet(returnType, ts.TypeFlags.Void));
        }
    }
    return hadVoidReturn;
}
/**
 * @returns Whether expression is a function that returns a thenable.
 */
function returnsThenable(checker, node) {
    const type = checker.getApparentType(checker.getTypeAtLocation(node));
    if (anySignatureIsThenableType(checker, node, type)) {
        return true;
    }
    return false;
}
//# sourceMappingURL=no-misused-promises.js.map