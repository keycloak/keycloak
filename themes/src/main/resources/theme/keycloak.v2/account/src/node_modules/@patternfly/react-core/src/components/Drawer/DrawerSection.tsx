import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Drawer/drawer';
import { css } from '@patternfly/react-styles';
import { DrawerColorVariant } from './Drawer';

export interface DrawerSectionProps extends React.HTMLProps<HTMLDivElement> {
  /** Additional classes added to the drawer section. */
  className?: string;
  /** Content to be rendered in the drawer section. */
  children?: React.ReactNode;
  /** Color variant of the background of the drawer Section */
  colorVariant?: DrawerColorVariant | 'light-200' | 'default';
}

export const DrawerSection: React.FunctionComponent<DrawerSectionProps> = ({
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  className = '',
  children,
  colorVariant = DrawerColorVariant.default,
  ...props
}: DrawerSectionProps) => (
  <div
    className={css(
      styles.drawerSection,
      colorVariant === DrawerColorVariant.light200 && styles.modifiers.light_200,
      className
    )}
    {...props}
  >
    {children}
  </div>
);
DrawerSection.displayName = 'DrawerSection';
