import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/InputGroup/input-group';
import { css } from '@patternfly/react-styles';
import { FormSelect } from '../FormSelect';
import { TextArea } from '../TextArea';
import { TextInput } from '../TextInput';
export const InputGroup = (_a) => {
    var { className = '', children, innerRef } = _a, props = __rest(_a, ["className", "children", "innerRef"]);
    const formCtrls = [FormSelect, TextArea, TextInput].map(comp => comp.displayName);
    const idItem = React.Children.toArray(children).find((child) => !formCtrls.includes(child.type.displayName) && child.props.id);
    const inputGroupRef = innerRef || React.useRef(null);
    return (React.createElement("div", Object.assign({ ref: inputGroupRef, className: css(styles.inputGroup, className) }, props), idItem
        ? React.Children.map(children, (child) => formCtrls.includes(child.type.displayName)
            ? React.cloneElement(child, { 'aria-describedby': idItem.props.id })
            : child)
        : children));
};
InputGroup.displayName = 'InputGroup';
//# sourceMappingURL=InputGroup.js.map