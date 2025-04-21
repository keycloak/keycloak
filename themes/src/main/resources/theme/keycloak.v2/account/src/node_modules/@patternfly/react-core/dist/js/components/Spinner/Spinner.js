"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Spinner = exports.spinnerSize = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const spinner_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Spinner/spinner"));
const react_styles_1 = require("@patternfly/react-styles");
var spinnerSize;
(function (spinnerSize) {
    spinnerSize["sm"] = "sm";
    spinnerSize["md"] = "md";
    spinnerSize["lg"] = "lg";
    spinnerSize["xl"] = "xl";
})(spinnerSize = exports.spinnerSize || (exports.spinnerSize = {}));
const Spinner = (_a) => {
    var { 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    className = '', size = 'xl', 'aria-valuetext': ariaValueText = 'Loading...', isSVG = false, diameter, 'aria-label': ariaLabel, 'aria-labelledBy': ariaLabelledBy } = _a, props = tslib_1.__rest(_a, ["className", "size", 'aria-valuetext', "isSVG", "diameter", 'aria-label', 'aria-labelledBy']);
    const Component = isSVG ? 'svg' : 'span';
    return (React.createElement(Component, Object.assign({ className: react_styles_1.css(spinner_1.default.spinner, spinner_1.default.modifiers[size], className), role: "progressbar", "aria-valuetext": ariaValueText }, (isSVG && { viewBox: '0 0 100 100' }), (diameter && { style: { '--pf-c-spinner--diameter': diameter } }), (ariaLabel && { 'aria-label': ariaLabel }), (ariaLabelledBy && { 'aria-labelledBy': ariaLabelledBy }), (!ariaLabel && !ariaLabelledBy && { 'aria-label': 'Contents' }), props), isSVG ? (React.createElement("circle", { className: spinner_1.default.spinnerPath, cx: "50", cy: "50", r: "45", fill: "none" })) : (React.createElement(React.Fragment, null,
        React.createElement("span", { className: react_styles_1.css(spinner_1.default.spinnerClipper) }),
        React.createElement("span", { className: react_styles_1.css(spinner_1.default.spinnerLeadBall) }),
        React.createElement("span", { className: react_styles_1.css(spinner_1.default.spinnerTailBall) })))));
};
exports.Spinner = Spinner;
exports.Spinner.displayName = 'Spinner';
//# sourceMappingURL=Spinner.js.map