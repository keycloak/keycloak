import * as React from 'react';
import { Button, ButtonVariant, ButtonProps } from '../Button';
import TimesIcon from '@patternfly/react-icons/dist/esm/icons/times-icon';
import { AlertContext } from './AlertContext';

export interface AlertActionCloseButtonProps extends ButtonProps {
  /** Additional classes added to the AlertActionCloseButton */
  className?: string;
  /** A callback for when the close button is clicked */
  onClose?: () => void;
  /** Aria Label for the Close button */
  'aria-label'?: string;
  /** Variant Label for the Close button */
  variantLabel?: string;
}

export const AlertActionCloseButton: React.FunctionComponent<AlertActionCloseButtonProps> = ({
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  className = '',
  onClose = () => undefined as any,
  'aria-label': ariaLabel = '',
  variantLabel,
  ...props
}: AlertActionCloseButtonProps) => (
  <AlertContext.Consumer>
    {({ title, variantLabel: alertVariantLabel }) => (
      <Button
        variant={ButtonVariant.plain}
        onClick={onClose}
        aria-label={ariaLabel === '' ? `Close ${variantLabel || alertVariantLabel} alert: ${title}` : ariaLabel}
        {...props}
      >
        <TimesIcon />
      </Button>
    )}
  </AlertContext.Consumer>
);
AlertActionCloseButton.displayName = 'AlertActionCloseButton';
