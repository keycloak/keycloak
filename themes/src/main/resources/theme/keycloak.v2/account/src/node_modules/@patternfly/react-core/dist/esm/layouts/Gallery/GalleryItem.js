import { __rest } from "tslib";
import * as React from 'react';
export const GalleryItem = (_a) => {
    var { children = null, component = 'div' } = _a, props = __rest(_a, ["children", "component"]);
    const Component = component;
    return React.createElement(Component, Object.assign({}, props), children);
};
GalleryItem.displayName = 'GalleryItem';
//# sourceMappingURL=GalleryItem.js.map