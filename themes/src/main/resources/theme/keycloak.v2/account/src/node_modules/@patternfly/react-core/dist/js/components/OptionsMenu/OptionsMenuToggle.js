"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.OptionsMenuToggle = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const Dropdown_1 = require("../Dropdown");
const OptionsMenuToggle = (_a) => {
    var { isPlain = false, isDisabled = false, isOpen = false, parentId = '', toggleTemplate = React.createElement(React.Fragment, null), hideCaret = false, 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    isActive = false, 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    isSplitButton = false, 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    type, 'aria-label': ariaLabel = 'Options menu' } = _a, props = tslib_1.__rest(_a, ["isPlain", "isDisabled", "isOpen", "parentId", "toggleTemplate", "hideCaret", "isActive", "isSplitButton", "type", 'aria-label']);
    return (React.createElement(Dropdown_1.DropdownContext.Consumer, null, ({ id: contextId }) => (React.createElement(Dropdown_1.DropdownToggle, Object.assign({}, ((isPlain || hideCaret) && { toggleIndicator: null }), props, { isPlain: isPlain, isOpen: isOpen, isDisabled: isDisabled, isActive: isActive, id: parentId ? `${parentId}-toggle` : `${contextId}-toggle`, "aria-haspopup": "listbox", "aria-label": ariaLabel, "aria-expanded": isOpen }, (toggleTemplate ? { children: toggleTemplate } : {}))))));
};
exports.OptionsMenuToggle = OptionsMenuToggle;
exports.OptionsMenuToggle.displayName = 'OptionsMenuToggle';
//# sourceMappingURL=OptionsMenuToggle.js.map