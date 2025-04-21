"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.detectOverflow = exports.defaultModifiers = exports.popperGenerator = exports.createPopper = void 0;
const tslib_1 = require("tslib");
// @ts-nocheck
const _1 = require(".");
Object.defineProperty(exports, "popperGenerator", { enumerable: true, get: function () { return _1.popperGenerator; } });
Object.defineProperty(exports, "detectOverflow", { enumerable: true, get: function () { return _1.detectOverflow; } });
const eventListeners_1 = tslib_1.__importDefault(require("./modifiers/eventListeners"));
const popperOffsets_1 = tslib_1.__importDefault(require("./modifiers/popperOffsets"));
const computeStyles_1 = tslib_1.__importDefault(require("./modifiers/computeStyles"));
const applyStyles_1 = tslib_1.__importDefault(require("./modifiers/applyStyles"));
const offset_1 = tslib_1.__importDefault(require("./modifiers/offset"));
const flip_1 = tslib_1.__importDefault(require("./modifiers/flip"));
const preventOverflow_1 = tslib_1.__importDefault(require("./modifiers/preventOverflow"));
const arrow_1 = tslib_1.__importDefault(require("./modifiers/arrow"));
const hide_1 = tslib_1.__importDefault(require("./modifiers/hide"));
tslib_1.__exportStar(require("./types"), exports);
const defaultModifiers = [
    eventListeners_1.default,
    popperOffsets_1.default,
    computeStyles_1.default,
    applyStyles_1.default,
    offset_1.default,
    flip_1.default,
    preventOverflow_1.default,
    arrow_1.default,
    hide_1.default
];
exports.defaultModifiers = defaultModifiers;
const createPopper = _1.popperGenerator({ defaultModifiers });
exports.createPopper = createPopper;
//# sourceMappingURL=popper.js.map