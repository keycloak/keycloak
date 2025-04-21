import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/ProgressStepper/progress-stepper';
import { css } from '@patternfly/react-styles';

export interface ProgressStepperProps
  extends React.DetailedHTMLProps<React.OlHTMLAttributes<HTMLOListElement>, HTMLOListElement> {
  /** Content rendered inside the progress stepper. */
  children?: React.ReactNode;
  /** Additional classes applied to the progress stepper container. */
  className?: string;
  /** Flag indicating the progress stepper should be centered. */
  isCenterAligned?: boolean;
  /** Flag indicating the progress stepper has a vertical layout. */
  isVertical?: boolean;
  /** Flag indicating the progress stepper should be rendered compactly. */
  isCompact?: boolean;
}

export const ProgressStepper: React.FunctionComponent<ProgressStepperProps> = ({
  children,
  className,
  isCenterAligned,
  isVertical,
  isCompact,
  ...props
}: ProgressStepperProps) => (
  <ol
    className={css(
      styles.progressStepper,
      isCenterAligned && styles.modifiers.center,
      isVertical && styles.modifiers.vertical,
      isCompact && styles.modifiers.compact,
      className
    )}
    {...props}
  >
    {children}
  </ol>
);
ProgressStepper.displayName = 'ProgressStepper';
