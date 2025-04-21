"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ProgressStep = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const progress_stepper_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/ProgressStepper/progress-stepper"));
const react_styles_1 = require("@patternfly/react-styles");
const check_circle_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/check-circle-icon'));
const resources_full_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/resources-full-icon'));
const exclamation_triangle_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/exclamation-triangle-icon'));
const exclamation_circle_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/exclamation-circle-icon'));
const variantIcons = {
    default: undefined,
    pending: undefined,
    success: React.createElement(check_circle_icon_1.default, null),
    info: React.createElement(resources_full_icon_1.default, null),
    warning: React.createElement(exclamation_triangle_icon_1.default, null),
    danger: React.createElement(exclamation_circle_icon_1.default, null)
};
const variantStyle = {
    default: '',
    info: progress_stepper_1.default.modifiers.info,
    success: progress_stepper_1.default.modifiers.success,
    pending: progress_stepper_1.default.modifiers.pending,
    warning: progress_stepper_1.default.modifiers.warning,
    danger: progress_stepper_1.default.modifiers.danger
};
const ProgressStep = (_a) => {
    var { children, className, variant, isCurrent, description, icon, titleId, 'aria-label': ariaLabel, popoverRender } = _a, props = tslib_1.__rest(_a, ["children", "className", "variant", "isCurrent", "description", "icon", "titleId", 'aria-label', "popoverRender"]);
    const _icon = icon !== undefined ? icon : variantIcons[variant];
    const Component = popoverRender !== undefined ? 'button' : 'div';
    const stepRef = React.useRef();
    if (props.id === undefined || titleId === undefined) {
        /* eslint-disable no-console */
        console.warn('ProgressStep: The titleId and id properties are required to make this component accessible, and one or both of these properties are missing.');
    }
    return (React.createElement("li", Object.assign({ className: react_styles_1.css(progress_stepper_1.default.progressStepperStep, variantStyle[variant], isCurrent && progress_stepper_1.default.modifiers.current, className), "aria-label": ariaLabel }, (isCurrent && { 'aria-current': 'step' }), props),
        React.createElement("div", { className: react_styles_1.css(progress_stepper_1.default.progressStepperStepConnector) },
            React.createElement("span", { className: react_styles_1.css(progress_stepper_1.default.progressStepperStepIcon) }, _icon && _icon)),
        React.createElement("div", { className: react_styles_1.css(progress_stepper_1.default.progressStepperStepMain) },
            React.createElement(Component, Object.assign({ className: react_styles_1.css(progress_stepper_1.default.progressStepperStepTitle, popoverRender && progress_stepper_1.default.modifiers.helpText), id: titleId, ref: stepRef }, (popoverRender && { type: 'button' }), (props.id !== undefined && titleId !== undefined && { 'aria-labelledby': `${props.id} ${titleId}` })),
                children,
                popoverRender && popoverRender(stepRef)),
            description && React.createElement("div", { className: react_styles_1.css(progress_stepper_1.default.progressStepperStepDescription) }, description))));
};
exports.ProgressStep = ProgressStep;
exports.ProgressStep.displayName = 'ProgressStep';
//# sourceMappingURL=ProgressStep.js.map