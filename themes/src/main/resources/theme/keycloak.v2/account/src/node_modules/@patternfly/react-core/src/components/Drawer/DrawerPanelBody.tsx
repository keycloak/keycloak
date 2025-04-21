import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Drawer/drawer';
import { css } from '@patternfly/react-styles';

export interface DrawerPanelBodyProps extends React.HTMLProps<HTMLDivElement> {
  /** Additional classes added to the Drawer. */
  className?: string;
  /** Content to be rendered in the drawer */
  children?: React.ReactNode;
  /** Indicates if there should be no padding around the drawer panel body */
  hasNoPadding?: boolean;
}

export const DrawerPanelBody: React.FunctionComponent<DrawerPanelBodyProps> = ({
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  className = '',
  children,
  hasNoPadding = false,
  ...props
}: DrawerPanelBodyProps) => (
  <div className={css(styles.drawerBody, hasNoPadding && styles.modifiers.noPadding, className)} {...props}>
    {children}
  </div>
);
DrawerPanelBody.displayName = 'DrawerPanelBody';
