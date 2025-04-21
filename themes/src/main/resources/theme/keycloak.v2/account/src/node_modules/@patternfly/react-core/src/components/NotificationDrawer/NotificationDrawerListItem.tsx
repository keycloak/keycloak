import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/NotificationDrawer/notification-drawer';

export interface NotificationDrawerListItemProps extends React.HTMLProps<HTMLLIElement> {
  /**  Content rendered inside the list item */
  children?: React.ReactNode;
  /**  Additional classes added to the list item */
  className?: string;
  /**  Modifies the list item to include hover styles on :hover */
  isHoverable?: boolean;
  /**  Adds styling to the list item to indicate it has been read */
  isRead?: boolean;
  /**  Callback for when a list item is clicked */
  onClick?: (event: any) => void;
  /**  Tab index for the list item */
  tabIndex?: number;
  /**  Variant indicates the severity level */
  variant?: 'default' | 'success' | 'danger' | 'warning' | 'info';
}

export const NotificationDrawerListItem: React.FunctionComponent<NotificationDrawerListItemProps> = ({
  children = null,
  className = '',
  isHoverable = true,
  isRead = false,
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  onClick = (event: React.MouseEvent) => undefined as any,
  tabIndex = 0,
  variant = 'default',
  ...props
}: NotificationDrawerListItemProps) => {
  const onKeyDown = (event: React.KeyboardEvent) => {
    // Accessibility function. Click on the list item when pressing Enter or Space on it.
    if (event.key === 'Enter' || event.key === ' ') {
      (event.target as HTMLElement).click();
    }
  };
  return (
    <li
      {...props}
      className={css(
        styles.notificationDrawerListItem,
        isHoverable && styles.modifiers.hoverable,
        styles.modifiers[variant],
        isRead && styles.modifiers.read,
        className
      )}
      tabIndex={tabIndex}
      onClick={e => onClick(e)}
      onKeyDown={onKeyDown}
    >
      {children}
    </li>
  );
};
NotificationDrawerListItem.displayName = 'NotificationDrawerListItem';
