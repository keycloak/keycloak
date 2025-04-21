"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.SliderStep = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const slider_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Slider/slider"));
const react_styles_1 = require("@patternfly/react-styles");
const SliderStep = (_a) => {
    var { className, label, value, isTickHidden = false, isLabelHidden = false, isActive = false } = _a, props = tslib_1.__rest(_a, ["className", "label", "value", "isTickHidden", "isLabelHidden", "isActive"]);
    const style = { '--pf-c-slider__step--Left': `${value}%` };
    return (React.createElement("div", Object.assign({ className: react_styles_1.css(slider_1.default.sliderStep, isActive && slider_1.default.modifiers.active, className), style: style }, props),
        !isTickHidden && React.createElement("div", { className: react_styles_1.css(slider_1.default.sliderStepTick) }),
        !isLabelHidden && label && React.createElement("div", { className: react_styles_1.css(slider_1.default.sliderStepLabel) }, label)));
};
exports.SliderStep = SliderStep;
exports.SliderStep.displayName = 'SliderStep';
//# sourceMappingURL=SliderStep.js.map