import * as React from 'react';
import { Button } from '../Button';
import TimesIcon from '@patternfly/react-icons/dist/esm/icons/times-icon';
import { FindRefWrapper } from '../../helpers/Popper/FindRefWrapper';

export const PopoverCloseButton: React.FunctionComponent<PopoverCloseButtonProps> = ({
  onClose = () => undefined as void,
  ...props
}: PopoverCloseButtonProps) => {
  const [closeButtonElement, setCloseButtonElement] = React.useState(null);
  React.useEffect(() => {
    closeButtonElement && closeButtonElement.addEventListener('click', onClose, false);
    return () => {
      closeButtonElement && closeButtonElement.removeEventListener('click', onClose, false);
    };
  }, [closeButtonElement]);
  return (
    <FindRefWrapper onFoundRef={(foundRef: any) => setCloseButtonElement(foundRef)}>
      <Button variant="plain" aria-label {...props} style={{ pointerEvents: 'auto' }}>
        <TimesIcon />
      </Button>
    </FindRefWrapper>
  );
};
PopoverCloseButton.displayName = 'PopoverCloseButton';

export interface PopoverCloseButtonProps {
  /** PopoverCloseButton onClose function */
  onClose?: (event: any) => void;
  /** Aria label for the Close button */
  'aria-label': string;
}
