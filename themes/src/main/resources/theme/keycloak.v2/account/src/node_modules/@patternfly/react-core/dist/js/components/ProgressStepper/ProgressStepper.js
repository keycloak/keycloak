"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ProgressStepper = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const progress_stepper_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/ProgressStepper/progress-stepper"));
const react_styles_1 = require("@patternfly/react-styles");
const ProgressStepper = (_a) => {
    var { children, className, isCenterAligned, isVertical, isCompact } = _a, props = tslib_1.__rest(_a, ["children", "className", "isCenterAligned", "isVertical", "isCompact"]);
    return (React.createElement("ol", Object.assign({ className: react_styles_1.css(progress_stepper_1.default.progressStepper, isCenterAligned && progress_stepper_1.default.modifiers.center, isVertical && progress_stepper_1.default.modifiers.vertical, isCompact && progress_stepper_1.default.modifiers.compact, className) }, props), children));
};
exports.ProgressStepper = ProgressStepper;
exports.ProgressStepper.displayName = 'ProgressStepper';
//# sourceMappingURL=ProgressStepper.js.map