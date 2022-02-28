import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Drawer/drawer';
import { css } from '@patternfly/react-styles';

export interface DrawerSectionProps extends React.HTMLProps<HTMLDivElement> {
  /** Additional classes added to the drawer section. */
  className?: string;
  /** Content to be rendered in the drawer section. */
  children?: React.ReactNode;
}

export const DrawerSection: React.SFC<DrawerSectionProps> = ({
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  className = '',
  children,
  ...props
}: DrawerSectionProps) => (
  <div className={css(styles.drawerSection, className)} {...props}>
    {children}
  </div>
);
