import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Form/form';
import { css } from '@patternfly/react-styles';
import { FormFieldGroupToggle } from './FormFieldGroupToggle';
import { GenerateId } from '../../helpers';
export const InternalFormFieldGroup = (_a) => {
    var { children, className, header, isExpandable, isExpanded, onToggle, toggleAriaLabel } = _a, props = __rest(_a, ["children", "className", "header", "isExpandable", "isExpanded", "onToggle", "toggleAriaLabel"]);
    const headerTitleText = header ? header.props.titleText : null;
    if (isExpandable && !toggleAriaLabel && !headerTitleText) {
        // eslint-disable-next-line no-console
        console.error('FormFieldGroupExpandable:', 'toggleAriaLabel or the titleText prop of FormFieldGroupHeader is required to make the toggle button accessible');
    }
    return (React.createElement("div", Object.assign({ className: css(styles.formFieldGroup, isExpanded && isExpandable && styles.modifiers.expanded, className), role: "group" }, (headerTitleText && { 'aria-labelledby': `${header.props.titleText.id}` }), props),
        isExpandable && (React.createElement(GenerateId, { prefix: "form-field-group-toggle" }, id => (React.createElement(FormFieldGroupToggle, Object.assign({ onToggle: onToggle, isExpanded: isExpanded, "aria-label": toggleAriaLabel, toggleId: id }, (headerTitleText && { 'aria-labelledby': `${header.props.titleText.id} ${id}` })))))),
        header && header,
        (!isExpandable || (isExpandable && isExpanded)) && (React.createElement("div", { className: css(styles.formFieldGroupBody) }, children))));
};
InternalFormFieldGroup.displayName = 'InternalFormFieldGroup';
//# sourceMappingURL=InternalFormFieldGroup.js.map