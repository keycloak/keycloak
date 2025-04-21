import { __rest } from "tslib";
import * as React from 'react';
import { DropdownToggle, DropdownContext } from '../Dropdown';
export const OptionsMenuToggle = (_a) => {
    var { isPlain = false, isDisabled = false, isOpen = false, parentId = '', toggleTemplate = React.createElement(React.Fragment, null), hideCaret = false, 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    isActive = false, 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    isSplitButton = false, 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    type, 'aria-label': ariaLabel = 'Options menu' } = _a, props = __rest(_a, ["isPlain", "isDisabled", "isOpen", "parentId", "toggleTemplate", "hideCaret", "isActive", "isSplitButton", "type", 'aria-label']);
    return (React.createElement(DropdownContext.Consumer, null, ({ id: contextId }) => (React.createElement(DropdownToggle, Object.assign({}, ((isPlain || hideCaret) && { toggleIndicator: null }), props, { isPlain: isPlain, isOpen: isOpen, isDisabled: isDisabled, isActive: isActive, id: parentId ? `${parentId}-toggle` : `${contextId}-toggle`, "aria-haspopup": "listbox", "aria-label": ariaLabel, "aria-expanded": isOpen }, (toggleTemplate ? { children: toggleTemplate } : {}))))));
};
OptionsMenuToggle.displayName = 'OptionsMenuToggle';
//# sourceMappingURL=OptionsMenuToggle.js.map