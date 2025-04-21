"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.BadgeToggle = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const dropdown_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Dropdown/dropdown"));
const caret_down_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/caret-down-icon'));
const Toggle_1 = require("./Toggle");
const Badge_1 = require("../Badge");
const BadgeToggle = (_a) => {
    var { id = '', children = null, badgeProps = { isRead: true }, className = '', isOpen = false, 'aria-label': ariaLabel = 'Actions', parentRef = null, getMenuRef = null, isActive = false, isPlain = null, isDisabled = false, bubbleEvent = false, onToggle = () => undefined, 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    ref } = _a, // Types of Ref are different for React.FunctionComponent vs React.Component
    props = tslib_1.__rest(_a, ["id", "children", "badgeProps", "className", "isOpen", 'aria-label', "parentRef", "getMenuRef", "isActive", "isPlain", "isDisabled", "bubbleEvent", "onToggle", "ref"]);
    return (React.createElement(Toggle_1.Toggle, Object.assign({ id: id, className: className, isOpen: isOpen, "aria-label": ariaLabel, parentRef: parentRef, getMenuRef: getMenuRef, isActive: isActive, isPlain: isPlain || true, isDisabled: isDisabled, onToggle: onToggle, bubbleEvent: bubbleEvent }, props),
        React.createElement(Badge_1.Badge, Object.assign({}, badgeProps),
            children,
            React.createElement("span", { className: react_styles_1.css(dropdown_1.default.dropdownToggleIcon) },
                React.createElement(caret_down_icon_1.default, null)))));
};
exports.BadgeToggle = BadgeToggle;
exports.BadgeToggle.displayName = 'BadgeToggle';
//# sourceMappingURL=BadgeToggle.js.map