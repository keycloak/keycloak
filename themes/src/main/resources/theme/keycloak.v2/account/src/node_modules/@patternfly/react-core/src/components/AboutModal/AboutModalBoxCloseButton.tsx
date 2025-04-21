import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/AboutModalBox/about-modal-box';
import { Button } from '../Button';
import TimesIcon from '@patternfly/react-icons/dist/esm/icons/times-icon';

export interface AboutModalBoxCloseButtonProps extends React.HTMLProps<HTMLDivElement> {
  /** additional classes added to the About Modal Close button  */
  className?: string;
  /** A callback for when the close button is clicked  */
  onClose?: () => void;
  /** Set close button aria label */
  'aria-label'?: string;
}

export const AboutModalBoxCloseButton: React.FunctionComponent<AboutModalBoxCloseButtonProps> = ({
  className = '',
  onClose = () => undefined as any,
  'aria-label': ariaLabel = 'Close Dialog',
  ...props
}: AboutModalBoxCloseButtonProps) => (
  <div className={css(styles.aboutModalBoxClose, className)} {...props}>
    <Button variant="plain" onClick={onClose} aria-label={ariaLabel}>
      <TimesIcon />
    </Button>
  </div>
);
AboutModalBoxCloseButton.displayName = 'AboutModalBoxCloseButton';
