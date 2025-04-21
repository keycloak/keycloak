import * as React from 'react';
import styles from '@patternfly/react-styles/css/layouts/Grid/grid';
import { css } from '@patternfly/react-styles';
import { DeviceSizes } from '../../styles/sizes';
import * as gridToken from '@patternfly/react-tokens/dist/esm/l_grid_item_Order';

import { setBreakpointCssVars } from '../../helpers/util';

export type gridSpans = 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12;

export interface GridItemProps extends React.HTMLProps<HTMLDivElement> {
  /** content rendered inside the Grid Layout Item */
  children?: React.ReactNode;
  /** additional classes added to the Grid Layout Item */
  className?: string;
  /** the number of columns the grid item spans. Value should be a number 1-12   */
  span?: gridSpans;
  /** the number of rows the grid item spans. Value should be a number 1-12   */
  rowSpan?: gridSpans;
  /** the number of columns a grid item is offset */
  offset?: gridSpans;
  /** the number of columns the grid item spans on small device. Value should be a number 1-12   */
  sm?: gridSpans;
  /** the number of rows the grid item spans on medium device. Value should be a number 1-12   */
  smRowSpan?: gridSpans;
  /** the number of columns the grid item is offset on small device. Value should be a number 1-12   */
  smOffset?: gridSpans;
  /** the number of columns the grid item spans on medium device. Value should be a number 1-12   */
  md?: gridSpans;
  /** the number of rows the grid item spans on medium device. Value should be a number 1-12   */
  mdRowSpan?: gridSpans;
  /** the number of columns the grid item is offset on medium device. Value should be a number 1-12   */
  mdOffset?: gridSpans;
  /** the number of columns the grid item spans on large device. Value should be a number 1-12   */
  lg?: gridSpans;
  /** the number of rows the grid item spans on large device. Value should be a number 1-12   */
  lgRowSpan?: gridSpans;
  /** the number of columns the grid item is offset on large device. Value should be a number 1-12   */
  lgOffset?: gridSpans;
  /** the number of columns the grid item spans on xLarge device. Value should be a number 1-12   */
  xl?: gridSpans;
  /** the number of rows the grid item spans on large device. Value should be a number 1-12   */
  xlRowSpan?: gridSpans;
  /** the number of columns the grid item is offset on xLarge device. Value should be a number 1-12   */
  xlOffset?: gridSpans;
  /** the number of columns the grid item spans on 2xLarge device. Value should be a number 1-12   */
  xl2?: gridSpans;
  /** the number of rows the grid item spans on 2xLarge device. Value should be a number 1-12   */
  xl2RowSpan?: gridSpans;
  /** the number of columns the grid item is offset on 2xLarge device. Value should be a number 1-12   */
  xl2Offset?: gridSpans;
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

export const GridItem: React.FunctionComponent<GridItemProps> = ({
  children = null,
  className = '',
  component = 'div',
  span = null,
  rowSpan = null,
  offset = null,
  order,
  style,
  ...props
}: GridItemProps) => {
  const classes = [
    styles.gridItem,
    span && styles.modifiers[`${span}Col` as keyof typeof styles.modifiers],
    rowSpan && styles.modifiers[`${rowSpan}Row` as keyof typeof styles.modifiers],
    offset && styles.modifiers[`offset_${offset}Col` as keyof typeof styles.modifiers]
  ];
  const Component: any = component;

  Object.entries(DeviceSizes).forEach(([propKey, classModifier]) => {
    const key = propKey as keyof typeof DeviceSizes;
    const rowSpanKey = `${key}RowSpan` as 'smRowSpan' | 'mdRowSpan' | 'lgRowSpan' | 'xlRowSpan' | 'xl2RowSpan';
    const offsetKey = `${key}Offset` as 'smOffset' | 'mdOffset' | 'lgOffset' | 'xlOffset' | 'xl2Offset';

    const spanValue = props[key] as gridSpans;
    const rowSpanValue = props[rowSpanKey] as gridSpans;
    const offsetValue = props[offsetKey] as gridSpans;

    if (spanValue) {
      classes.push(styles.modifiers[`${spanValue}ColOn${classModifier}` as keyof typeof styles.modifiers]);
    }
    if (rowSpanValue) {
      classes.push(styles.modifiers[`${rowSpanValue}RowOn${classModifier}` as keyof typeof styles.modifiers]);
    }
    if (offsetValue) {
      classes.push(styles.modifiers[`offset_${offsetValue}ColOn${classModifier}` as keyof typeof styles.modifiers]);
    }

    delete props[key];
    delete props[rowSpanKey];
    delete props[offsetKey];
  });

  return (
    <Component
      className={css(...classes, className)}
      style={
        style || order ? { ...style, ...setBreakpointCssVars(order, gridToken.l_grid_item_Order.name) } : undefined
      }
      {...props}
    >
      {children}
    </Component>
  );
};
GridItem.displayName = 'GridItem';
