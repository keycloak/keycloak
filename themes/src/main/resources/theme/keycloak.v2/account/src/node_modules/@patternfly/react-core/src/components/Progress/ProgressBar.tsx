import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Progress/progress';
import { css } from '@patternfly/react-styles';

export interface AriaProps {
  'aria-labelledby'?: string;
  'aria-label'?: string;
  'aria-valuemin'?: number;
  'aria-valuenow'?: number;
  'aria-valuemax'?: number;
  'aria-valuetext'?: string;
}

export interface ProgressBarProps extends React.HTMLProps<HTMLDivElement> {
  /** What should be rendered inside progress bar. */
  children?: React.ReactNode;
  /** Additional classes for Progres bar. */
  className?: string;
  /** Actual progress value. */
  value: number;
  /** Minimal value of progress. */
  progressBarAriaProps?: AriaProps;
}

export const ProgressBar: React.FunctionComponent<ProgressBarProps> = ({
  progressBarAriaProps,
  className = '',
  children = null,
  value,
  ...props
}: ProgressBarProps) => (
  <div {...props} className={css(styles.progressBar, className)} {...progressBarAriaProps}>
    <div className={css(styles.progressIndicator)} style={{ width: `${value}%` }}>
      <span className={css(styles.progressMeasure)}>{children}</span>
    </div>
  </div>
);
ProgressBar.displayName = 'ProgressBar';
