"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.PopoverContent = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const popover_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Popover/popover"));
const react_styles_1 = require("@patternfly/react-styles");
const PopoverContent = (_a) => {
    var { className = null, children } = _a, props = tslib_1.__rest(_a, ["className", "children"]);
    return (React.createElement("div", Object.assign({ className: react_styles_1.css(popover_1.default.popoverContent, className) }, props), children));
};
exports.PopoverContent = PopoverContent;
exports.PopoverContent.displayName = 'PopoverContent';
//# sourceMappingURL=PopoverContent.js.map