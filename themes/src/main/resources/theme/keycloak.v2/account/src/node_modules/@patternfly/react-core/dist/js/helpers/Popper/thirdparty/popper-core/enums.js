"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.modifierPhases = exports.afterWrite = exports.write = exports.beforeWrite = exports.afterMain = exports.main = exports.beforeMain = exports.afterRead = exports.read = exports.beforeRead = exports.placements = exports.variationPlacements = exports.reference = exports.popper = exports.viewport = exports.clippingParents = exports.end = exports.start = exports.basePlacements = exports.auto = exports.left = exports.right = exports.bottom = exports.top = void 0;
// @ts-nocheck
exports.top = 'top';
exports.bottom = 'bottom';
exports.right = 'right';
exports.left = 'left';
exports.auto = 'auto';
exports.basePlacements = [exports.top, exports.bottom, exports.right, exports.left];
exports.start = 'start';
exports.end = 'end';
exports.clippingParents = 'clippingParents';
exports.viewport = 'viewport';
exports.popper = 'popper';
exports.reference = 'reference';
exports.variationPlacements = exports.basePlacements.reduce((acc, placement) => acc.concat([`${placement}-${exports.start}`, `${placement}-${exports.end}`]), []);
exports.placements = [...exports.basePlacements, exports.auto].reduce((acc, placement) => acc.concat([placement, `${placement}-${exports.start}`, `${placement}-${exports.end}`]), []);
// modifiers that need to read the DOM
exports.beforeRead = 'beforeRead';
exports.read = 'read';
exports.afterRead = 'afterRead';
// pure-logic modifiers
exports.beforeMain = 'beforeMain';
exports.main = 'main';
exports.afterMain = 'afterMain';
// modifier with the purpose to write to the DOM (or write into a framework state)
exports.beforeWrite = 'beforeWrite';
exports.write = 'write';
exports.afterWrite = 'afterWrite';
exports.modifierPhases = [
    exports.beforeRead,
    exports.read,
    exports.afterRead,
    exports.beforeMain,
    exports.main,
    exports.afterMain,
    exports.beforeWrite,
    exports.write,
    exports.afterWrite
];
//# sourceMappingURL=enums.js.map