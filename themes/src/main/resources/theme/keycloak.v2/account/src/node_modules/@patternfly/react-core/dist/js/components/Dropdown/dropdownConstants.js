"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.DropdownArrowContext = exports.DropdownContext = exports.DropdownDirection = exports.DropdownPosition = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
var DropdownPosition;
(function (DropdownPosition) {
    DropdownPosition["right"] = "right";
    DropdownPosition["left"] = "left";
})(DropdownPosition = exports.DropdownPosition || (exports.DropdownPosition = {}));
var DropdownDirection;
(function (DropdownDirection) {
    DropdownDirection["up"] = "up";
    DropdownDirection["down"] = "down";
})(DropdownDirection = exports.DropdownDirection || (exports.DropdownDirection = {}));
exports.DropdownContext = React.createContext({
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
exports.DropdownArrowContext = React.createContext({
    keyHandler: null,
    sendRef: null
});
//# sourceMappingURL=dropdownConstants.js.map