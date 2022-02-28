import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/DataToolbar/data-toolbar';
import { css, getModifier } from '@patternfly/react-styles';

import { DataToolbarBreakpointMod } from './DataToolbarUtils';
import { formatBreakpointMods } from '../../helpers/util';

export enum DataToolbarItemVariant {
  separator = 'separator',
  'bulk-select' = 'bulk-select',
  'overflow-menu' = 'overflow-menu',
  pagination = 'pagination',
  'search-filter' = 'search-filter',
  label = 'label',
  'chip-group' = 'chip-group'
}

export interface DataToolbarItemProps extends React.HTMLProps<HTMLDivElement> {
  /** Classes applied to root element of the data toolbar item */
  className?: string;
  /** A type modifier which modifies spacing specifically depending on the type of item */
  variant?:
    | DataToolbarItemVariant
    | 'separator'
    | 'bulk-select'
    | 'overflow-menu'
    | 'pagination'
    | 'search-filter'
    | 'label'
    | 'chip-group';
  /** An array of objects representing the various modifiers to apply to the data toolbar item at various breakpoints */
  breakpointMods?: DataToolbarBreakpointMod[];
  /** id for this data toolbar item */
  id?: string;
  /** Content to be rendered inside the data toolbar item */
  children?: React.ReactNode;
}

export const DataToolbarItem: React.FunctionComponent<DataToolbarItemProps> = ({
  className,
  variant,
  breakpointMods = [] as DataToolbarBreakpointMod[],
  id,
  children,
  ...props
}: DataToolbarItemProps) => {
  const labelVariant = variant === 'label';

  return (
    <div
      className={css(
        styles.dataToolbarItem,
        variant && getModifier(styles, variant),
        formatBreakpointMods(breakpointMods, styles),
        className
      )}
      {...(labelVariant && { 'aria-hidden': true })}
      id={id}
      {...props}
    >
      {children}
    </div>
  );
};
