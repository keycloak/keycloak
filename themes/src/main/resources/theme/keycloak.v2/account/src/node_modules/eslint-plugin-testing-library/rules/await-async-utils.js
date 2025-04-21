"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.RULE_NAME = void 0;
const create_testing_library_rule_1 = require("../create-testing-library-rule");
const node_utils_1 = require("../node-utils");
exports.RULE_NAME = 'await-async-utils';
exports.default = (0, create_testing_library_rule_1.createTestingLibraryRule)({
    name: exports.RULE_NAME,
    meta: {
        type: 'problem',
        docs: {
            description: 'Enforce promises from async utils to be awaited properly',
            recommendedConfig: {
                dom: 'error',
                angular: 'error',
                react: 'error',
                vue: 'error',
                marko: 'error',
            },
        },
        messages: {
            awaitAsyncUtil: 'Promise returned from `{{ name }}` must be handled',
            asyncUtilWrapper: 'Promise returned from {{ name }} wrapper over async util must be handled',
        },
        schema: [],
    },
    defaultOptions: [],
    create(context, _, helpers) {
        const functionWrappersNames = [];
        function detectAsyncUtilWrapper(node) {
            const innerFunction = (0, node_utils_1.getInnermostReturningFunction)(context, node);
            if (innerFunction) {
                functionWrappersNames.push((0, node_utils_1.getFunctionName)(innerFunction));
            }
        }
        return {
            'CallExpression Identifier'(node) {
                if (helpers.isAsyncUtil(node)) {
                    detectAsyncUtilWrapper(node);
                    const closestCallExpression = (0, node_utils_1.findClosestCallExpressionNode)(node, true);
                    if (!closestCallExpression || !closestCallExpression.parent) {
                        return;
                    }
                    const references = (0, node_utils_1.getVariableReferences)(context, closestCallExpression.parent);
                    if (references.length === 0) {
                        if (!(0, node_utils_1.isPromiseHandled)(node)) {
                            context.report({
                                node,
                                messageId: 'awaitAsyncUtil',
                                data: {
                                    name: node.name,
                                },
                            });
                        }
                    }
                    else {
                        for (const reference of references) {
                            const referenceNode = reference.identifier;
                            if (!(0, node_utils_1.isPromiseHandled)(referenceNode)) {
                                context.report({
                                    node,
                                    messageId: 'awaitAsyncUtil',
                                    data: {
                                        name: node.name,
                                    },
                                });
                                return;
                            }
                        }
                    }
                }
                else if (functionWrappersNames.includes(node.name)) {
                    if (!(0, node_utils_1.isPromiseHandled)(node)) {
                        context.report({
                            node,
                            messageId: 'asyncUtilWrapper',
                            data: { name: node.name },
                        });
                    }
                }
            },
        };
    },
});
