"use strict";
var __importStar = (this && this.__importStar) || function (mod) {
    if (mod && mod.__esModule) return mod;
    var result = {};
    if (mod != null) for (var k in mod) if (Object.hasOwnProperty.call(mod, k)) result[k] = mod[k];
    result["default"] = mod;
    return result;
};
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const tsutils = __importStar(require("tsutils"));
const typescript_1 = __importDefault(require("typescript"));
/**
 * @param type Type being checked by name.
 * @param allowedNames Symbol names checking on the type.
 * @returns Whether the type is, extends, or contains any of the allowed names.
 */
function containsTypeByName(type, allowedNames) {
    if (tsutils.isTypeFlagSet(type, typescript_1.default.TypeFlags.Any | typescript_1.default.TypeFlags.Unknown)) {
        return true;
    }
    if (tsutils.isTypeReference(type)) {
        type = type.target;
    }
    if (typeof type.symbol !== 'undefined' &&
        allowedNames.has(type.symbol.name)) {
        return true;
    }
    if (tsutils.isUnionOrIntersectionType(type)) {
        return type.types.some(t => containsTypeByName(t, allowedNames));
    }
    const bases = type.getBaseTypes();
    return (typeof bases !== 'undefined' &&
        bases.some(t => containsTypeByName(t, allowedNames)));
}
exports.containsTypeByName = containsTypeByName;
//# sourceMappingURL=types.js.map