import { __rest } from "tslib";
import * as React from 'react';
import { DropdownItem } from '../Dropdown';
import { OverflowMenuContext } from './OverflowMenuContext';
export const OverflowMenuDropdownItem = (_a) => {
    var { children, isShared = false, index } = _a, additionalProps = __rest(_a, ["children", "isShared", "index"]);
    return (React.createElement(OverflowMenuContext.Consumer, null, value => (!isShared || value.isBelowBreakpoint) && (React.createElement(DropdownItem, Object.assign({ component: "button", index: index }, additionalProps), children))));
};
OverflowMenuDropdownItem.displayName = 'OverflowMenuDropdownItem';
//# sourceMappingURL=OverflowMenuDropdownItem.js.map