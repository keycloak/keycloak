import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Progress/progress';
import { css } from '@patternfly/react-styles';
import { ProgressContainer, ProgressMeasureLocation } from './ProgressContainer';
import { getUniqueId } from '../../helpers/util';
export var ProgressSize;
(function (ProgressSize) {
    ProgressSize["sm"] = "sm";
    ProgressSize["md"] = "md";
    ProgressSize["lg"] = "lg";
})(ProgressSize || (ProgressSize = {}));
export class Progress extends React.Component {
    constructor() {
        super(...arguments);
        this.id = this.props.id || getUniqueId();
    }
    render() {
        const _a = this.props, { 
        /* eslint-disable @typescript-eslint/no-unused-vars */
        id, size, 
        /* eslint-enable @typescript-eslint/no-unused-vars */
        className, value, title, label, variant, measureLocation, min, max, valueText, isTitleTruncated, tooltipPosition, 'aria-label': ariaLabel, 'aria-labelledby': ariaLabelledBy } = _a, props = __rest(_a, ["id", "size", "className", "value", "title", "label", "variant", "measureLocation", "min", "max", "valueText", "isTitleTruncated", "tooltipPosition", 'aria-label', 'aria-labelledby']);
        const progressBarAriaProps = {
            'aria-valuemin': min,
            'aria-valuenow': value,
            'aria-valuemax': max
        };
        if (title || ariaLabelledBy) {
            progressBarAriaProps['aria-labelledby'] = title ? `${this.id}-description` : ariaLabelledBy;
        }
        if (ariaLabel) {
            progressBarAriaProps['aria-label'] = ariaLabel;
        }
        if (valueText) {
            progressBarAriaProps['aria-valuetext'] = valueText;
        }
        if (!title && !ariaLabelledBy && !ariaLabel) {
            /* eslint-disable no-console */
            console.warn('One of aria-label or aria-labelledby properties should be passed when using the progress component without a title.');
        }
        const scaledValue = Math.min(100, Math.max(0, Math.floor(((value - min) / (max - min)) * 100))) || 0;
        return (React.createElement("div", Object.assign({}, props, { className: css(styles.progress, styles.modifiers[variant], ['inside', 'outside'].includes(measureLocation) && styles.modifiers[measureLocation], measureLocation === 'inside' ? styles.modifiers[ProgressSize.lg] : styles.modifiers[size], !title && styles.modifiers.singleline, className), id: this.id }),
            React.createElement(ProgressContainer, { parentId: this.id, value: scaledValue, title: title, label: label, variant: variant, measureLocation: measureLocation, progressBarAriaProps: progressBarAriaProps, isTitleTruncated: isTitleTruncated, tooltipPosition: tooltipPosition })));
    }
}
Progress.displayName = 'Progress';
Progress.defaultProps = {
    className: '',
    measureLocation: ProgressMeasureLocation.top,
    variant: null,
    id: '',
    title: '',
    min: 0,
    max: 100,
    size: null,
    label: null,
    value: 0,
    valueText: null,
    isTitleTruncated: false,
    tooltipPosition: 'top',
    'aria-label': null,
    'aria-labelledby': null
};
//# sourceMappingURL=Progress.js.map