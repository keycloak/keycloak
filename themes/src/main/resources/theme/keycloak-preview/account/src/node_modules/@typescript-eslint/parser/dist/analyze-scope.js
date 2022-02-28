"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const scope_manager_1 = require("./scope/scope-manager");
const definition_1 = require("eslint-scope/lib/definition");
const pattern_visitor_1 = __importDefault(require("eslint-scope/lib/pattern-visitor"));
const reference_1 = __importDefault(require("eslint-scope/lib/reference"));
const referencer_1 = __importDefault(require("eslint-scope/lib/referencer"));
const eslint_visitor_keys_1 = require("eslint-visitor-keys");
const visitor_keys_1 = require("./visitor-keys");
const typescript_estree_1 = require("@typescript-eslint/typescript-estree");
/**
 * Define the override function of `Scope#__define` for global augmentation.
 * @param {Function} define The original Scope#__define method.
 * @returns {Function} The override function.
 */
function overrideDefine(define) {
    return /* @this {Scope} */ function (node, definition) {
        define.call(this, node, definition);
        // Set `variable.eslintUsed` to tell ESLint that the variable is exported.
        const variable = this.set.get(node.name);
        if (variable) {
            variable.eslintUsed = true;
        }
    };
}
class PatternVisitor extends pattern_visitor_1.default {
    constructor(options, rootPattern, callback) {
        super(options, rootPattern, callback);
    }
    Identifier(node) {
        super.Identifier(node);
        if (node.decorators) {
            this.rightHandNodes.push(...node.decorators);
        }
        if (node.typeAnnotation) {
            this.rightHandNodes.push(node.typeAnnotation);
        }
    }
    ArrayPattern(node) {
        node.elements.forEach(this.visit, this);
        if (node.decorators) {
            this.rightHandNodes.push(...node.decorators);
        }
        if (node.typeAnnotation) {
            this.rightHandNodes.push(node.typeAnnotation);
        }
    }
    ObjectPattern(node) {
        node.properties.forEach(this.visit, this);
        if (node.decorators) {
            this.rightHandNodes.push(...node.decorators);
        }
        if (node.typeAnnotation) {
            this.rightHandNodes.push(node.typeAnnotation);
        }
    }
    RestElement(node) {
        super.RestElement(node);
        if (node.decorators) {
            this.rightHandNodes.push(...node.decorators);
        }
        if (node.typeAnnotation) {
            this.rightHandNodes.push(node.typeAnnotation);
        }
    }
    TSParameterProperty(node) {
        this.visit(node.parameter);
        if (node.decorators) {
            this.rightHandNodes.push(...node.decorators);
        }
    }
}
class Referencer extends referencer_1.default {
    constructor(options, scopeManager) {
        super(options, scopeManager);
        this.typeMode = false;
    }
    /**
     * Override to use PatternVisitor we overrode.
     * @param node The Identifier node to visit.
     * @param [options] The flag to visit right-hand side nodes.
     * @param callback The callback function for left-hand side nodes.
     */
    visitPattern(node, options, callback) {
        if (!node) {
            return;
        }
        if (typeof options === 'function') {
            callback = options;
            options = { processRightHandNodes: false };
        }
        const visitor = new PatternVisitor(this.options, node, callback);
        visitor.visit(node);
        if (options.processRightHandNodes) {
            // @ts-ignore
            visitor.rightHandNodes.forEach(this.visit, this);
        }
    }
    /**
     * Override.
     * Visit `node.typeParameters` and `node.returnType` additionally to find `typeof` expressions.
     * @param node The function node to visit.
     */
    visitFunction(node) {
        const { type, id, typeParameters, params, returnType, body } = node;
        const scopeManager = this.scopeManager;
        const upperScope = this.currentScope();
        // Process the name.
        if (type === 'FunctionDeclaration' && id) {
            upperScope.__define(id, new definition_1.Definition('FunctionName', id, node, null, null, null));
            // Remove overload definition to avoid confusion of no-redeclare rule.
            const { defs, identifiers } = upperScope.set.get(id.name);
            for (let i = 0; i < defs.length; ++i) {
                const def = defs[i];
                if (def.type === 'FunctionName' &&
                    // @ts-ignore
                    def.node.type === 'TSDeclareFunction') {
                    defs.splice(i, 1);
                    identifiers.splice(i, 1);
                    break;
                }
            }
        }
        else if (type === 'FunctionExpression' && id) {
            scopeManager.__nestFunctionExpressionNameScope(node);
        }
        // Open the function scope.
        scopeManager.__nestFunctionScope(node, this.isInnerMethodDefinition);
        const innerScope = this.currentScope();
        // Process the type parameters
        this.visit(typeParameters);
        // Process parameter declarations.
        for (let i = 0; i < params.length; ++i) {
            this.visitPattern(params[i], { processRightHandNodes: true }, (pattern, info) => {
                if (pattern.type !== typescript_estree_1.AST_NODE_TYPES.Identifier ||
                    pattern.name !== 'this') {
                    innerScope.__define(pattern, new definition_1.ParameterDefinition(pattern, node, i, info.rest));
                    this.referencingDefaultValue(pattern, info.assignments, null, true);
                }
            });
        }
        // Process the return type.
        this.visit(returnType);
        // Process the body.
        if (body && body.type === 'BlockStatement') {
            this.visitChildren(body);
        }
        else {
            this.visit(body);
        }
        // Close the function scope.
        this.close(node);
    }
    /**
     * Override.
     * Visit decorators.
     * @param node The class node to visit.
     */
    visitClass(node) {
        this.visitDecorators(node.decorators);
        const upperTypeMode = this.typeMode;
        this.typeMode = true;
        if (node.superTypeParameters) {
            this.visit(node.superTypeParameters);
        }
        if (node.implements) {
            node.implements.forEach(this.visit, this);
        }
        this.typeMode = upperTypeMode;
        super.visitClass(node);
    }
    /**
     * Visit typeParameters.
     * @param node The node to visit.
     */
    visitTypeParameters(node) {
        if (node.typeParameters) {
            const upperTypeMode = this.typeMode;
            this.typeMode = true;
            this.visit(node.typeParameters);
            this.typeMode = upperTypeMode;
        }
    }
    /**
     * Override.
     */
    JSXOpeningElement(node) {
        this.visit(node.name);
        this.visitTypeParameters(node);
        node.attributes.forEach(this.visit, this);
    }
    /**
     * Override.
     * Don't create the reference object in the type mode.
     * @param node The Identifier node to visit.
     */
    Identifier(node) {
        this.visitDecorators(node.decorators);
        if (!this.typeMode) {
            super.Identifier(node);
        }
        this.visit(node.typeAnnotation);
    }
    /**
     * Override.
     * Visit decorators.
     * @param node The MethodDefinition node to visit.
     */
    MethodDefinition(node) {
        this.visitDecorators(node.decorators);
        super.MethodDefinition(node);
    }
    /**
     * Don't create the reference object for the key if not computed.
     * @param node The ClassProperty node to visit.
     */
    ClassProperty(node) {
        const upperTypeMode = this.typeMode;
        const { computed, decorators, key, typeAnnotation, value } = node;
        this.typeMode = false;
        this.visitDecorators(decorators);
        if (computed) {
            this.visit(key);
        }
        this.typeMode = true;
        this.visit(typeAnnotation);
        this.typeMode = false;
        this.visit(value);
        this.typeMode = upperTypeMode;
    }
    /**
     * Visit new expression.
     * @param node The NewExpression node to visit.
     */
    NewExpression(node) {
        this.visitTypeParameters(node);
        this.visit(node.callee);
        node.arguments.forEach(this.visit, this);
    }
    /**
     * Override.
     * Visit call expression.
     * @param node The CallExpression node to visit.
     */
    CallExpression(node) {
        this.visitTypeParameters(node);
        this.visit(node.callee);
        node.arguments.forEach(this.visit, this);
    }
    /**
     * Define the variable of this function declaration only once.
     * Because to avoid confusion of `no-redeclare` rule by overloading.
     * @param node The TSDeclareFunction node to visit.
     */
    TSDeclareFunction(node) {
        const scopeManager = this.scopeManager;
        const upperScope = this.currentScope();
        const { id, typeParameters, params, returnType } = node;
        // Ignore this if other overloadings have already existed.
        if (id) {
            const variable = upperScope.set.get(id.name);
            const defs = variable && variable.defs;
            const existed = defs && defs.some(d => d.type === 'FunctionName');
            if (!existed) {
                upperScope.__define(id, new definition_1.Definition('FunctionName', id, node, null, null, null));
            }
        }
        // Open the function scope.
        scopeManager.__nestEmptyFunctionScope(node);
        const innerScope = this.currentScope();
        // Process the type parameters
        this.visit(typeParameters);
        // Process parameter declarations.
        for (let i = 0; i < params.length; ++i) {
            this.visitPattern(params[i], { processRightHandNodes: true }, (pattern, info) => {
                innerScope.__define(pattern, new definition_1.ParameterDefinition(pattern, node, i, info.rest));
                // Set `variable.eslintUsed` to tell ESLint that the variable is used.
                const variable = innerScope.set.get(pattern.name);
                if (variable) {
                    variable.eslintUsed = true;
                }
                this.referencingDefaultValue(pattern, info.assignments, null, true);
            });
        }
        // Process the return type.
        this.visit(returnType);
        // Close the function scope.
        this.close(node);
    }
    /**
     * Create reference objects for the references in parameters and return type.
     * @param node The TSEmptyBodyFunctionExpression node to visit.
     */
    TSEmptyBodyFunctionExpression(node) {
        const upperTypeMode = this.typeMode;
        const { typeParameters, params, returnType } = node;
        this.typeMode = true;
        this.visit(typeParameters);
        params.forEach(this.visit, this);
        this.visit(returnType);
        this.typeMode = upperTypeMode;
    }
    /**
     * Don't make variable because it declares only types.
     * Switch to the type mode and visit child nodes to find `typeof x` expression in type declarations.
     * @param node The TSInterfaceDeclaration node to visit.
     */
    TSInterfaceDeclaration(node) {
        this.visitTypeNodes(node);
    }
    /**
     * Don't make variable because it declares only types.
     * Switch to the type mode and visit child nodes to find `typeof x` expression in type declarations.
     * @param node The TSClassImplements node to visit.
     */
    TSClassImplements(node) {
        this.visitTypeNodes(node);
    }
    /**
     * Don't make variable because it declares only types.
     * Switch to the type mode and visit child nodes to find `typeof x` expression in type declarations.
     * @param node The TSIndexSignature node to visit.
     */
    TSIndexSignature(node) {
        this.visitTypeNodes(node);
    }
    /**
     * Visit type assertion.
     * @param node The TSTypeAssertion node to visit.
     */
    TSTypeAssertion(node) {
        if (this.typeMode) {
            this.visit(node.typeAnnotation);
        }
        else {
            this.typeMode = true;
            this.visit(node.typeAnnotation);
            this.typeMode = false;
        }
        this.visit(node.expression);
    }
    /**
     * Visit as expression.
     * @param node The TSAsExpression node to visit.
     */
    TSAsExpression(node) {
        this.visit(node.expression);
        if (this.typeMode) {
            this.visit(node.typeAnnotation);
        }
        else {
            this.typeMode = true;
            this.visit(node.typeAnnotation);
            this.typeMode = false;
        }
    }
    /**
     * Switch to the type mode and visit child nodes to find `typeof x` expression in type declarations.
     * @param node The TSTypeAnnotation node to visit.
     */
    TSTypeAnnotation(node) {
        this.visitTypeNodes(node);
    }
    /**
     * Switch to the type mode and visit child nodes to find `typeof x` expression in type declarations.
     * @param node The TSTypeParameterDeclaration node to visit.
     */
    TSTypeParameterDeclaration(node) {
        this.visitTypeNodes(node);
    }
    /**
     * Create reference objects for the references in `typeof` expression.
     * @param node The TSTypeQuery node to visit.
     */
    TSTypeQuery(node) {
        if (this.typeMode) {
            this.typeMode = false;
            this.visitChildren(node);
            this.typeMode = true;
        }
        else {
            this.visitChildren(node);
        }
    }
    /**
     * @param node The TSTypeParameter node to visit.
     */
    TSTypeParameter(node) {
        this.visitTypeNodes(node);
    }
    /**
     * @param node The TSInferType node to visit.
     */
    TSInferType(node) {
        this.visitTypeNodes(node);
    }
    /**
     * @param node The TSTypeReference node to visit.
     */
    TSTypeReference(node) {
        this.visitTypeNodes(node);
    }
    /**
     * @param node The TSTypeLiteral node to visit.
     */
    TSTypeLiteral(node) {
        this.visitTypeNodes(node);
    }
    /**
     * @param node The TSLiteralType node to visit.
     */
    TSLiteralType(node) {
        this.visitTypeNodes(node);
    }
    /**
     * @param node The TSIntersectionType node to visit.
     */
    TSIntersectionType(node) {
        this.visitTypeNodes(node);
    }
    /**
     * @param node The TSConditionalType node to visit.
     */
    TSConditionalType(node) {
        this.visitTypeNodes(node);
    }
    /**
     * @param node The TSIndexedAccessType node to visit.
     */
    TSIndexedAccessType(node) {
        this.visitTypeNodes(node);
    }
    /**
     * @param node The TSMappedType node to visit.
     */
    TSMappedType(node) {
        this.visitTypeNodes(node);
    }
    /**
     * @param node The TSOptionalType node to visit.
     */
    TSOptionalType(node) {
        this.visitTypeNodes(node);
    }
    /**
     * @param node The TSParenthesizedType node to visit.
     */
    TSParenthesizedType(node) {
        this.visitTypeNodes(node);
    }
    /**
     * @param node The TSRestType node to visit.
     */
    TSRestType(node) {
        this.visitTypeNodes(node);
    }
    /**
     * @param node The TSTupleType node to visit.
     */
    TSTupleType(node) {
        this.visitTypeNodes(node);
    }
    /**
     * Create reference objects for the object part. (This is `obj.prop`)
     * @param node The TSQualifiedName node to visit.
     */
    TSQualifiedName(node) {
        this.visit(node.left);
    }
    /**
     * Create reference objects for the references in computed keys.
     * @param node The TSPropertySignature node to visit.
     */
    TSPropertySignature(node) {
        const upperTypeMode = this.typeMode;
        const { computed, key, typeAnnotation, initializer } = node;
        if (computed) {
            this.typeMode = false;
            this.visit(key);
            this.typeMode = true;
        }
        else {
            this.typeMode = true;
            this.visit(key);
        }
        this.visit(typeAnnotation);
        this.visit(initializer);
        this.typeMode = upperTypeMode;
    }
    /**
     * Create reference objects for the references in computed keys.
     * @param node The TSMethodSignature node to visit.
     */
    TSMethodSignature(node) {
        const upperTypeMode = this.typeMode;
        const { computed, key, typeParameters, params, returnType } = node;
        if (computed) {
            this.typeMode = false;
            this.visit(key);
            this.typeMode = true;
        }
        else {
            this.typeMode = true;
            this.visit(key);
        }
        this.visit(typeParameters);
        params.forEach(this.visit, this);
        this.visit(returnType);
        this.typeMode = upperTypeMode;
    }
    /**
     * Create variable object for the enum.
     * The enum declaration creates a scope for the enum members.
     *
     * enum E {
     *   A,
     *   B,
     *   C = A + B // A and B are references to the enum member.
     * }
     *
     * const a = 0
     * enum E {
     *   A = a // a is above constant.
     * }
     *
     * @param node The TSEnumDeclaration node to visit.
     */
    TSEnumDeclaration(node) {
        const { id, members } = node;
        const scopeManager = this.scopeManager;
        const scope = this.currentScope();
        if (id) {
            scope.__define(id, new definition_1.Definition('EnumName', id, node));
        }
        scopeManager.__nestEnumScope(node);
        for (const member of members) {
            this.visit(member);
        }
        this.close(node);
    }
    /**
     * Create variable object for the enum member and create reference object for the initializer.
     * And visit the initializer.
     *
     * @param node The TSEnumMember node to visit.
     */
    TSEnumMember(node) {
        const { id, initializer } = node;
        const scope = this.currentScope();
        scope.__define(id, new definition_1.Definition('EnumMemberName', id, node));
        if (initializer) {
            scope.__referencing(id, reference_1.default.WRITE, initializer, null, false, true);
            this.visit(initializer);
        }
    }
    /**
     * Create the variable object for the module name, and visit children.
     * @param node The TSModuleDeclaration node to visit.
     */
    TSModuleDeclaration(node) {
        const scope = this.currentScope();
        const { id, body } = node;
        if (node.global) {
            this.visitGlobalAugmentation(node);
            return;
        }
        if (id && id.type === 'Identifier') {
            scope.__define(id, new definition_1.Definition('NamespaceName', id, node, null, null, null));
        }
        this.visit(body);
    }
    TSTypeAliasDeclaration(node) {
        this.typeMode = true;
        this.visitChildren(node);
        this.typeMode = false;
    }
    /**
     * Process the module block.
     * @param node The TSModuleBlock node to visit.
     */
    TSModuleBlock(node) {
        this.scopeManager.__nestBlockScope(node);
        this.visitChildren(node);
        this.close(node);
    }
    TSAbstractClassProperty(node) {
        this.ClassProperty(node);
    }
    TSAbstractMethodDefinition(node) {
        this.MethodDefinition(node);
    }
    /**
     * Process import equal declaration
     * @param node The TSImportEqualsDeclaration node to visit.
     */
    TSImportEqualsDeclaration(node) {
        const { id, moduleReference } = node;
        if (id && id.type === 'Identifier') {
            this.currentScope().__define(id, new definition_1.Definition('ImportBinding', id, node, null, null, null));
        }
        this.visit(moduleReference);
    }
    /**
     * Process the global augmentation.
     * 1. Set the global scope as the current scope.
     * 2. Configure the global scope to set `variable.eslintUsed = true` for all defined variables. This means `no-unused-vars` doesn't warn those.
     * @param node The TSModuleDeclaration node to visit.
     */
    visitGlobalAugmentation(node) {
        const scopeManager = this.scopeManager;
        const currentScope = this.currentScope();
        const globalScope = scopeManager.globalScope;
        const originalDefine = globalScope.__define;
        globalScope.__define = overrideDefine(originalDefine);
        scopeManager.__currentScope = globalScope;
        // Skip TSModuleBlock to avoid to create that block scope.
        if (node.body && node.body.type === 'TSModuleBlock') {
            node.body.body.forEach(this.visit, this);
        }
        scopeManager.__currentScope = currentScope;
        globalScope.__define = originalDefine;
    }
    /**
     * Process decorators.
     * @param decorators The decorator nodes to visit.
     */
    visitDecorators(decorators) {
        if (decorators) {
            decorators.forEach(this.visit, this);
        }
    }
    /**
     * Process all child of type nodes
     * @param node node to be processed
     */
    visitTypeNodes(node) {
        if (this.typeMode) {
            this.visitChildren(node);
        }
        else {
            this.typeMode = true;
            this.visitChildren(node);
            this.typeMode = false;
        }
    }
}
function analyzeScope(ast, parserOptions) {
    const options = {
        ignoreEval: true,
        optimistic: false,
        directive: false,
        nodejsScope: parserOptions.sourceType === 'script' &&
            (parserOptions.ecmaFeatures &&
                parserOptions.ecmaFeatures.globalReturn) === true,
        impliedStrict: false,
        sourceType: parserOptions.sourceType,
        ecmaVersion: parserOptions.ecmaVersion || 2018,
        childVisitorKeys: visitor_keys_1.visitorKeys,
        fallback: eslint_visitor_keys_1.getKeys,
    };
    const scopeManager = new scope_manager_1.ScopeManager(options);
    const referencer = new Referencer(options, scopeManager);
    referencer.visit(ast);
    return scopeManager;
}
exports.analyzeScope = analyzeScope;
//# sourceMappingURL=analyze-scope.js.map