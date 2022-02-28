import * as React from 'react';
import { css, getModifier } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/DataList/data-list';

export interface DataListCellProps extends Omit<React.HTMLProps<HTMLDivElement>, 'width'> {
  /** Content rendered inside the DataList cell */
  children?: React.ReactNode;
  /** Additional classes added to the DataList cell */
  className?: string;
  /** Width (from 1-5) to the DataList cell */
  width?: 1 | 2 | 3 | 4 | 5;
  /** Enables the body Content to fill the height of the card */
  isFilled?: boolean;
  /**  Aligns the cell content to the right of its parent. */
  alignRight?: boolean;
  /** Set to true if the cell content is an Icon */
  isIcon?: boolean;
}

export const DataListCell: React.FunctionComponent<DataListCellProps> = ({
  children = null,
  className = '',
  width = 1,
  isFilled = true,
  alignRight = false,
  isIcon = false,
  ...props
}: DataListCellProps) => (
  <div
    className={css(
      styles.dataListCell,
      width > 1 && getModifier(styles, `flex_${width}`, ''),
      !isFilled && styles.modifiers.noFill,
      alignRight && styles.modifiers.alignRight,
      isIcon && styles.modifiers.icon,
      className
    )}
    {...props}
  >
    {children}
  </div>
);
