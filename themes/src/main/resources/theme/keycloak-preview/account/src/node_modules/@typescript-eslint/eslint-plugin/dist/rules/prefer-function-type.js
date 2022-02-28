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
exports.default = util.createRule({
    name: 'prefer-function-type',
    meta: {
        docs: {
            description: 'Use function types instead of interfaces with call signatures',
            category: 'Best Practices',
            recommended: false,
            tslintName: 'callable-types',
        },
        fixable: 'code',
        messages: {
            functionTypeOverCallableType: "{{ type }} has only a call signature - use '{{ sigSuggestion }}' instead.",
        },
        schema: [],
        type: 'suggestion',
    },
    defaultOptions: [],
    create(context) {
        const sourceCode = context.getSourceCode();
        /**
         * Checks if there the interface has exactly one supertype that isn't named 'Function'
         * @param node The node being checked
         */
        function hasOneSupertype(node) {
            if (!node.extends || node.extends.length === 0) {
                return false;
            }
            if (node.extends.length !== 1) {
                return true;
            }
            const expr = node.extends[0].expression;
            return (expr.type !== typescript_estree_1.AST_NODE_TYPES.Identifier || expr.name !== 'Function');
        }
        /**
         * @param parent The parent of the call signature causing the diagnostic
         */
        function shouldWrapSuggestion(parent) {
            if (!parent) {
                return false;
            }
            switch (parent.type) {
                case typescript_estree_1.AST_NODE_TYPES.TSUnionType:
                case typescript_estree_1.AST_NODE_TYPES.TSIntersectionType:
                case typescript_estree_1.AST_NODE_TYPES.TSArrayType:
                    return true;
                default:
                    return false;
            }
        }
        /**
         * @param call The call signature causing the diagnostic
         * @param parent The parent of the call
         * @returns The suggestion to report
         */
        function renderSuggestion(call, parent) {
            const start = call.range[0];
            const colonPos = call.returnType.range[0] - start;
            const text = sourceCode.getText().slice(start, call.range[1]);
            let suggestion = `${text.slice(0, colonPos)} =>${text.slice(colonPos + 1)}`;
            if (shouldWrapSuggestion(parent.parent)) {
                suggestion = `(${suggestion})`;
            }
            if (parent.type === typescript_estree_1.AST_NODE_TYPES.TSInterfaceDeclaration) {
                if (typeof parent.typeParameters !== 'undefined') {
                    return `type ${sourceCode
                        .getText()
                        .slice(parent.id.range[0], parent.typeParameters.range[1])} = ${suggestion}`;
                }
                return `type ${parent.id.name} = ${suggestion}`;
            }
            return suggestion.endsWith(';') ? suggestion.slice(0, -1) : suggestion;
        }
        /**
         * @param member The TypeElement being checked
         * @param node The parent of member being checked
         */
        function checkMember(member, node) {
            if ((member.type === typescript_estree_1.AST_NODE_TYPES.TSCallSignatureDeclaration ||
                member.type === typescript_estree_1.AST_NODE_TYPES.TSConstructSignatureDeclaration) &&
                typeof member.returnType !== 'undefined') {
                const suggestion = renderSuggestion(member, node);
                const fixStart = node.type === typescript_estree_1.AST_NODE_TYPES.TSTypeLiteral
                    ? node.range[0]
                    : sourceCode
                        .getTokens(node)
                        .filter(token => token.type === typescript_estree_1.AST_TOKEN_TYPES.Keyword &&
                        token.value === 'interface')[0].range[0];
                context.report({
                    node: member,
                    messageId: 'functionTypeOverCallableType',
                    data: {
                        type: node.type === typescript_estree_1.AST_NODE_TYPES.TSTypeLiteral
                            ? 'Type literal'
                            : 'Interface',
                        sigSuggestion: suggestion,
                    },
                    fix(fixer) {
                        return fixer.replaceTextRange([fixStart, node.range[1]], suggestion);
                    },
                });
            }
        }
        return {
            TSInterfaceDeclaration(node) {
                if (!hasOneSupertype(node) && node.body.body.length === 1) {
                    checkMember(node.body.body[0], node);
                }
            },
            'TSTypeLiteral[members.length = 1]'(node) {
                checkMember(node.members[0], node);
            },
        };
    },
});
//# sourceMappingURL=prefer-function-type.js.map