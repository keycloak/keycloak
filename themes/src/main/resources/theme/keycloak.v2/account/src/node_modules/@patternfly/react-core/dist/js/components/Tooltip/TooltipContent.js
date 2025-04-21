"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.TooltipContent = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const tooltip_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Tooltip/tooltip"));
const react_styles_1 = require("@patternfly/react-styles");
const TooltipContent = (_a) => {
    var { className, children, isLeftAligned } = _a, props = tslib_1.__rest(_a, ["className", "children", "isLeftAligned"]);
    return (React.createElement("div", Object.assign({ className: react_styles_1.css(tooltip_1.default.tooltipContent, isLeftAligned && tooltip_1.default.modifiers.textAlignLeft, className) }, props), children));
};
exports.TooltipContent = TooltipContent;
exports.TooltipContent.displayName = 'TooltipContent';
//# sourceMappingURL=TooltipContent.js.map