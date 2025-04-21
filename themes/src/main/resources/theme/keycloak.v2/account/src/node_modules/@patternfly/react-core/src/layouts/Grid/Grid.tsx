import * as React from 'react';
import styles from '@patternfly/react-styles/css/layouts/Grid/grid';
import { css } from '@patternfly/react-styles';
import { DeviceSizes } from '../../styles/sizes';
import * as gridToken from '@patternfly/react-tokens/dist/esm/l_grid_item_Order';

import { setBreakpointCssVars } from '../../helpers/util';

export type gridItemSpanValueShape = 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12;

export interface GridProps extends React.HTMLProps<HTMLDivElement> {
  /** content rendered inside the Grid layout */
  children?: React.ReactNode;
  /** additional classes added to the Grid layout */
  className?: string;
  /** Adds space between children. */
  hasGutter?: boolean;
  /** The number of rows a column in the grid should span.  Value should be a number 1-12 */
  span?: gridItemSpanValueShape;
  /** the number of columns all grid items should span on a small device */
  sm?: gridItemSpanValueShape;
  /** the number of columns all grid items should span on a medium device */
  md?: gridItemSpanValueShape;
  /** the number of columns all grid items should span on a large device */
  lg?: gridItemSpanValueShape;
  /** the number of columns all grid items should span on a xLarge device */
  xl?: gridItemSpanValueShape;
  /** the number of columns all grid items should span on a 2xLarge device */
  xl2?: gridItemSpanValueShape;
  /** Modifies the flex layout element order property */
  order?: {
    default?: string;
    md?: string;
    lg?: string;
    xl?: string;
    '2xl'?: string;
  };
  /** Sets the base component to render. defaults to div */
  component?: React.ElementType<any> | React.ComponentType<any>;
}

export const Grid: React.FunctionComponent<GridProps> = ({
  children = null,
  className = '',
  component = 'div',
  hasGutter,
  span = null,
  order,
  style,
  ...props
}: GridProps) => {
  const classes = [styles.grid, span && styles.modifiers[`all_${span}Col` as keyof typeof styles.modifiers]];
  const Component: any = component;

  Object.entries(DeviceSizes).forEach(([propKey, gridSpanModifier]) => {
    const key = propKey as keyof typeof DeviceSizes;
    const propValue = props[key] as gridItemSpanValueShape;
    if (propValue) {
      classes.push(styles.modifiers[`all_${propValue}ColOn${gridSpanModifier}` as keyof typeof styles.modifiers]);
    }
    delete props[key];
  });

  return (
    <Component
      className={css(...classes, hasGutter && styles.modifiers.gutter, className)}
      style={
        style || order ? { ...style, ...setBreakpointCssVars(order, gridToken.l_grid_item_Order.name) } : undefined
      }
      {...props}
    >
      {children}
    </Component>
  );
};
Grid.displayName = 'Grid';
