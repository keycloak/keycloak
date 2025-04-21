import { __rest } from "tslib";
import * as React from 'react';
import AngleRightIcon from '@patternfly/react-icons/dist/esm/icons/angle-right-icon';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/NotificationDrawer/notification-drawer';
import maxLines from '@patternfly/react-tokens/dist/esm/c_notification_drawer__group_toggle_title_max_lines';
import { Badge } from '../Badge';
import { Tooltip } from '../Tooltip';
export const NotificationDrawerGroup = (_a) => {
    var { children, className = '', count, isExpanded, isRead = false, 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    onExpand = (event, expanded) => undefined, title, truncateTitle = 0, tooltipPosition } = _a, props = __rest(_a, ["children", "className", "count", "isExpanded", "isRead", "onExpand", "title", "truncateTitle", "tooltipPosition"]);
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
        titleRef.current.style.setProperty(maxLines.name, truncateTitle.toString());
    }, [titleRef, truncateTitle, isTooltipVisible]);
    const Title = (React.createElement("div", Object.assign({}, (isTooltipVisible && { tabIndex: 0 }), { ref: titleRef, className: css(styles.notificationDrawerGroupToggleTitle) }), title));
    return (React.createElement("section", Object.assign({}, props, { className: css(styles.notificationDrawerGroup, isExpanded && styles.modifiers.expanded, className) }),
        React.createElement("h1", null,
            React.createElement("button", { className: css(styles.notificationDrawerGroupToggle), "aria-expanded": isExpanded, onClick: e => onExpand(e, !isExpanded), onKeyDown: e => {
                    if (e.key === 'Enter' || e.key === ' ') {
                        e.preventDefault();
                        onExpand(e, !isExpanded);
                    }
                } },
                isTooltipVisible ? (React.createElement(Tooltip, { content: title, position: tooltipPosition }, Title)) : (Title),
                React.createElement("div", { className: css(styles.notificationDrawerGroupToggleCount) },
                    React.createElement(Badge, { isRead: isRead }, count)),
                React.createElement("span", { className: "pf-c-notification-drawer__group-toggle-icon" },
                    React.createElement(AngleRightIcon, null)))),
        children));
};
NotificationDrawerGroup.displayName = 'NotificationDrawerGroup';
//# sourceMappingURL=NotificationDrawerGroup.js.map