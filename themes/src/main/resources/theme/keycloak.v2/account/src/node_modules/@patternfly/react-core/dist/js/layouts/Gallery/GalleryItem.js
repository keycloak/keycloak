"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.GalleryItem = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const GalleryItem = (_a) => {
    var { children = null, component = 'div' } = _a, props = tslib_1.__rest(_a, ["children", "component"]);
    const Component = component;
    return React.createElement(Component, Object.assign({}, props), children);
};
exports.GalleryItem = GalleryItem;
exports.GalleryItem.displayName = 'GalleryItem';
//# sourceMappingURL=GalleryItem.js.map