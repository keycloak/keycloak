"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const ts = require("typescript");
const type_1 = require("../typeguard/type");
const util_1 = require("./util");
function isEmptyObjectType(type) {
    if (type_1.isObjectType(type) &&
        type.objectFlags & ts.ObjectFlags.Anonymous &&
        type.getProperties().length === 0 &&
        type.getCallSignatures().length === 0 &&
        type.getConstructSignatures().length === 0 &&
        type.getStringIndexType() === undefined &&
        type.getNumberIndexType() === undefined) {
        const baseTypes = type.getBaseTypes();
        return baseTypes === undefined || baseTypes.every(isEmptyObjectType);
    }
    return false;
}
exports.isEmptyObjectType = isEmptyObjectType;
function removeOptionalityFromType(checker, type) {
    if (!containsTypeWithFlag(type, ts.TypeFlags.Undefined))
        return type;
    const allowsNull = containsTypeWithFlag(type, ts.TypeFlags.Null);
    type = checker.getNonNullableType(type);
    return allowsNull ? checker.getNullableType(type, ts.TypeFlags.Null) : type;
}
exports.removeOptionalityFromType = removeOptionalityFromType;
function containsTypeWithFlag(type, flag) {
    for (const t of unionTypeParts(type))
        if (util_1.isTypeFlagSet(t, flag))
            return true;
    return false;
}
function isTypeAssignableToNumber(checker, type) {
    return isTypeAssignableTo(checker, type, ts.TypeFlags.NumberLike);
}
exports.isTypeAssignableToNumber = isTypeAssignableToNumber;
function isTypeAssignableToString(checker, type) {
    return isTypeAssignableTo(checker, type, ts.TypeFlags.StringLike);
}
exports.isTypeAssignableToString = isTypeAssignableToString;
function isTypeAssignableTo(checker, type, flags) {
    flags |= ts.TypeFlags.Any;
    let typeParametersSeen;
    return (function check(t) {
        if (type_1.isTypeParameter(t) && t.symbol !== undefined && t.symbol.declarations !== undefined) {
            if (typeParametersSeen === undefined) {
                typeParametersSeen = new Set([t]);
            }
            else if (!typeParametersSeen.has(t)) {
                typeParametersSeen.add(t);
            }
            else {
                return false;
            }
            const declaration = t.symbol.declarations[0];
            if (declaration.constraint === undefined)
                return true;
            return check(checker.getTypeFromTypeNode(declaration.constraint));
        }
        if (type_1.isUnionType(t))
            return t.types.every(check);
        if (type_1.isIntersectionType(t))
            return t.types.some(check);
        return util_1.isTypeFlagSet(t, flags);
    })(type);
}
function getCallSignaturesOfType(type) {
    if (type_1.isUnionType(type)) {
        const signatures = [];
        for (const t of type.types)
            signatures.push(...getCallSignaturesOfType(t));
        return signatures;
    }
    if (type_1.isIntersectionType(type)) {
        let signatures;
        for (const t of type.types) {
            const sig = getCallSignaturesOfType(t);
            if (sig.length !== 0) {
                if (signatures !== undefined)
                    return [];
                signatures = sig;
            }
        }
        return signatures === undefined ? [] : signatures;
    }
    return type.getCallSignatures();
}
exports.getCallSignaturesOfType = getCallSignaturesOfType;
function unionTypeParts(type) {
    return type_1.isUnionType(type) ? type.types : [type];
}
exports.unionTypeParts = unionTypeParts;
function isThenableType(checker, node, type = checker.getTypeAtLocation(node)) {
    for (const ty of unionTypeParts(checker.getApparentType(type))) {
        const then = ty.getProperty('then');
        if (then === undefined)
            continue;
        const thenType = checker.getTypeOfSymbolAtLocation(then, node);
        for (const t of unionTypeParts(thenType))
            for (const signature of t.getCallSignatures())
                if (signature.parameters.length !== 0 && isCallback(checker, signature.parameters[0], node))
                    return true;
    }
    return false;
}
exports.isThenableType = isThenableType;
function isCallback(checker, param, node) {
    let type = checker.getApparentType(checker.getTypeOfSymbolAtLocation(param, node));
    if (param.valueDeclaration.dotDotDotToken) {
        type = type.getNumberIndexType();
        if (type === undefined)
            return false;
    }
    for (const t of unionTypeParts(type))
        if (t.getCallSignatures().length !== 0)
            return true;
    return false;
}
function isFalsyType(type) {
    if (type.flags & (ts.TypeFlags.Undefined | ts.TypeFlags.Null | ts.TypeFlags.Void))
        return true;
    if (type_1.isLiteralType(type))
        return !type.value;
    if (type.flags & ts.TypeFlags.BooleanLiteral)
        return type.intrinsicName === 'false';
    return false;
}
exports.isFalsyType = isFalsyType;
