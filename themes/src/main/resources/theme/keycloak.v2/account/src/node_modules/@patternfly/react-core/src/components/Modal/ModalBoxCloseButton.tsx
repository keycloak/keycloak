import * as React from 'react';
import { Button } from '../Button';
import TimesIcon from '@patternfly/react-icons/dist/esm/icons/times-icon';
import { OUIAProps } from '../../helpers';

export interface ModalBoxCloseButtonProps extends OUIAProps {
  /** Additional classes added to the close button */
  className?: string;
  /** A callback for when the close button is clicked */
  onClose?: () => void;
}

export const ModalBoxCloseButton: React.FunctionComponent<ModalBoxCloseButtonProps> = ({
  className = '',
  onClose = () => undefined as any,
  ouiaId,
  ...props
}: ModalBoxCloseButtonProps) => (
  <Button
    className={className}
    variant="plain"
    onClick={onClose}
    aria-label="Close"
    {...(ouiaId && { ouiaId: `${ouiaId}-${ModalBoxCloseButton.displayName}` })}
    {...props}
  >
    <TimesIcon />
  </Button>
);
ModalBoxCloseButton.displayName = 'ModalBoxCloseButton';
