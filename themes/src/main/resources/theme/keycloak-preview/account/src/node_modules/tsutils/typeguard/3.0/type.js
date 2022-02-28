"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const tslib_1 = require("tslib");
tslib_1.__exportStar(require("../2.9/type"), exports);
const ts = require("typescript");
function isTupleType(type) {
    return (type.flags & ts.TypeFlags.Object && type.objectFlags & ts.ObjectFlags.Tuple) !== 0;
}
exports.isTupleType = isTupleType;
