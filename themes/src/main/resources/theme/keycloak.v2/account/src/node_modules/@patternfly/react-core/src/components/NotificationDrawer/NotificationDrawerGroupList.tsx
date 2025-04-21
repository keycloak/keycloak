import * as React from 'react';

import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/NotificationDrawer/notification-drawer';

export interface NotificationDrawerGroupListProps extends React.HTMLProps<HTMLDivElement> {
  /**  Content rendered inside the notification drawer list body */
  children?: React.ReactNode;
  /**  Additional classes added to the notification drawer list body */
  className?: string;
}

export const NotificationDrawerGroupList: React.FunctionComponent<NotificationDrawerGroupListProps> = ({
  children,
  className = '',
  ...props
}: NotificationDrawerGroupListProps) => (
  <div {...props} className={css(styles.notificationDrawerGroupList, className)}>
    {children}
  </div>
);
NotificationDrawerGroupList.displayName = 'NotificationDrawerGroupList';
