import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/InputGroup/input-group';
import { css } from '@patternfly/react-styles';
export var InputGroupTextVariant;
(function (InputGroupTextVariant) {
    InputGroupTextVariant["default"] = "default";
    InputGroupTextVariant["plain"] = "plain";
})(InputGroupTextVariant || (InputGroupTextVariant = {}));
export const InputGroupText = (_a) => {
    var { className = '', component = 'span', children, variant = InputGroupTextVariant.default } = _a, props = __rest(_a, ["className", "component", "children", "variant"]);
    const Component = component;
    return (React.createElement(Component, Object.assign({ className: css(styles.inputGroupText, variant === InputGroupTextVariant.plain && styles.modifiers.plain, className) }, props), children));
};
InputGroupText.displayName = 'InputGroupText';
//# sourceMappingURL=InputGroupText.js.map