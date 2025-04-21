import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Tooltip/tooltip';
import { css } from '@patternfly/react-styles';

export interface TooltipContentProps extends React.HTMLProps<HTMLDivElement> {
  /** PopoverContent additional class */
  className?: string;
  /** PopoverContent content */
  children: React.ReactNode;
  /** Flag to align text to the left */
  isLeftAligned?: boolean;
}

export const TooltipContent: React.FunctionComponent<TooltipContentProps> = ({
  className,
  children,
  isLeftAligned,
  ...props
}: TooltipContentProps) => (
  <div className={css(styles.tooltipContent, isLeftAligned && styles.modifiers.textAlignLeft, className)} {...props}>
    {children}
  </div>
);
TooltipContent.displayName = 'TooltipContent';
