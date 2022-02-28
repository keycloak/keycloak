import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Drawer/drawer';
import { css } from '@patternfly/react-styles';
import { DrawerPanelBody } from './DrawerPanelBody';

export interface DrawerHeadProps extends React.HTMLProps<HTMLDivElement> {
  /** Additional classes added to the drawer head. */
  className?: string;
  /** Content to be rendered in the drawer head */
  children?: React.ReactNode;
  /** Indicates if there should be no padding around the drawer panel body of the head*/
  noPadding?: boolean;
}

export const DrawerHead: React.SFC<DrawerHeadProps> = ({
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  className = '',
  children,
  noPadding = false,
  ...props
}: DrawerHeadProps) => (
  <DrawerPanelBody noPadding={noPadding}>
    <div className={css(styles.drawerHead, className)} {...props}>
      {children}
    </div>
  </DrawerPanelBody>
);
