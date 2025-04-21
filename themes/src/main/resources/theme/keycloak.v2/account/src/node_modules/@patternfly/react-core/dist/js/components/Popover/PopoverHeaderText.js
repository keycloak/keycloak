"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.PopoverHeaderText = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const popover_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Popover/popover"));
const PopoverHeaderText = (_a) => {
    var { children, className } = _a, props = tslib_1.__rest(_a, ["children", "className"]);
    return (React.createElement("span", Object.assign({ className: react_styles_1.css(popover_1.default.popoverTitleText, className) }, props), children));
};
exports.PopoverHeaderText = PopoverHeaderText;
exports.PopoverHeaderText.displayName = 'PopoverHeaderText';
//# sourceMappingURL=PopoverHeaderText.js.map