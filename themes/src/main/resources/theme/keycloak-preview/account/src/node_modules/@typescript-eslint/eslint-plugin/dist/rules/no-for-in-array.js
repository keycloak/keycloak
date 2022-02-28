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
    name: 'no-for-in-array',
    meta: {
        docs: {
            description: 'Disallow iterating over an array with a for-in loop',
            category: 'Best Practices',
            recommended: false,
            tslintName: 'no-for-in-array',
        },
        messages: {
            forInViolation: 'For-in loops over arrays are forbidden. Use for-of or array.forEach instead.',
        },
        schema: [],
        type: 'problem',
    },
    defaultOptions: [],
    create(context) {
        return {
            ForInStatement(node) {
                const parserServices = util.getParserServices(context);
                const checker = parserServices.program.getTypeChecker();
                const originalNode = parserServices.esTreeNodeToTSNodeMap.get(node);
                const type = checker.getTypeAtLocation(originalNode.expression);
                if ((typeof type.symbol !== 'undefined' &&
                    type.symbol.name === 'Array') ||
                    (type.flags & typescript_1.default.TypeFlags.StringLike) !== 0) {
                    context.report({
                        node,
                        messageId: 'forInViolation',
                    });
                }
            },
        };
    },
});
//# sourceMappingURL=no-for-in-array.js.map