"use strict";
var __importStar = (this && this.__importStar) || function (mod) {
    if (mod && mod.__esModule) return mod;
    var result = {};
    if (mod != null) for (var k in mod) if (Object.hasOwnProperty.call(mod, k)) result[k] = mod[k];
    result["default"] = mod;
    return result;
};
Object.defineProperty(exports, "__esModule", { value: true });
const util = __importStar(require("../util"));
const typescript_estree_1 = require("@typescript-eslint/typescript-estree");
exports.default = util.createRule({
    name: 'class-name-casing',
    meta: {
        type: 'suggestion',
        docs: {
            description: 'Require PascalCased class and interface names',
            tslintRuleName: 'class-name',
            category: 'Best Practices',
            recommended: 'error',
        },
        messages: {
            notPascalCased: "{{friendlyName}} '{{name}}' must be PascalCased.",
        },
        schema: [],
    },
    defaultOptions: [],
    create(context) {
        /**
         * Determine if the identifier name is PascalCased
         * @param name The identifier name
         */
        function isPascalCase(name) {
            return /^[A-Z][0-9A-Za-z]*$/.test(name);
        }
        /**
         * Report a class declaration as invalid
         * @param decl The declaration
         * @param id The name of the declaration
         */
        function report(decl, id) {
            let friendlyName;
            switch (decl.type) {
                case typescript_estree_1.AST_NODE_TYPES.ClassDeclaration:
                case typescript_estree_1.AST_NODE_TYPES.ClassExpression:
                    friendlyName = decl.abstract ? 'Abstract class' : 'Class';
                    break;
                case typescript_estree_1.AST_NODE_TYPES.TSInterfaceDeclaration:
                    friendlyName = 'Interface';
                    break;
                default:
                    friendlyName = decl.type;
            }
            context.report({
                node: id,
                messageId: 'notPascalCased',
                data: {
                    friendlyName,
                    name: id.name,
                },
            });
        }
        return {
            'ClassDeclaration, TSInterfaceDeclaration, ClassExpression'(node) {
                // class expressions (i.e. export default class {}) are OK
                if (node.id && !isPascalCase(node.id.name)) {
                    report(node, node.id);
                }
            },
            "VariableDeclarator[init.type='ClassExpression']"(node) {
                if (node.id.type === typescript_estree_1.AST_NODE_TYPES.ArrayPattern ||
                    node.id.type === typescript_estree_1.AST_NODE_TYPES.ObjectPattern) {
                    // TODO - handle the BindingPattern case maybe?
                    /*
                    // this example makes me barf, but it's valid code
                    var { bar } = class {
                      static bar() { return 2 }
                    }
                    */
                }
                else {
                    const id = node.id;
                    const nodeInit = node.init;
                    if (id && !nodeInit.id && !isPascalCase(id.name)) {
                        report(nodeInit, id);
                    }
                }
            },
        };
    },
});
//# sourceMappingURL=class-name-casing.js.map