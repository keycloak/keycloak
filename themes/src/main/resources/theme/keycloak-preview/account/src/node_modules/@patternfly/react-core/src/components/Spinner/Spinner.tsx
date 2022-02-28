import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Spinner/spinner';
import { css, getModifier } from '@patternfly/react-styles';

export enum spinnerSize {
  sm = 'sm',
  md = 'md',
  lg = 'lg',
  xl = 'xl'
}

export interface SpinnerProps extends Omit<React.HTMLProps<HTMLSpanElement>, 'size'> {
  /** Additional classes added to the Spinner. */
  className?: string;
  /** Size variant of progress. */
  size?: 'sm' | 'md' | 'lg' | 'xl';
  /** Aria value text */
  'aria-valuetext'?: string;
}

export const Spinner: React.FunctionComponent<SpinnerProps> = ({
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  className = '',
  size = 'xl',
  'aria-valuetext': ariaValueText = 'Loading...',
  ...props
}: SpinnerProps) => (
  <span
    className={css(styles.spinner, getModifier(styles, size))}
    role="progressbar"
    aria-valuetext={ariaValueText}
    {...props}
  >
    <span className={css(styles.spinnerClipper)} />
    <span className={css(styles.spinnerLeadBall)} />
    <span className={css(styles.spinnerTailBall)} />
  </span>
);
