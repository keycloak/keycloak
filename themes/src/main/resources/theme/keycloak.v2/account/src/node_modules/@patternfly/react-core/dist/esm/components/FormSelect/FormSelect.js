import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/FormControl/form-control';
import { css } from '@patternfly/react-styles';
import { ValidatedOptions } from '../../helpers/constants';
import { getOUIAProps, getDefaultOUIAId } from '../../helpers';
export class FormSelect extends React.Component {
    constructor(props) {
        super(props);
        this.handleChange = (event) => {
            this.props.onChange(event.currentTarget.value, event);
        };
        if (!props.id && !props['aria-label']) {
            // eslint-disable-next-line no-console
            console.error('FormSelect requires either an id or aria-label to be specified');
        }
        this.state = {
            ouiaStateId: getDefaultOUIAId(FormSelect.displayName, props.validated)
        };
    }
    render() {
        const _a = this.props, { children, className, value, validated, isDisabled, isRequired, isIconSprite, ouiaId, ouiaSafe } = _a, props = __rest(_a, ["children", "className", "value", "validated", "isDisabled", "isRequired", "isIconSprite", "ouiaId", "ouiaSafe"]);
        /* find selected option and get placeholder flag */
        const selectedOption = React.Children.toArray(children).find((option) => option.props.value === value);
        const isSelectedPlaceholder = selectedOption && selectedOption.props.isPlaceholder;
        return (React.createElement("select", Object.assign({}, props, { className: css(styles.formControl, isIconSprite && styles.modifiers.iconSprite, className, validated === ValidatedOptions.success && styles.modifiers.success, validated === ValidatedOptions.warning && styles.modifiers.warning, isSelectedPlaceholder && styles.modifiers.placeholder), "aria-invalid": validated === ValidatedOptions.error }, getOUIAProps(FormSelect.displayName, ouiaId !== undefined ? ouiaId : this.state.ouiaStateId, ouiaSafe), { onChange: this.handleChange, disabled: isDisabled, required: isRequired, value: value }), children));
    }
}
FormSelect.displayName = 'FormSelect';
FormSelect.defaultProps = {
    className: '',
    value: '',
    validated: 'default',
    isDisabled: false,
    isRequired: false,
    isIconSprite: false,
    onBlur: () => undefined,
    onFocus: () => undefined,
    onChange: () => undefined,
    ouiaSafe: true
};
//# sourceMappingURL=FormSelect.js.map