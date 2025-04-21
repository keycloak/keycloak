// @ts-nocheck
export const top = 'top';
export const bottom = 'bottom';
export const right = 'right';
export const left = 'left';
export const auto = 'auto';
export const basePlacements = [top, bottom, right, left];
export const start = 'start';
export const end = 'end';
export const clippingParents = 'clippingParents';
export const viewport = 'viewport';
export const popper = 'popper';
export const reference = 'reference';
export const variationPlacements = basePlacements.reduce((acc, placement) => acc.concat([`${placement}-${start}`, `${placement}-${end}`]), []);
export const placements = [...basePlacements, auto].reduce((acc, placement) => acc.concat([placement, `${placement}-${start}`, `${placement}-${end}`]), []);
// modifiers that need to read the DOM
export const beforeRead = 'beforeRead';
export const read = 'read';
export const afterRead = 'afterRead';
// pure-logic modifiers
export const beforeMain = 'beforeMain';
export const main = 'main';
export const afterMain = 'afterMain';
// modifier with the purpose to write to the DOM (or write into a framework state)
export const beforeWrite = 'beforeWrite';
export const write = 'write';
export const afterWrite = 'afterWrite';
export const modifierPhases = [
    beforeRead,
    read,
    afterRead,
    beforeMain,
    main,
    afterMain,
    beforeWrite,
    write,
    afterWrite
];
//# sourceMappingURL=enums.js.map