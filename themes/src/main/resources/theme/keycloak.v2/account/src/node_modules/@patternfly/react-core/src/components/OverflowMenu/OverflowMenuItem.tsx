import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/OverflowMenu/overflow-menu';
import { OverflowMenuContext } from './OverflowMenuContext';

export interface OverflowMenuItemProps extends React.HTMLProps<HTMLDivElement> {
  /** Any elements that can be rendered in the menu */
  children?: any;
  /** Additional classes added to the OverflowMenuItem */
  className?: string;
  /** Modifies the overflow menu item visibility */
  isPersistent?: boolean;
}

export const OverflowMenuItem: React.FunctionComponent<OverflowMenuItemProps> = ({
  className,
  children,
  isPersistent = false
}: OverflowMenuItemProps) => (
  <OverflowMenuContext.Consumer>
    {value =>
      (isPersistent || !value.isBelowBreakpoint) && (
        <div className={css(styles.overflowMenuItem, className)}> {children} </div>
      )
    }
  </OverflowMenuContext.Consumer>
);
OverflowMenuItem.displayName = 'OverflowMenuItem';
