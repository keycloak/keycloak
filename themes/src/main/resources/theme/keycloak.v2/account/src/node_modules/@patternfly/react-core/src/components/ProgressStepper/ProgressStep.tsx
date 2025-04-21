import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/ProgressStepper/progress-stepper';
import { css } from '@patternfly/react-styles';
import CheckCircleIcon from '@patternfly/react-icons/dist/esm/icons/check-circle-icon';
import ResourcesFullIcon from '@patternfly/react-icons/dist/esm/icons/resources-full-icon';
import ExclamationTriangleIcon from '@patternfly/react-icons/dist/esm/icons/exclamation-triangle-icon';
import ExclamationCircleIcon from '@patternfly/react-icons/dist/esm/icons/exclamation-circle-icon';

export interface ProgressStepProps
  extends React.DetailedHTMLProps<React.LiHTMLAttributes<HTMLLIElement>, HTMLLIElement> {
  /** Content rendered inside the progress step. */
  children?: React.ReactNode;
  /** Additional classes applied to the progress step container. */
  className?: string;
  /** Variant of the progress step. Each variant has a default icon. */
  variant?: 'default' | 'success' | 'info' | 'pending' | 'warning' | 'danger';
  /** Flag indicating the progress step is the current step. */
  isCurrent?: boolean;
  /** Custom icon of a progress step. Will override default icons provided by the variant. */
  icon?: React.ReactNode;
  /** Description text of a progress step. */
  description?: string;
  /** ID of the title of the progress step. */
  titleId?: string;
  /** Accessible label for the progress step. Should communicate all information being communicated by the progress
   * step's icon, including the variant and the completed status. */
  'aria-label'?: string;
  /** Forwards the step ref to rendered function.  Use this prop to add a popover to the step.*/
  popoverRender?: (stepRef: React.RefObject<any>) => React.ReactNode;
}

const variantIcons = {
  default: undefined as any,
  pending: undefined as any,
  success: <CheckCircleIcon />,
  info: <ResourcesFullIcon />,
  warning: <ExclamationTriangleIcon />,
  danger: <ExclamationCircleIcon />
};

const variantStyle = {
  default: '',
  info: styles.modifiers.info,
  success: styles.modifiers.success,
  pending: styles.modifiers.pending,
  warning: styles.modifiers.warning,
  danger: styles.modifiers.danger
};

export const ProgressStep: React.FunctionComponent<ProgressStepProps> = ({
  children,
  className,
  variant,
  isCurrent,
  description,
  icon,
  titleId,
  'aria-label': ariaLabel,
  popoverRender,
  ...props
}: ProgressStepProps) => {
  const _icon = icon !== undefined ? icon : variantIcons[variant];
  const Component = popoverRender !== undefined ? 'button' : 'div';
  const stepRef = React.useRef();

  if (props.id === undefined || titleId === undefined) {
    /* eslint-disable no-console */
    console.warn(
      'ProgressStep: The titleId and id properties are required to make this component accessible, and one or both of these properties are missing.'
    );
  }

  return (
    <li
      className={css(
        styles.progressStepperStep,
        variantStyle[variant],
        isCurrent && styles.modifiers.current,
        className
      )}
      aria-label={ariaLabel}
      {...(isCurrent && { 'aria-current': 'step' })}
      {...props}
    >
      <div className={css(styles.progressStepperStepConnector)}>
        <span className={css(styles.progressStepperStepIcon)}>{_icon && _icon}</span>
      </div>
      <div className={css(styles.progressStepperStepMain)}>
        <Component
          className={css(styles.progressStepperStepTitle, popoverRender && styles.modifiers.helpText)}
          id={titleId}
          ref={stepRef}
          {...(popoverRender && { type: 'button' })}
          {...(props.id !== undefined && titleId !== undefined && { 'aria-labelledby': `${props.id} ${titleId}` })}
        >
          {children}
          {popoverRender && popoverRender(stepRef)}
        </Component>
        {description && <div className={css(styles.progressStepperStepDescription)}>{description}</div>}
      </div>
    </li>
  );
};

ProgressStep.displayName = 'ProgressStep';
