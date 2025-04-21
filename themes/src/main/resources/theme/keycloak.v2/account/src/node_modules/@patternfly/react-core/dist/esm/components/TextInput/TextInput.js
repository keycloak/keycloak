import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/FormControl/form-control';
import { css } from '@patternfly/react-styles';
import { ValidatedOptions } from '../../helpers/constants';
import { trimLeft } from '../../helpers/util';
import { getDefaultOUIAId, getOUIAProps } from '../../helpers';
import { getResizeObserver } from '../../helpers/resizeObserver';
export var TextInputTypes;
(function (TextInputTypes) {
    TextInputTypes["text"] = "text";
    TextInputTypes["date"] = "date";
    TextInputTypes["datetimeLocal"] = "datetime-local";
    TextInputTypes["email"] = "email";
    TextInputTypes["month"] = "month";
    TextInputTypes["number"] = "number";
    TextInputTypes["password"] = "password";
    TextInputTypes["search"] = "search";
    TextInputTypes["tel"] = "tel";
    TextInputTypes["time"] = "time";
    TextInputTypes["url"] = "url";
})(TextInputTypes || (TextInputTypes = {}));
export class TextInputBase extends React.Component {
    constructor(props) {
        super(props);
        this.inputRef = React.createRef();
        this.observer = () => { };
        this.handleChange = (event) => {
            if (this.props.onChange) {
                this.props.onChange(event.currentTarget.value, event);
            }
        };
        this.handleResize = () => {
            const inputRef = this.props.innerRef || this.inputRef;
            if (inputRef && inputRef.current) {
                trimLeft(inputRef.current, String(this.props.value));
            }
        };
        this.restoreText = () => {
            const inputRef = this.props.innerRef || this.inputRef;
            // restore the value
            inputRef.current.value = String(this.props.value);
            // make sure we still see the rightmost value to preserve cursor click position
            inputRef.current.scrollLeft = inputRef.current.scrollWidth;
        };
        this.onFocus = (event) => {
            const { isLeftTruncated, onFocus } = this.props;
            if (isLeftTruncated) {
                this.restoreText();
            }
            onFocus && onFocus(event);
        };
        this.onBlur = (event) => {
            const { isLeftTruncated, onBlur } = this.props;
            if (isLeftTruncated) {
                this.handleResize();
            }
            onBlur && onBlur(event);
        };
        this.sanitizeInputValue = (value) => typeof value === 'string' ? value.replace(/\n/g, ' ') : value;
        if (!props.id && !props['aria-label'] && !props['aria-labelledby']) {
            // eslint-disable-next-line no-console
            console.error('Text input:', 'Text input requires either an id or aria-label to be specified');
        }
        this.state = {
            ouiaStateId: getDefaultOUIAId(TextInputBase.displayName)
        };
    }
    componentDidMount() {
        if (this.props.isLeftTruncated) {
            const inputRef = this.props.innerRef || this.inputRef;
            this.observer = getResizeObserver(inputRef.current, this.handleResize);
            this.handleResize();
        }
    }
    componentWillUnmount() {
        if (this.props.isLeftTruncated) {
            this.observer();
        }
    }
    render() {
        const _a = this.props, { innerRef, className, type, value, validated, 
        /* eslint-disable @typescript-eslint/no-unused-vars */
        onChange, onFocus, onBlur, isLeftTruncated, 
        /* eslint-enable @typescript-eslint/no-unused-vars */
        isReadOnly, isRequired, isDisabled, isIconSprite, iconVariant, customIconUrl, customIconDimensions, ouiaId, ouiaSafe } = _a, props = __rest(_a, ["innerRef", "className", "type", "value", "validated", "onChange", "onFocus", "onBlur", "isLeftTruncated", "isReadOnly", "isRequired", "isDisabled", "isIconSprite", "iconVariant", "customIconUrl", "customIconDimensions", "ouiaId", "ouiaSafe"]);
        const customIconStyle = {};
        if (customIconUrl) {
            customIconStyle.backgroundImage = `url('${customIconUrl}')`;
        }
        if (customIconDimensions) {
            customIconStyle.backgroundSize = customIconDimensions;
        }
        return (React.createElement("input", Object.assign({}, props, { onFocus: this.onFocus, onBlur: this.onBlur, className: css(styles.formControl, isIconSprite && styles.modifiers.iconSprite, validated === ValidatedOptions.success && styles.modifiers.success, validated === ValidatedOptions.warning && styles.modifiers.warning, ((iconVariant && iconVariant !== 'search') || customIconUrl) && styles.modifiers.icon, iconVariant && styles.modifiers[iconVariant], className), onChange: this.handleChange, type: type, value: this.sanitizeInputValue(value), "aria-invalid": props['aria-invalid'] ? props['aria-invalid'] : validated === ValidatedOptions.error, required: isRequired, disabled: isDisabled, readOnly: isReadOnly, ref: innerRef || this.inputRef }, ((customIconUrl || customIconDimensions) && { style: customIconStyle }), getOUIAProps(TextInput.displayName, ouiaId !== undefined ? ouiaId : this.state.ouiaStateId, ouiaSafe))));
    }
}
TextInputBase.displayName = 'TextInputBase';
TextInputBase.defaultProps = {
    'aria-label': null,
    className: '',
    isRequired: false,
    validated: 'default',
    isDisabled: false,
    isReadOnly: false,
    isIconSprite: false,
    type: TextInputTypes.text,
    isLeftTruncated: false,
    onChange: () => undefined,
    ouiaSafe: true
};
export const TextInput = React.forwardRef((props, ref) => (React.createElement(TextInputBase, Object.assign({}, props, { innerRef: ref }))));
TextInput.displayName = 'TextInput';
//# sourceMappingURL=TextInput.js.map