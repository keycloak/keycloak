import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Switch/switch';
import { css } from '@patternfly/react-styles';
import CheckIcon from '@patternfly/react-icons/dist/esm/icons/check-icon';
import { getUniqueId } from '../../helpers/util';
import { getOUIAProps, getDefaultOUIAId } from '../../helpers';
export class Switch extends React.Component {
    constructor(props) {
        super(props);
        if (!props.label && !props['aria-label']) {
            // eslint-disable-next-line no-console
            console.error('Switch: Switch requires either a label or an aria-label to be specified');
        }
        this.id = props.id || getUniqueId();
        this.state = {
            ouiaStateId: getDefaultOUIAId(Switch.displayName)
        };
    }
    render() {
        const _a = this.props, { 
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        id, className, label, labelOff, isChecked, defaultChecked, hasCheckIcon, isDisabled, onChange, isReversed, ouiaId, ouiaSafe } = _a, props = __rest(_a, ["id", "className", "label", "labelOff", "isChecked", "defaultChecked", "hasCheckIcon", "isDisabled", "onChange", "isReversed", "ouiaId", "ouiaSafe"]);
        const isAriaLabelledBy = props['aria-label'] === '';
        return (React.createElement("label", Object.assign({ className: css(styles.switch, isReversed && styles.modifiers.reverse, className), htmlFor: this.id }, getOUIAProps(Switch.displayName, ouiaId !== undefined ? ouiaId : this.state.ouiaStateId, ouiaSafe)),
            React.createElement("input", Object.assign({ id: this.id, className: css(styles.switchInput), type: "checkbox", onChange: event => onChange(event.target.checked, event) }, ([true, false].includes(defaultChecked) && { defaultChecked }), (![true, false].includes(defaultChecked) && { checked: isChecked }), { disabled: isDisabled, "aria-labelledby": isAriaLabelledBy ? `${this.id}-on` : null }, props)),
            label !== undefined ? (React.createElement(React.Fragment, null,
                React.createElement("span", { className: css(styles.switchToggle) }, hasCheckIcon && (React.createElement("span", { className: css(styles.switchToggleIcon), "aria-hidden": "true" },
                    React.createElement(CheckIcon, { noVerticalAlign: true })))),
                React.createElement("span", { className: css(styles.switchLabel, styles.modifiers.on), id: isAriaLabelledBy ? `${this.id}-on` : null, "aria-hidden": "true" }, label),
                React.createElement("span", { className: css(styles.switchLabel, styles.modifiers.off), id: isAriaLabelledBy ? `${this.id}-off` : null, "aria-hidden": "true" }, labelOff !== undefined ? labelOff : label))) : (React.createElement("span", { className: css(styles.switchToggle) },
                React.createElement("div", { className: css(styles.switchToggleIcon), "aria-hidden": "true" },
                    React.createElement(CheckIcon, { noVerticalAlign: true }))))));
    }
}
Switch.displayName = 'Switch';
Switch.defaultProps = {
    isChecked: true,
    isDisabled: false,
    isReversed: false,
    'aria-label': '',
    onChange: () => undefined
};
//# sourceMappingURL=Switch.js.map