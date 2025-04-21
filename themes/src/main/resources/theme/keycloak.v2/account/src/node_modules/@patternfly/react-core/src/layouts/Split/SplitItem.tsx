import * as React from 'react';
import styles from '@patternfly/react-styles/css/layouts/Split/split';
import { css } from '@patternfly/react-styles';

export interface SplitItemProps extends React.HTMLProps<HTMLDivElement> {
  /** Flag indicating if this Split Layout item should fill the available horizontal space. */
  isFilled?: boolean;
  /** content rendered inside the Split Layout Item */
  children?: React.ReactNode;
  /** additional classes added to the Split Layout Item */
  className?: string;
}

export const SplitItem: React.FunctionComponent<SplitItemProps> = ({
  isFilled = false,
  className = '',
  children = null,
  ...props
}: SplitItemProps) => (
  <div {...props} className={css(styles.splitItem, isFilled && styles.modifiers.fill, className)}>
    {children}
  </div>
);
SplitItem.displayName = 'SplitItem';
