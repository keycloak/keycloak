"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const scope_1 = require("eslint-scope/lib/scope");
/** The scope class for enum. */
class EnumScope extends scope_1.Scope {
    constructor(scopeManager, upperScope, block) {
        super(scopeManager, 'enum', upperScope, block, false);
    }
}
exports.EnumScope = EnumScope;
/** The scope class for empty functions. */
class EmptyFunctionScope extends scope_1.Scope {
    constructor(scopeManager, upperScope, block) {
        super(scopeManager, 'empty-function', upperScope, block, false);
    }
}
exports.EmptyFunctionScope = EmptyFunctionScope;
//# sourceMappingURL=scopes.js.map