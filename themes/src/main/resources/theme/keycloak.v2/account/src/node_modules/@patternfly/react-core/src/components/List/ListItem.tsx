import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/List/list';
import { css } from '@patternfly/react-styles';

export interface ListItemProps extends React.HTMLProps<HTMLLIElement> {
  /** Icon for the list item */
  icon?: React.ReactNode | null;
  /** Anything that can be rendered inside of list item */
  children: React.ReactNode;
}

export const ListItem: React.FunctionComponent<ListItemProps> = ({
  icon = null,
  children = null,
  ...props
}: ListItemProps) => (
  <li className={css(icon && styles.listItem)} {...props}>
    {icon && <span className={css(styles.listItemIcon)}>{icon}</span>}
    {children}
  </li>
);
ListItem.displayName = 'ListItem';
