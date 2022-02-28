import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Drawer/drawer';
import { css } from '@patternfly/react-styles';
import { DrawerContext } from './Drawer';

export interface DrawerPanelContentProps extends React.HTMLProps<HTMLDivElement> {
  /** Additional classes added to the drawer. */
  className?: string;
  /** Content to be rendered in the drawer panel. */
  children?: React.ReactNode;
  /* Flag indicating that the drawer panel should have a border. */
  hasBorder?: boolean;
  /* Default width for drawer panel */
  width?: 25 | 33 | 50 | 66 | 75 | 100;
  /* Drawer panel width on large viewports */
  widthOnLg?: 25 | 33 | 50 | 66 | 75 | 100;
  /* Drawer panel width on xl viewports */
  widthOnXl?: 25 | 33 | 50 | 66 | 75 | 100;
  /* Drawer panel width on 2xl viewports */
  widthOn2Xl?: 25 | 33 | 50 | 66 | 75 | 100;
}

export const DrawerPanelContent: React.SFC<DrawerPanelContentProps> = ({
  className = '',
  children,
  hasBorder = false,
  width,
  widthOnLg,
  widthOnXl,
  widthOn2Xl,
  ...props
}: DrawerPanelContentProps) => (
  <DrawerContext.Consumer>
    {({ isExpanded }) => (
      <div
        className={css(
          styles.drawerPanel,
          hasBorder && styles.modifiers.border,
          width && styles.modifiers[`width_${width}` as keyof typeof styles.modifiers],
          widthOnLg && styles.modifiers[`width_${widthOnLg}OnLg` as keyof typeof styles.modifiers],
          widthOnXl && styles.modifiers[`width_${widthOnXl}OnXl` as keyof typeof styles.modifiers],
          widthOn2Xl && styles.modifiers[`width_${widthOn2Xl}On_2xl` as keyof typeof styles.modifiers],
          className
        )}
        hidden={!isExpanded}
        aria-hidden={!isExpanded}
        aria-expanded={isExpanded}
        {...props}
      >
        {children}
      </div>
    )}
  </DrawerContext.Consumer>
);
