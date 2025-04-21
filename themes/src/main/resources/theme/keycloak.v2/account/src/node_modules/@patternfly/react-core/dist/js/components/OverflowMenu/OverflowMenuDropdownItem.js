"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.OverflowMenuDropdownItem = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const Dropdown_1 = require("../Dropdown");
const OverflowMenuContext_1 = require("./OverflowMenuContext");
const OverflowMenuDropdownItem = (_a) => {
    var { children, isShared = false, index } = _a, additionalProps = tslib_1.__rest(_a, ["children", "isShared", "index"]);
    return (React.createElement(OverflowMenuContext_1.OverflowMenuContext.Consumer, null, value => (!isShared || value.isBelowBreakpoint) && (React.createElement(Dropdown_1.DropdownItem, Object.assign({ component: "button", index: index }, additionalProps), children))));
};
exports.OverflowMenuDropdownItem = OverflowMenuDropdownItem;
exports.OverflowMenuDropdownItem.displayName = 'OverflowMenuDropdownItem';
//# sourceMappingURL=OverflowMenuDropdownItem.js.map