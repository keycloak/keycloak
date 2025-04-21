import { __rest } from "tslib";
import * as React from 'react';
import EllipsisVIcon from '@patternfly/react-icons/dist/esm/icons/ellipsis-v-icon';
import { Toggle } from './Toggle';
export const KebabToggle = (_a) => {
    var { id = '', 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    children = null, className = '', isOpen = false, 'aria-label': ariaLabel = 'Actions', parentRef = null, getMenuRef = null, isActive = false, isPlain = false, isDisabled = false, bubbleEvent = false, onToggle = () => undefined, 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    ref } = _a, // Types of Ref are different for React.FunctionComponent vs React.Component
    props = __rest(_a, ["id", "children", "className", "isOpen", 'aria-label', "parentRef", "getMenuRef", "isActive", "isPlain", "isDisabled", "bubbleEvent", "onToggle", "ref"]);
    return (React.createElement(Toggle, Object.assign({ id: id, className: className, isOpen: isOpen, "aria-label": ariaLabel, parentRef: parentRef, getMenuRef: getMenuRef, isActive: isActive, isPlain: isPlain, isDisabled: isDisabled, onToggle: onToggle, bubbleEvent: bubbleEvent }, props),
        React.createElement(EllipsisVIcon, null)));
};
KebabToggle.displayName = 'KebabToggle';
//# sourceMappingURL=KebabToggle.js.map