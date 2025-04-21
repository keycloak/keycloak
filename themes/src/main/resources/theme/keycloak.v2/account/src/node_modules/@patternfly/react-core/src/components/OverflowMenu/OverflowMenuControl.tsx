import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/OverflowMenu/overflow-menu';
import { OverflowMenuContext } from './OverflowMenuContext';

export interface OverflowMenuControlProps extends React.HTMLProps<HTMLDivElement> {
  /** Any elements that can be rendered in the menu */
  children?: any;
  /** Additional classes added to the OverflowMenuControl */
  className?: string;
  /** Triggers the overflow dropdown to persist at all viewport sizes */
  hasAdditionalOptions?: boolean;
}

export const OverflowMenuControl: React.FunctionComponent<OverflowMenuControlProps> = ({
  className,
  children,
  hasAdditionalOptions,
  ...props
}: OverflowMenuControlProps) => (
  <OverflowMenuContext.Consumer>
    {value =>
      (value.isBelowBreakpoint || hasAdditionalOptions) && (
        <div className={css(styles.overflowMenuControl, className)} {...props}>
          {' '}
          {children}{' '}
        </div>
      )
    }
  </OverflowMenuContext.Consumer>
);
OverflowMenuControl.displayName = 'OverflowMenuControl';
