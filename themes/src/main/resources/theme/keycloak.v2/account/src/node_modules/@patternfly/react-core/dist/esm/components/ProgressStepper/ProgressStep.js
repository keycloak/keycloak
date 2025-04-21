import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/ProgressStepper/progress-stepper';
import { css } from '@patternfly/react-styles';
import CheckCircleIcon from '@patternfly/react-icons/dist/esm/icons/check-circle-icon';
import ResourcesFullIcon from '@patternfly/react-icons/dist/esm/icons/resources-full-icon';
import ExclamationTriangleIcon from '@patternfly/react-icons/dist/esm/icons/exclamation-triangle-icon';
import ExclamationCircleIcon from '@patternfly/react-icons/dist/esm/icons/exclamation-circle-icon';
const variantIcons = {
    default: undefined,
    pending: undefined,
    success: React.createElement(CheckCircleIcon, null),
    info: React.createElement(ResourcesFullIcon, null),
    warning: React.createElement(ExclamationTriangleIcon, null),
    danger: React.createElement(ExclamationCircleIcon, null)
};
const variantStyle = {
    default: '',
    info: styles.modifiers.info,
    success: styles.modifiers.success,
    pending: styles.modifiers.pending,
    warning: styles.modifiers.warning,
    danger: styles.modifiers.danger
};
export const ProgressStep = (_a) => {
    var { children, className, variant, isCurrent, description, icon, titleId, 'aria-label': ariaLabel, popoverRender } = _a, props = __rest(_a, ["children", "className", "variant", "isCurrent", "description", "icon", "titleId", 'aria-label', "popoverRender"]);
    const _icon = icon !== undefined ? icon : variantIcons[variant];
    const Component = popoverRender !== undefined ? 'button' : 'div';
    const stepRef = React.useRef();
    if (props.id === undefined || titleId === undefined) {
        /* eslint-disable no-console */
        console.warn('ProgressStep: The titleId and id properties are required to make this component accessible, and one or both of these properties are missing.');
    }
    return (React.createElement("li", Object.assign({ className: css(styles.progressStepperStep, variantStyle[variant], isCurrent && styles.modifiers.current, className), "aria-label": ariaLabel }, (isCurrent && { 'aria-current': 'step' }), props),
        React.createElement("div", { className: css(styles.progressStepperStepConnector) },
            React.createElement("span", { className: css(styles.progressStepperStepIcon) }, _icon && _icon)),
        React.createElement("div", { className: css(styles.progressStepperStepMain) },
            React.createElement(Component, Object.assign({ className: css(styles.progressStepperStepTitle, popoverRender && styles.modifiers.helpText), id: titleId, ref: stepRef }, (popoverRender && { type: 'button' }), (props.id !== undefined && titleId !== undefined && { 'aria-labelledby': `${props.id} ${titleId}` })),
                children,
                popoverRender && popoverRender(stepRef)),
            description && React.createElement("div", { className: css(styles.progressStepperStepDescription) }, description))));
};
ProgressStep.displayName = 'ProgressStep';
//# sourceMappingURL=ProgressStep.js.map