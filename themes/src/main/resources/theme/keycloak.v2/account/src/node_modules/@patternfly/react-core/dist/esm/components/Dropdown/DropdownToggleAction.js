import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Dropdown/dropdown';
import { css } from '@patternfly/react-styles';
export class DropdownToggleAction extends React.Component {
    render() {
        const _a = this.props, { id, className, onClick, isDisabled, children } = _a, props = __rest(_a, ["id", "className", "onClick", "isDisabled", "children"]);
        return (React.createElement("button", Object.assign({ id: id, className: css(styles.dropdownToggleButton, className), onClick: onClick }, (isDisabled && { disabled: true, 'aria-disabled': true }), props), children));
    }
}
DropdownToggleAction.displayName = 'DropdownToggleAction';
DropdownToggleAction.defaultProps = {
    className: '',
    isDisabled: false,
    onClick: () => { }
};
//# sourceMappingURL=DropdownToggleAction.js.map