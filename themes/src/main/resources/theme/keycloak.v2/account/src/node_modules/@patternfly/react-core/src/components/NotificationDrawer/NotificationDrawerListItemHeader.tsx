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

import { Tooltip, TooltipPosition } from '../Tooltip';

export const variantIcons = {
  success: CheckCircleIcon,
  danger: ExclamationCircleIcon,
  warning: ExclamationTriangleIcon,
  info: InfoCircleIcon,
  default: BellIcon
};

export interface NotificationDrawerListItemHeaderProps extends React.HTMLProps<HTMLDivElement> {
  /**  Actions rendered inside the notification drawer list item header */
  children?: React.ReactNode;
  /**  Additional classes for notification drawer list item header. */
  className?: string;
  /**  Add custom icon for notification drawer list item header */
  icon?: React.ReactNode;
  /**  Notification drawer list item header screen reader title */
  srTitle?: string;
  /**  Notification drawer list item title */
  title: string;
  /**  Variant indicates the severity level */
  variant?: 'success' | 'danger' | 'warning' | 'info' | 'default';
  /** Truncate title to number of lines */
  truncateTitle?: number;
  /** Position of the tooltip which is displayed if text is truncated */
  tooltipPosition?:
    | TooltipPosition
    | 'auto'
    | 'top'
    | 'bottom'
    | 'left'
    | 'right'
    | 'top-start'
    | 'top-end'
    | 'bottom-start'
    | 'bottom-end'
    | 'left-start'
    | 'left-end'
    | 'right-start'
    | 'right-end';
}

export const NotificationDrawerListItemHeader: React.FunctionComponent<NotificationDrawerListItemHeaderProps> = ({
  children,
  className = '',
  icon = null,
  srTitle,
  title,
  variant = 'default',
  truncateTitle = 0,
  tooltipPosition,
  ...props
}: NotificationDrawerListItemHeaderProps) => {
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
  const Title = (
    <h2
      {...(isTooltipVisible && { tabIndex: 0 })}
      ref={titleRef}
      className={css(styles.notificationDrawerListItemHeaderTitle, truncateTitle && styles.modifiers.truncate)}
    >
      {srTitle && <span className={css(a11yStyles.screenReader)}>{srTitle}</span>}
      {title}
    </h2>
  );

  return (
    <React.Fragment>
      <div {...props} className={css(styles.notificationDrawerListItemHeader, className)}>
        <span className={css(styles.notificationDrawerListItemHeaderIcon)}>{icon ? icon : <Icon />}</span>
        {isTooltipVisible ? (
          <Tooltip content={title} position={tooltipPosition}>
            {Title}
          </Tooltip>
        ) : (
          Title
        )}
      </div>
      {children && <div className={css(styles.notificationDrawerListItemAction)}>{children}</div>}
    </React.Fragment>
  );
};
NotificationDrawerListItemHeader.displayName = 'NotificationDrawerListItemHeader';
