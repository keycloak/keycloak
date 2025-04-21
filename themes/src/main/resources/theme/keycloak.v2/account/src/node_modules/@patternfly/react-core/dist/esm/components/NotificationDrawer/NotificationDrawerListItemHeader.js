import { __rest } from "tslib";
import * as React from 'react';
import BellIcon from '@patternfly/react-icons/dist/esm/icons/bell-icon';
import CheckCircleIcon from '@patternfly/react-icons/dist/esm/icons/check-circle-icon';
import ExclamationCircleIcon from '@patternfly/react-icons/dist/esm/icons/exclamation-circle-icon';
import ExclamationTriangleIcon from '@patternfly/react-icons/dist/esm/icons/exclamation-triangle-icon';
import InfoCircleIcon from '@patternfly/react-icons/dist/esm/icons/info-circle-icon';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/NotificationDrawer/notification-drawer';
import a11yStyles from '@patternfly/react-styles/css/utilities/Accessibility/accessibility';
import maxLines from '@patternfly/react-tokens/dist/esm/c_notification_drawer__list_item_header_title_max_lines';
import { Tooltip } from '../Tooltip';
export const variantIcons = {
    success: CheckCircleIcon,
    danger: ExclamationCircleIcon,
    warning: ExclamationTriangleIcon,
    info: InfoCircleIcon,
    default: BellIcon
};
export const NotificationDrawerListItemHeader = (_a) => {
    var { children, className = '', icon = null, srTitle, title, variant = 'default', truncateTitle = 0, tooltipPosition } = _a, props = __rest(_a, ["children", "className", "icon", "srTitle", "title", "variant", "truncateTitle", "tooltipPosition"]);
    const titleRef = React.useRef(null);
    const [isTooltipVisible, setIsTooltipVisible] = React.useState(false);
    React.useEffect(() => {
        if (!titleRef.current || !truncateTitle) {
            return;
        }
        titleRef.current.style.setProperty(maxLines.name, truncateTitle.toString());
        const showTooltip = titleRef.current && titleRef.current.offsetHeight < titleRef.current.scrollHeight;
        if (isTooltipVisible !== showTooltip) {
            setIsTooltipVisible(showTooltip);
        }
    }, [titleRef, truncateTitle, isTooltipVisible]);
    const Icon = variantIcons[variant];
    const Title = (React.createElement("h2", Object.assign({}, (isTooltipVisible && { tabIndex: 0 }), { ref: titleRef, className: css(styles.notificationDrawerListItemHeaderTitle, truncateTitle && styles.modifiers.truncate) }),
        srTitle && React.createElement("span", { className: css(a11yStyles.screenReader) }, srTitle),
        title));
    return (React.createElement(React.Fragment, null,
        React.createElement("div", Object.assign({}, props, { className: css(styles.notificationDrawerListItemHeader, className) }),
            React.createElement("span", { className: css(styles.notificationDrawerListItemHeaderIcon) }, icon ? icon : React.createElement(Icon, null)),
            isTooltipVisible ? (React.createElement(Tooltip, { content: title, position: tooltipPosition }, Title)) : (Title)),
        children && React.createElement("div", { className: css(styles.notificationDrawerListItemAction) }, children)));
};
NotificationDrawerListItemHeader.displayName = 'NotificationDrawerListItemHeader';
//# sourceMappingURL=NotificationDrawerListItemHeader.js.map