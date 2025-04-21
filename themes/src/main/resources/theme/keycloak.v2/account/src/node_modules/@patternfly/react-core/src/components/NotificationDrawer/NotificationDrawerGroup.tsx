import * as React from 'react';

import AngleRightIcon from '@patternfly/react-icons/dist/esm/icons/angle-right-icon';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/NotificationDrawer/notification-drawer';
import maxLines from '@patternfly/react-tokens/dist/esm/c_notification_drawer__group_toggle_title_max_lines';

import { Badge } from '../Badge';
import { Tooltip, TooltipPosition } from '../Tooltip';

export interface NotificationDrawerGroupProps extends Omit<React.HTMLProps<HTMLDivElement>, 'title'> {
  /**  Content rendered inside the group */
  children?: React.ReactNode;
  /**  Additional classes added to the group */
  className?: string;
  /**  Notification drawer group count */
  count: number;
  /**  Adds styling to the group to indicate expanded state */
  isExpanded: boolean;
  /**  Adds styling to the group to indicate whether it has been read */
  isRead?: boolean;
  /**  Callback for when group button is clicked to expand */
  onExpand?: (event: any, value: boolean) => void;
  /**  Notification drawer group title */
  title: string | React.ReactNode;
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

export const NotificationDrawerGroup: React.FunctionComponent<NotificationDrawerGroupProps> = ({
  children,
  className = '',
  count,
  isExpanded,
  isRead = false,
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  onExpand = (event: any, expanded: boolean) => undefined as any,
  title,
  truncateTitle = 0,
  tooltipPosition,
  ...props
}: NotificationDrawerGroupProps) => {
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

  const Title = (
    <div
      {...(isTooltipVisible && { tabIndex: 0 })}
      ref={titleRef}
      className={css(styles.notificationDrawerGroupToggleTitle)}
    >
      {title}
    </div>
  );

  return (
    <section
      {...props}
      className={css(styles.notificationDrawerGroup, isExpanded && styles.modifiers.expanded, className)}
    >
      <h1>
        <button
          className={css(styles.notificationDrawerGroupToggle)}
          aria-expanded={isExpanded}
          onClick={e => onExpand(e, !isExpanded)}
          onKeyDown={e => {
            if (e.key === 'Enter' || e.key === ' ') {
              e.preventDefault();
              onExpand(e, !isExpanded);
            }
          }}
        >
          {isTooltipVisible ? (
            <Tooltip content={title} position={tooltipPosition}>
              {Title}
            </Tooltip>
          ) : (
            Title
          )}
          <div className={css(styles.notificationDrawerGroupToggleCount)}>
            <Badge isRead={isRead}>{count}</Badge>
          </div>

          <span className="pf-c-notification-drawer__group-toggle-icon">
            <AngleRightIcon />
          </span>
        </button>
      </h1>
      {children}
    </section>
  );
};
NotificationDrawerGroup.displayName = 'NotificationDrawerGroup';
