"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
var __importStar = (this && this.__importStar) || function (mod) {
    if (mod && mod.__esModule) return mod;
    var result = {};
    if (mod != null) for (var k in mod) if (Object.hasOwnProperty.call(mod, k)) result[k] = mod[k];
    result["default"] = mod;
    return result;
};
Object.defineProperty(exports, "__esModule", { value: true });
const typescript_estree_1 = require("@typescript-eslint/typescript-estree");
const no_useless_constructor_1 = __importDefault(require("eslint/lib/rules/no-useless-constructor"));
const util = __importStar(require("../util"));
/**
 * Check if method with accessibility is not useless
 */
function checkAccessibility(node) {
    switch (node.accessibility) {
        case 'protected':
        case 'private':
            return false;
        case 'public':
            if (node.parent &&
                node.parent.type === typescript_estree_1.AST_NODE_TYPES.ClassBody &&
                node.parent.parent &&
                'superClass' in node.parent.parent &&
                node.parent.parent.superClass) {
                return false;
            }
            break;
    }
    return true;
}
/**
 * Check if method is not unless due to typescript parameter properties
 */
function checkParams(node) {
    return (!node.value.params ||
        !node.value.params.some(param => param.type === typescript_estree_1.AST_NODE_TYPES.TSParameterProperty));
}
exports.default = util.createRule({
    name: 'no-useless-constructor',
    meta: {
        type: 'problem',
        docs: {
            description: 'Disallow unnecessary constructors',
            category: 'Best Practices',
            recommended: false,
        },
        schema: no_useless_constructor_1.default.meta.schema,
        messages: no_useless_constructor_1.default.meta.messages,
    },
    defaultOptions: [],
    create(context) {
        const rules = no_useless_constructor_1.default.create(context);
        return {
            MethodDefinition(node) {
                if (node.value &&
                    node.value.type === typescript_estree_1.AST_NODE_TYPES.FunctionExpression &&
                    checkAccessibility(node) &&
                    checkParams(node)) {
                    rules.MethodDefinition(node);
                }
            },
        };
    },
});
//# sourceMappingURL=no-useless-constructor.js.map