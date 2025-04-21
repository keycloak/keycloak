import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Radio/radio';
import { css } from '@patternfly/react-styles';
import { getOUIAProps, getDefaultOUIAId } from '../../helpers';
export class Radio extends React.Component {
    constructor(props) {
        super(props);
        this.handleChange = (event) => {
            this.props.onChange(event.currentTarget.checked, event);
        };
        if (!props.label && !props['aria-label']) {
            // eslint-disable-next-line no-console
            console.error('Radio:', 'Radio requires an aria-label to be specified');
        }
        this.state = {
            ouiaStateId: getDefaultOUIAId(Radio.displayName)
        };
    }
    render() {
        const _a = this.props, { 'aria-label': ariaLabel, checked, className, defaultChecked, isLabelWrapped, isLabelBeforeButton, isChecked, isDisabled, isValid, label, 
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        onChange, description, body, ouiaId, ouiaSafe = true } = _a, props = __rest(_a, ['aria-label', "checked", "className", "defaultChecked", "isLabelWrapped", "isLabelBeforeButton", "isChecked", "isDisabled", "isValid", "label", "onChange", "description", "body", "ouiaId", "ouiaSafe"]);
        if (!props.id) {
            // eslint-disable-next-line no-console
            console.error('Radio:', 'id is required to make input accessible');
        }
        const inputRendered = (React.createElement("input", Object.assign({}, props, { className: css(styles.radioInput), type: "radio", onChange: this.handleChange, "aria-invalid": !isValid, disabled: isDisabled, checked: checked || isChecked }, (checked === undefined && { defaultChecked }), (!label && { 'aria-label': ariaLabel }), getOUIAProps(Radio.displayName, ouiaId !== undefined ? ouiaId : this.state.ouiaStateId, ouiaSafe))));
        let labelRendered = null;
        if (label && isLabelWrapped) {
            labelRendered = React.createElement("span", { className: css(styles.radioLabel, isDisabled && styles.modifiers.disabled) }, label);
        }
        else if (label) {
            labelRendered = (React.createElement("label", { className: css(styles.radioLabel, isDisabled && styles.modifiers.disabled), htmlFor: props.id }, label));
        }
        const descRender = description ? React.createElement("span", { className: css(styles.radioDescription) }, description) : null;
        const bodyRender = body ? React.createElement("span", { className: css(styles.radioBody) }, body) : null;
        const childrenRendered = isLabelBeforeButton ? (React.createElement(React.Fragment, null,
            labelRendered,
            inputRendered,
            descRender,
            bodyRender)) : (React.createElement(React.Fragment, null,
            inputRendered,
            labelRendered,
            descRender,
            bodyRender));
        return isLabelWrapped ? (React.createElement("label", { className: css(styles.radio, className), htmlFor: props.id }, childrenRendered)) : (React.createElement("div", { className: css(styles.radio, !label && styles.modifiers.standalone, className) }, childrenRendered));
    }
}
Radio.displayName = 'Radio';
Radio.defaultProps = {
    className: '',
    isDisabled: false,
    isValid: true,
    onChange: () => { }
};
//# sourceMappingURL=Radio.js.map