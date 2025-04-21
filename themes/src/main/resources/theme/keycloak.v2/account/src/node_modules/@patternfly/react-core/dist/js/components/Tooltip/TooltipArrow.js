"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.TooltipArrow = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const tooltip_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Tooltip/tooltip"));
const react_styles_1 = require("@patternfly/react-styles");
const TooltipArrow = (_a) => {
    var { className } = _a, props = tslib_1.__rest(_a, ["className"]);
    return React.createElement("div", Object.assign({ className: react_styles_1.css(tooltip_1.default.tooltipArrow, className) }, props));
};
exports.TooltipArrow = TooltipArrow;
exports.TooltipArrow.displayName = 'TooltipArrow';
//# sourceMappingURL=TooltipArrow.js.map