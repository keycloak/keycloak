"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.KebabToggle = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const ellipsis_v_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/ellipsis-v-icon'));
const Toggle_1 = require("./Toggle");
const KebabToggle = (_a) => {
    var { id = '', 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    children = null, className = '', isOpen = false, 'aria-label': ariaLabel = 'Actions', parentRef = null, getMenuRef = null, isActive = false, isPlain = false, isDisabled = false, bubbleEvent = false, onToggle = () => undefined, 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    ref } = _a, // Types of Ref are different for React.FunctionComponent vs React.Component
    props = tslib_1.__rest(_a, ["id", "children", "className", "isOpen", 'aria-label', "parentRef", "getMenuRef", "isActive", "isPlain", "isDisabled", "bubbleEvent", "onToggle", "ref"]);
    return (React.createElement(Toggle_1.Toggle, Object.assign({ id: id, className: className, isOpen: isOpen, "aria-label": ariaLabel, parentRef: parentRef, getMenuRef: getMenuRef, isActive: isActive, isPlain: isPlain, isDisabled: isDisabled, onToggle: onToggle, bubbleEvent: bubbleEvent }, props),
        React.createElement(ellipsis_v_icon_1.default, null)));
};
exports.KebabToggle = KebabToggle;
exports.KebabToggle.displayName = 'KebabToggle';
//# sourceMappingURL=KebabToggle.js.map