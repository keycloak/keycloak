import { __rest } from "tslib";
import * as React from 'react';
export const LoginFooterItem = (_a) => {
    var { children = null, href = '#', target = '_blank' } = _a, props = __rest(_a, ["children", "href", "target"]);
    return React.isValidElement(children) ? (children) : (React.createElement("a", Object.assign({ target: target, href: href }, props), children));
};
LoginFooterItem.displayName = 'LoginFooterItem';
//# sourceMappingURL=LoginFooterItem.js.map