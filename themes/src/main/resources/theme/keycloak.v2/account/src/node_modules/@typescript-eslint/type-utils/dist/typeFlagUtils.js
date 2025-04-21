"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || function (mod) {
    if (mod && mod.__esModule) return mod;
    var result = {};
    if (mod != null) for (var k in mod) if (k !== "default" && Object.prototype.hasOwnProperty.call(mod, k)) __createBinding(result, mod, k);
    __setModuleDefault(result, mod);
    return result;
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.isTypeFlagSet = exports.getTypeFlags = void 0;
const tsutils_1 = require("tsutils");
const ts = __importStar(require("typescript"));
/**
 * Gets all of the type flags in a type, iterating through unions automatically
 */
function getTypeFlags(type) {
    let flags = 0;
    for (const t of (0, tsutils_1.unionTypeParts)(type)) {
        flags |= t.flags;
    }
    return flags;
}
exports.getTypeFlags = getTypeFlags;
/**
 * Checks if the given type is (or accepts) the given flags
 * @param isReceiver true if the type is a receiving type (i.e. the type of a called function's parameter)
 */
function isTypeFlagSet(type, flagsToCheck, isReceiver) {
    const flags = getTypeFlags(type);
    if (isReceiver && flags & (ts.TypeFlags.Any | ts.TypeFlags.Unknown)) {
        return true;
    }
    return (flags & flagsToCheck) !== 0;
}
exports.isTypeFlagSet = isTypeFlagSet;
//# sourceMappingURL=typeFlagUtils.js.map