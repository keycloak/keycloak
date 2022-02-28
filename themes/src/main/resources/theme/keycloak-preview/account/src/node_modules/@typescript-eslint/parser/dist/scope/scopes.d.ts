import { Scope } from 'eslint-scope/lib/scope';
import { ScopeManager } from './scope-manager';
import { TSESTree } from '@typescript-eslint/typescript-estree';
/** The scope class for enum. */
export declare class EnumScope extends Scope {
    constructor(scopeManager: ScopeManager, upperScope: Scope, block: TSESTree.TSEnumDeclaration | null);
}
/** The scope class for empty functions. */
export declare class EmptyFunctionScope extends Scope {
    constructor(scopeManager: ScopeManager, upperScope: Scope, block: TSESTree.TSDeclareFunction | null);
}
//# sourceMappingURL=scopes.d.ts.map