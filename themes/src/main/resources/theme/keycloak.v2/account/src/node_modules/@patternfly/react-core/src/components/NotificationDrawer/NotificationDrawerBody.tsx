import * as React from 'react';

import styles from '@patternfly/react-styles/css/components/NotificationDrawer/notification-drawer';
import { css } from '@patternfly/react-styles';

export interface NotificationDrawerBodyProps extends React.HTMLProps<HTMLDivElement> {
  /**  Content rendered inside the body of the notification drawer */
  children?: React.ReactNode;
  /**  Additional classes added to the notification drawer body */
  className?: string;
}

export const NotificationDrawerBody: React.FunctionComponent<NotificationDrawerBodyProps> = ({
  children,
  className = '',
  ...props
}: NotificationDrawerBodyProps) => (
  <div {...props} className={css(styles.notificationDrawerBody, className)}>
    {children}
  </div>
);
NotificationDrawerBody.displayName = 'NotificationDrawerBody';
