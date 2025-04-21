"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.HelperText = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const helper_text_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/HelperText/helper-text"));
const react_styles_1 = require("@patternfly/react-styles");
const HelperText = (_a) => {
    var { children, className, component = 'div', id, isLiveRegion = false } = _a, props = tslib_1.__rest(_a, ["children", "className", "component", "id", "isLiveRegion"]);
    const Component = component;
    return (React.createElement(Component, Object.assign({ id: id, className: react_styles_1.css(helper_text_1.default.helperText, className) }, (isLiveRegion && { 'aria-live': 'polite' }), props), children));
};
exports.HelperText = HelperText;
exports.HelperText.displayName = 'HelperText';
//# sourceMappingURL=HelperText.js.map