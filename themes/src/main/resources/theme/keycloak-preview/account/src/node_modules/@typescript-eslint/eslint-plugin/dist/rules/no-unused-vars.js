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
const no_unused_vars_1 = __importDefault(require("eslint/lib/rules/no-unused-vars"));
const util = __importStar(require("../util"));
exports.default = util.createRule({
    name: 'no-unused-vars',
    meta: {
        type: 'problem',
        docs: {
            description: 'Disallow unused variables',
            tslintRuleName: 'no-unused-variable',
            category: 'Variables',
            recommended: 'warn',
        },
        schema: no_unused_vars_1.default.meta.schema,
        messages: no_unused_vars_1.default.meta.messages,
    },
    defaultOptions: [],
    create(context) {
        const rules = no_unused_vars_1.default.create(context);
        /**
         * Mark heritage clause as used
         * @param node The node currently being traversed
         */
        function markHeritageAsUsed(node) {
            switch (node.type) {
                case typescript_estree_1.AST_NODE_TYPES.Identifier:
                    context.markVariableAsUsed(node.name);
                    break;
                case typescript_estree_1.AST_NODE_TYPES.MemberExpression:
                    markHeritageAsUsed(node.object);
                    break;
                case typescript_estree_1.AST_NODE_TYPES.CallExpression:
                    markHeritageAsUsed(node.callee);
                    break;
            }
        }
        return Object.assign({}, rules, {
            'TSTypeReference Identifier'(node) {
                context.markVariableAsUsed(node.name);
            },
            TSInterfaceHeritage(node) {
                if (node.expression) {
                    markHeritageAsUsed(node.expression);
                }
            },
            TSClassImplements(node) {
                if (node.expression) {
                    markHeritageAsUsed(node.expression);
                }
            },
            'TSParameterProperty Identifier'(node) {
                // just assume parameter properties are used
                context.markVariableAsUsed(node.name);
            },
            'TSEnumMember Identifier'(node) {
                context.markVariableAsUsed(node.name);
            },
            '*[declare=true] Identifier'(node) {
                context.markVariableAsUsed(node.name);
                const scope = context.getScope();
                const { variableScope } = scope;
                if (variableScope !== scope) {
                    const superVar = variableScope.set.get(node.name);
                    if (superVar) {
                        superVar.eslintUsed = true;
                    }
                }
            },
        });
    },
});
//# sourceMappingURL=no-unused-vars.js.map