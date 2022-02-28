import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/layouts/Flex/flex';

import { FlexItemBreakpointMod } from './FlexUtils';
import { formatBreakpointMods } from '../../helpers/util';

export interface FlexItemProps extends React.HTMLProps<HTMLDivElement> {
  /** content rendered inside the Flex layout */
  children?: React.ReactNode;
  /** additional classes added to the Flex layout */
  className?: string;
  /** An array of objects representing the various modifiers to apply to the flex item at various breakpoints */
  breakpointMods?: FlexItemBreakpointMod[];
}

export const FlexItem: React.FunctionComponent<FlexItemProps> = ({
  children = null,
  className = '',
  breakpointMods = [] as FlexItemBreakpointMod[],
  ...props
}: FlexItemProps) => (
  <div {...props} className={css(breakpointMods.length > 0 && formatBreakpointMods(breakpointMods, styles), className)}>
    {children}
  </div>
);
