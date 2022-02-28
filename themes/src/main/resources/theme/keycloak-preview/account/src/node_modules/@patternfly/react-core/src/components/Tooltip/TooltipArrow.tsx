import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Tooltip/tooltip';
import { css } from '@patternfly/react-styles';

export interface TooltipArrowProps extends React.HTMLProps<HTMLDivElement> {
  /** className */
  className?: string;
}

export const TooltipArrow = ({ className, ...props }: TooltipArrowProps) => (
  <div className={css(styles.tooltipArrow, className)} {...props} />
);
