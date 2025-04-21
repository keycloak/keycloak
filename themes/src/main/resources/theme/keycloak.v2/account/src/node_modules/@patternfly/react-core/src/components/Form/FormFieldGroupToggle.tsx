import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Form/form';
import { css } from '@patternfly/react-styles';
import AngleRightIcon from '@patternfly/react-icons/dist/esm/icons/angle-right-icon';
import { Button } from '../Button';

export interface FormFieldGroupToggleProps extends React.HTMLProps<HTMLDivElement> {
  /** Additional classes added to the section */
  className?: string;
  /** Callback for onClick */
  onToggle: () => void;
  /** Flag indicating if the toggle is expanded */
  isExpanded: boolean;
  /** Aria label of the toggle button */
  'aria-label'?: string;
  /** Sets the aria-labelledby attribute on the toggle button element */
  'aria-labelledby'?: string;
  /** The id applied to the toggle button */
  toggleId?: string;
}

export const FormFieldGroupToggle: React.FunctionComponent<FormFieldGroupToggleProps> = ({
  className,
  onToggle,
  isExpanded,
  'aria-label': ariaLabel,
  'aria-labelledby': ariaLabelledby,
  toggleId,
  ...props
}: FormFieldGroupToggleProps) => (
  <div className={css(styles.formFieldGroupToggle, className)} {...props}>
    <div className={css(styles.formFieldGroupToggleButton)}>
      <Button
        variant="plain"
        aria-label={ariaLabel}
        onClick={onToggle}
        aria-expanded={isExpanded}
        aria-labelledby={ariaLabelledby}
        id={toggleId}
      >
        <span className={css(styles.formFieldGroupToggleIcon)}>
          <AngleRightIcon aria-hidden="true" />
        </span>
      </Button>
    </div>
  </div>
);
FormFieldGroupToggle.displayName = 'FormFieldGroupToggle';
