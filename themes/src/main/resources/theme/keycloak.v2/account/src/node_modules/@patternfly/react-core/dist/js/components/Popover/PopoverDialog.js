"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.PopoverDialog = exports.PopoverPosition = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const popover_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Popover/popover"));
const react_styles_1 = require("@patternfly/react-styles");
exports.PopoverPosition = {
    top: 'top',
    bottom: 'bottom',
    left: 'left',
    right: 'right'
};
const PopoverDialog = (_a) => {
    var { position = 'top', children = null, className = null } = _a, props = tslib_1.__rest(_a, ["position", "children", "className"]);
    return (React.createElement("div", Object.assign({ className: react_styles_1.css(popover_1.default.popover, popover_1.default.modifiers[position] || popover_1.default.modifiers.top, className), role: "dialog", "aria-modal": "true" }, props), children));
};
exports.PopoverDialog = PopoverDialog;
exports.PopoverDialog.displayName = 'PopoverDialog';
//# sourceMappingURL=PopoverDialog.js.map