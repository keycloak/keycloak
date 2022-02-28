import { TSESTree } from '@typescript-eslint/typescript-estree';
import EslintScopeManager, { ScopeManagerOptions } from 'eslint-scope/lib/scope-manager';
import { Scope } from 'eslint-scope/lib/scope';
/**
 * based on eslint-scope
 */
export declare class ScopeManager extends EslintScopeManager {
    scopes: Scope[];
    globalScope: Scope;
    constructor(options: ScopeManagerOptions);
    /** @internal */
    __nestEnumScope(node: TSESTree.TSEnumDeclaration): Scope;
    /** @internal */
    __nestEmptyFunctionScope(node: TSESTree.TSDeclareFunction): Scope;
}
//# sourceMappingURL=scope-manager.d.ts.map