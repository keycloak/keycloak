import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Dropdown/dropdown';
import CaretDownIcon from '@patternfly/react-icons/dist/esm/icons/caret-down-icon';
import { Toggle } from './Toggle';
import { Badge } from '../Badge';
export const BadgeToggle = (_a) => {
    var { id = '', children = null, badgeProps = { isRead: true }, className = '', isOpen = false, 'aria-label': ariaLabel = 'Actions', parentRef = null, getMenuRef = null, isActive = false, isPlain = null, isDisabled = false, bubbleEvent = false, onToggle = () => undefined, 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    ref } = _a, // Types of Ref are different for React.FunctionComponent vs React.Component
    props = __rest(_a, ["id", "children", "badgeProps", "className", "isOpen", 'aria-label', "parentRef", "getMenuRef", "isActive", "isPlain", "isDisabled", "bubbleEvent", "onToggle", "ref"]);
    return (React.createElement(Toggle, Object.assign({ id: id, className: className, isOpen: isOpen, "aria-label": ariaLabel, parentRef: parentRef, getMenuRef: getMenuRef, isActive: isActive, isPlain: isPlain || true, isDisabled: isDisabled, onToggle: onToggle, bubbleEvent: bubbleEvent }, props),
        React.createElement(Badge, Object.assign({}, badgeProps),
            children,
            React.createElement("span", { className: css(styles.dropdownToggleIcon) },
                React.createElement(CaretDownIcon, null)))));
};
BadgeToggle.displayName = 'BadgeToggle';
//# sourceMappingURL=BadgeToggle.js.map