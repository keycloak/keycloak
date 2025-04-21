"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ActionListItem = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const ActionListItem = (_a) => {
    var { children, className = '' } = _a, props = tslib_1.__rest(_a, ["children", "className"]);
    return (React.createElement("div", Object.assign({ className: react_styles_1.css('pf-c-action-list__item', className) }, props), children));
};
exports.ActionListItem = ActionListItem;
exports.ActionListItem.displayName = 'ActionListItem';
//# sourceMappingURL=ActionListItem.js.map