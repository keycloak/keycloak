import * as React from 'react';
import { Button, ButtonVariant, ButtonProps } from '../Button';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/NotificationBadge/notification-badge';

export interface NotificationBadgeProps extends ButtonProps {
  /**  Adds styling to the notification badge to indicate it has been read */
  isRead?: boolean;
  /** content rendered inside the Notification Badge */
  children?: React.ReactNode;
  /** additional classes added to the Notification Badge */
  className?: string;
  /** Adds accessible text to the Notification Badge. */
  'aria-label'?: string;
}

export const NotificationBadge: React.FunctionComponent<NotificationBadgeProps> = ({
  isRead = false,
  className,
  children,
  ...props
}: NotificationBadgeProps) => (
  <Button variant={ButtonVariant.plain} className={className} {...props}>
    <span className={css(styles.notificationBadge, isRead ? styles.modifiers.read : styles.modifiers.unread)}>
      {children}
    </span>
  </Button>
);
