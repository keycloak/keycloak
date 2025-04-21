"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.NotificationDrawerListItemHeader = exports.variantIcons = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const bell_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/bell-icon'));
const check_circle_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/check-circle-icon'));
const exclamation_circle_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/exclamation-circle-icon'));
const exclamation_triangle_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/exclamation-triangle-icon'));
const info_circle_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/info-circle-icon'));
const react_styles_1 = require("@patternfly/react-styles");
const notification_drawer_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/NotificationDrawer/notification-drawer"));
const accessibility_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/utilities/Accessibility/accessibility"));
const c_notification_drawer__list_item_header_title_max_lines_1 = tslib_1.__importDefault(require('@patternfly/react-tokens/dist/js/c_notification_drawer__list_item_header_title_max_lines'));
const Tooltip_1 = require("../Tooltip");
exports.variantIcons = {
    success: check_circle_icon_1.default,
    danger: exclamation_circle_icon_1.default,
    warning: exclamation_triangle_icon_1.default,
    info: info_circle_icon_1.default,
    default: bell_icon_1.default
};
const NotificationDrawerListItemHeader = (_a) => {
    var { children, className = '', icon = null, srTitle, title, variant = 'default', truncateTitle = 0, tooltipPosition } = _a, props = tslib_1.__rest(_a, ["children", "className", "icon", "srTitle", "title", "variant", "truncateTitle", "tooltipPosition"]);
    const titleRef = React.useRef(null);
    const [isTooltipVisible, setIsTooltipVisible] = React.useState(false);
    React.useEffect(() => {
        if (!titleRef.current || !truncateTitle) {
            return;
        }
        titleRef.current.style.setProperty(c_notification_drawer__list_item_header_title_max_lines_1.default.name, truncateTitle.toString());
        const showTooltip = titleRef.current && titleRef.current.offsetHeight < titleRef.current.scrollHeight;
        if (isTooltipVisible !== showTooltip) {
            setIsTooltipVisible(showTooltip);
        }
    }, [titleRef, truncateTitle, isTooltipVisible]);
    const Icon = exports.variantIcons[variant];
    const Title = (React.createElement("h2", Object.assign({}, (isTooltipVisible && { tabIndex: 0 }), { ref: titleRef, className: react_styles_1.css(notification_drawer_1.default.notificationDrawerListItemHeaderTitle, truncateTitle && notification_drawer_1.default.modifiers.truncate) }),
        srTitle && React.createElement("span", { className: react_styles_1.css(accessibility_1.default.screenReader) }, srTitle),
        title));
    return (React.createElement(React.Fragment, null,
        React.createElement("div", Object.assign({}, props, { className: react_styles_1.css(notification_drawer_1.default.notificationDrawerListItemHeader, className) }),
            React.createElement("span", { className: react_styles_1.css(notification_drawer_1.default.notificationDrawerListItemHeaderIcon) }, icon ? icon : React.createElement(Icon, null)),
            isTooltipVisible ? (React.createElement(Tooltip_1.Tooltip, { content: title, position: tooltipPosition }, Title)) : (Title)),
        children && React.createElement("div", { className: react_styles_1.css(notification_drawer_1.default.notificationDrawerListItemAction) }, children)));
};
exports.NotificationDrawerListItemHeader = NotificationDrawerListItemHeader;
exports.NotificationDrawerListItemHeader.displayName = 'NotificationDrawerListItemHeader';
//# sourceMappingURL=NotificationDrawerListItemHeader.js.map