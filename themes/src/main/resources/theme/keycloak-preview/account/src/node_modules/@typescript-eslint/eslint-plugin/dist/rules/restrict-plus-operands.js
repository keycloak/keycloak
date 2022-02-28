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
const typescript_1 = __importDefault(require("typescript"));
const util = __importStar(require("../util"));
exports.default = util.createRule({
    name: 'restrict-plus-operands',
    meta: {
        type: 'problem',
        docs: {
            description: 'When adding two variables, operands must both be of type number or of type string.',
            tslintRuleName: 'restrict-plus-operands',
            category: 'Best Practices',
            recommended: false,
        },
        messages: {
            notNumbers: "Operands of '+' operation must either be both strings or both numbers.",
            notStrings: "Operands of '+' operation must either be both strings or both numbers. Consider using a template literal.",
            notBigInts: "Operands of '+' operation must be both bigints.",
        },
        schema: [],
    },
    defaultOptions: [],
    create(context) {
        const service = util.getParserServices(context);
        const typeChecker = service.program.getTypeChecker();
        /**
         * Helper function to get base type of node
         * @param type type to be evaluated
         * @returns string, number or invalid
         */
        function getBaseTypeOfLiteralType(type) {
            if (type.isNumberLiteral()) {
                return 'number';
            }
            if (type.isStringLiteral()) {
                return 'string';
            }
            // is BigIntLiteral
            if (type.flags & typescript_1.default.TypeFlags.BigIntLiteral) {
                return 'bigint';
            }
            if (type.isUnion()) {
                const types = type.types.map(getBaseTypeOfLiteralType);
                return types.every(value => value === types[0]) ? types[0] : 'invalid';
            }
            const stringType = typeChecker.typeToString(type);
            if (stringType === 'number' || stringType === 'string') {
                return stringType;
            }
            return 'invalid';
        }
        /**
         * Helper function to get base type of node
         * @param node the node to be evaluated.
         */
        function getNodeType(node) {
            const tsNode = service.esTreeNodeToTSNodeMap.get(node);
            const type = typeChecker.getTypeAtLocation(tsNode);
            return getBaseTypeOfLiteralType(type);
        }
        return {
            "BinaryExpression[operator='+']"(node) {
                const leftType = getNodeType(node.left);
                const rightType = getNodeType(node.right);
                if (leftType === 'invalid' ||
                    rightType === 'invalid' ||
                    leftType !== rightType) {
                    if (leftType === 'string' || rightType === 'string') {
                        context.report({
                            node,
                            messageId: 'notStrings',
                        });
                    }
                    else if (leftType === 'bigint' || rightType === 'bigint') {
                        context.report({
                            node,
                            messageId: 'notBigInts',
                        });
                    }
                    else {
                        context.report({
                            node,
                            messageId: 'notNumbers',
                        });
                    }
                }
            },
        };
    },
});
//# sourceMappingURL=restrict-plus-operands.js.map