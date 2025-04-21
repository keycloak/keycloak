"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ProgressBar = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const progress_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Progress/progress"));
const react_styles_1 = require("@patternfly/react-styles");
const ProgressBar = (_a) => {
    var { progressBarAriaProps, className = '', children = null, value } = _a, props = tslib_1.__rest(_a, ["progressBarAriaProps", "className", "children", "value"]);
    return (React.createElement("div", Object.assign({}, props, { className: react_styles_1.css(progress_1.default.progressBar, className) }, progressBarAriaProps),
        React.createElement("div", { className: react_styles_1.css(progress_1.default.progressIndicator), style: { width: `${value}%` } },
            React.createElement("span", { className: react_styles_1.css(progress_1.default.progressMeasure) }, children))));
};
exports.ProgressBar = ProgressBar;
exports.ProgressBar.displayName = 'ProgressBar';
//# sourceMappingURL=ProgressBar.js.map