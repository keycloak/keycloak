"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.MenuToggle = exports.MenuToggleBase = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const menu_toggle_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/MenuToggle/menu-toggle"));
const react_styles_1 = require("@patternfly/react-styles");
const caret_down_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/caret-down-icon'));
class MenuToggleBase extends React.Component {
    constructor() {
        super(...arguments);
        this.displayName = 'MenuToggleBase';
    }
    render() {
        const _a = this.props, { children, className, icon, badge, isExpanded, isDisabled, isFullHeight, isFullWidth, variant, innerRef } = _a, props = tslib_1.__rest(_a, ["children", "className", "icon", "badge", "isExpanded", "isDisabled", "isFullHeight", "isFullWidth", "variant", "innerRef"]);
        const isPlain = variant === 'plain';
        const isPlainText = variant === 'plainText';
        const content = (React.createElement(React.Fragment, null,
            icon && React.createElement("span", { className: react_styles_1.css(menu_toggle_1.default.menuToggleIcon) }, icon),
            React.createElement("span", { className: "pf-c-menu-toggle__text" }, children),
            badge && React.createElement("span", { className: react_styles_1.css(menu_toggle_1.default.menuToggleCount) }, badge),
            React.createElement("span", { className: react_styles_1.css(menu_toggle_1.default.menuToggleControls) },
                React.createElement("span", { className: react_styles_1.css(menu_toggle_1.default.menuToggleToggleIcon) },
                    React.createElement(caret_down_icon_1.default, { "aria-hidden": true })))));
        return (React.createElement("button", Object.assign({ className: react_styles_1.css(menu_toggle_1.default.menuToggle, isExpanded && menu_toggle_1.default.modifiers.expanded, variant === 'primary' && menu_toggle_1.default.modifiers.primary, variant === 'secondary' && menu_toggle_1.default.modifiers.secondary, (isPlain || isPlainText) && menu_toggle_1.default.modifiers.plain, isPlainText && menu_toggle_1.default.modifiers.text, isFullHeight && menu_toggle_1.default.modifiers.fullHeight, isFullWidth && menu_toggle_1.default.modifiers.fullWidth, className), type: "button", "aria-expanded": false, ref: innerRef }, (isExpanded && { 'aria-expanded': true }), (isDisabled && { disabled: true }), props),
            isPlain && children,
            !isPlain && content));
    }
}
exports.MenuToggleBase = MenuToggleBase;
MenuToggleBase.defaultProps = {
    className: '',
    isExpanded: false,
    isDisabled: false,
    isFullWidth: false,
    isFullHeight: false,
    variant: 'default'
};
exports.MenuToggle = React.forwardRef((props, ref) => (React.createElement(MenuToggleBase, Object.assign({ innerRef: ref }, props))));
exports.MenuToggle.displayName = 'MenuToggle';
//# sourceMappingURL=MenuToggle.js.map