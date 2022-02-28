"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const scope_manager_1 = __importDefault(require("eslint-scope/lib/scope-manager"));
const scopes_1 = require("./scopes");
/**
 * based on eslint-scope
 */
class ScopeManager extends scope_manager_1.default {
    constructor(options) {
        super(options);
    }
    /** @internal */
    __nestEnumScope(node) {
        return this.__nestScope(new scopes_1.EnumScope(this, this.__currentScope, node));
    }
    /** @internal */
    __nestEmptyFunctionScope(node) {
        return this.__nestScope(new scopes_1.EmptyFunctionScope(this, this.__currentScope, node));
    }
}
exports.ScopeManager = ScopeManager;
//# sourceMappingURL=scope-manager.js.map