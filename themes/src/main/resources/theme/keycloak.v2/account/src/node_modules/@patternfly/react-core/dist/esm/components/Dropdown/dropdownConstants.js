import * as React from 'react';
export var DropdownPosition;
(function (DropdownPosition) {
    DropdownPosition["right"] = "right";
    DropdownPosition["left"] = "left";
})(DropdownPosition || (DropdownPosition = {}));
export var DropdownDirection;
(function (DropdownDirection) {
    DropdownDirection["up"] = "up";
    DropdownDirection["down"] = "down";
})(DropdownDirection || (DropdownDirection = {}));
export const DropdownContext = React.createContext({
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    onSelect: (event) => undefined,
    id: '',
    toggleIndicatorClass: '',
    toggleIconClass: '',
    toggleTextClass: '',
    menuClass: '',
    itemClass: '',
    toggleClass: '',
    baseClass: '',
    baseComponent: 'div',
    sectionClass: '',
    sectionTitleClass: '',
    sectionComponent: 'section',
    disabledClass: '',
    plainTextClass: '',
    menuComponent: 'ul'
});
export const DropdownArrowContext = React.createContext({
    keyHandler: null,
    sendRef: null
});
//# sourceMappingURL=dropdownConstants.js.map