import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Spinner/spinner';
import { css } from '@patternfly/react-styles';
export var spinnerSize;
(function (spinnerSize) {
    spinnerSize["sm"] = "sm";
    spinnerSize["md"] = "md";
    spinnerSize["lg"] = "lg";
    spinnerSize["xl"] = "xl";
})(spinnerSize || (spinnerSize = {}));
export const Spinner = (_a) => {
    var { 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    className = '', size = 'xl', 'aria-valuetext': ariaValueText = 'Loading...', isSVG = false, diameter, 'aria-label': ariaLabel, 'aria-labelledBy': ariaLabelledBy } = _a, props = __rest(_a, ["className", "size", 'aria-valuetext', "isSVG", "diameter", 'aria-label', 'aria-labelledBy']);
    const Component = isSVG ? 'svg' : 'span';
    return (React.createElement(Component, Object.assign({ className: css(styles.spinner, styles.modifiers[size], className), role: "progressbar", "aria-valuetext": ariaValueText }, (isSVG && { viewBox: '0 0 100 100' }), (diameter && { style: { '--pf-c-spinner--diameter': diameter } }), (ariaLabel && { 'aria-label': ariaLabel }), (ariaLabelledBy && { 'aria-labelledBy': ariaLabelledBy }), (!ariaLabel && !ariaLabelledBy && { 'aria-label': 'Contents' }), props), isSVG ? (React.createElement("circle", { className: styles.spinnerPath, cx: "50", cy: "50", r: "45", fill: "none" })) : (React.createElement(React.Fragment, null,
        React.createElement("span", { className: css(styles.spinnerClipper) }),
        React.createElement("span", { className: css(styles.spinnerLeadBall) }),
        React.createElement("span", { className: css(styles.spinnerTailBall) })))));
};
Spinner.displayName = 'Spinner';
//# sourceMappingURL=Spinner.js.map