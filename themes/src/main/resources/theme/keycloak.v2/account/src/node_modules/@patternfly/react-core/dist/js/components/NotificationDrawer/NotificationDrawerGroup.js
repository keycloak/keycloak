"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.NotificationDrawerGroup = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const angle_right_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/angle-right-icon'));
const react_styles_1 = require("@patternfly/react-styles");
const notification_drawer_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/NotificationDrawer/notification-drawer"));
const c_notification_drawer__group_toggle_title_max_lines_1 = tslib_1.__importDefault(require('@patternfly/react-tokens/dist/js/c_notification_drawer__group_toggle_title_max_lines'));
const Badge_1 = require("../Badge");
const Tooltip_1 = require("../Tooltip");
const NotificationDrawerGroup = (_a) => {
    var { children, className = '', count, isExpanded, isRead = false, 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    onExpand = (event, expanded) => undefined, title, truncateTitle = 0, tooltipPosition } = _a, props = tslib_1.__rest(_a, ["children", "className", "count", "isExpanded", "isRead", "onExpand", "title", "truncateTitle", "tooltipPosition"]);
    const titleRef = React.useRef(null);
    const [isTooltipVisible, setIsTooltipVisible] = React.useState(false);
    React.useEffect(() => {
        // Title will always truncate on overflow regardless of truncateTitle prop
        const showTooltip = titleRef.current && titleRef.current.offsetHeight < titleRef.current.scrollHeight;
        if (isTooltipVisible !== showTooltip) {
            setIsTooltipVisible(showTooltip);
        }
        if (!titleRef.current || !truncateTitle) {
            return;
        }
        titleRef.current.style.setProperty(c_notification_drawer__group_toggle_title_max_lines_1.default.name, truncateTitle.toString());
    }, [titleRef, truncateTitle, isTooltipVisible]);
    const Title = (React.createElement("div", Object.assign({}, (isTooltipVisible && { tabIndex: 0 }), { ref: titleRef, className: react_styles_1.css(notification_drawer_1.default.notificationDrawerGroupToggleTitle) }), title));
    return (React.createElement("section", Object.assign({}, props, { className: react_styles_1.css(notification_drawer_1.default.notificationDrawerGroup, isExpanded && notification_drawer_1.default.modifiers.expanded, className) }),
        React.createElement("h1", null,
            React.createElement("button", { className: react_styles_1.css(notification_drawer_1.default.notificationDrawerGroupToggle), "aria-expanded": isExpanded, onClick: e => onExpand(e, !isExpanded), onKeyDown: e => {
                    if (e.key === 'Enter' || e.key === ' ') {
                        e.preventDefault();
                        onExpand(e, !isExpanded);
                    }
                } },
                isTooltipVisible ? (React.createElement(Tooltip_1.Tooltip, { content: title, position: tooltipPosition }, Title)) : (Title),
                React.createElement("div", { className: react_styles_1.css(notification_drawer_1.default.notificationDrawerGroupToggleCount) },
                    React.createElement(Badge_1.Badge, { isRead: isRead }, count)),
                React.createElement("span", { className: "pf-c-notification-drawer__group-toggle-icon" },
                    React.createElement(angle_right_icon_1.default, null)))),
        children));
};
exports.NotificationDrawerGroup = NotificationDrawerGroup;
exports.NotificationDrawerGroup.displayName = 'NotificationDrawerGroup';
//# sourceMappingURL=NotificationDrawerGroup.js.map