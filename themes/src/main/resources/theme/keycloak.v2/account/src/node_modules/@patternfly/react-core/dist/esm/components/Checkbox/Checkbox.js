import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Check/check';
import { css } from '@patternfly/react-styles';
import { getDefaultOUIAId, getOUIAProps } from '../../helpers';
// tslint:disable-next-line:no-empty
const defaultOnChange = () => { };
export class Checkbox extends React.Component {
    constructor(props) {
        super(props);
        this.handleChange = (event) => {
            this.props.onChange(event.currentTarget.checked, event);
        };
        this.state = {
            ouiaStateId: getDefaultOUIAId(Checkbox.displayName)
        };
    }
    render() {
        const _a = this.props, { 'aria-label': ariaLabel, className, onChange, isValid, isDisabled, isChecked, label, checked, defaultChecked, description, body, ouiaId, ouiaSafe, component: Component } = _a, props = __rest(_a, ['aria-label', "className", "onChange", "isValid", "isDisabled", "isChecked", "label", "checked", "defaultChecked", "description", "body", "ouiaId", "ouiaSafe", "component"]);
        if (!props.id) {
            // eslint-disable-next-line no-console
            console.error('Checkbox:', 'id is required to make input accessible');
        }
        const checkedProps = {};
        if ([true, false].includes(checked) || isChecked === true) {
            checkedProps.checked = checked || isChecked;
        }
        if (onChange !== defaultOnChange) {
            checkedProps.checked = isChecked;
        }
        if ([false, true].includes(defaultChecked)) {
            checkedProps.defaultChecked = defaultChecked;
        }
        checkedProps.checked = checkedProps.checked === null ? false : checkedProps.checked;
        return (React.createElement(Component, { className: css(styles.check, !label && styles.modifiers.standalone, className) },
            React.createElement("input", Object.assign({}, props, { className: css(styles.checkInput), type: "checkbox", onChange: this.handleChange, "aria-invalid": !isValid, "aria-label": ariaLabel, disabled: isDisabled, ref: elem => elem && (elem.indeterminate = isChecked === null) }, checkedProps, getOUIAProps(Checkbox.displayName, ouiaId !== undefined ? ouiaId : this.state.ouiaStateId, ouiaSafe))),
            label && (React.createElement("label", { className: css(styles.checkLabel, isDisabled && styles.modifiers.disabled), htmlFor: props.id }, label)),
            description && React.createElement("span", { className: css(styles.checkDescription) }, description),
            body && React.createElement("span", { className: css(styles.checkBody) }, body)));
    }
}
Checkbox.displayName = 'Checkbox';
Checkbox.defaultProps = {
    className: '',
    isValid: true,
    isDisabled: false,
    isChecked: false,
    onChange: defaultOnChange,
    ouiaSafe: true,
    component: 'div'
};
//# sourceMappingURL=Checkbox.js.map