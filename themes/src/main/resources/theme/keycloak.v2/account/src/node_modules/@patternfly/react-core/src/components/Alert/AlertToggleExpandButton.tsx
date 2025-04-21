import * as React from 'react';
import { Button, ButtonProps, ButtonVariant } from '../Button';
import { AlertContext } from './AlertContext';
import AngleRightIcon from '@patternfly/react-icons/dist/esm/icons/angle-right-icon';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Alert/alert';

export interface AlertToggleExpandButtonProps extends ButtonProps {
  /** Aria label for the toggle button */
  'aria-label'?: string;
  /** A callback for when the toggle button is clicked */
  onToggleExpand?: () => void;
  /** Flag to indicate if the content is expanded */
  isExpanded?: boolean;
  /** Variant label for the toggle button */
  variantLabel?: string;
}

export const AlertToggleExpandButton: React.FunctionComponent<AlertToggleExpandButtonProps> = ({
  'aria-label': ariaLabel,
  variantLabel,
  onToggleExpand,
  isExpanded,
  ...props
}: AlertToggleExpandButtonProps) => {
  const { title, variantLabel: alertVariantLabel } = React.useContext(AlertContext);
  return (
    <Button
      variant={ButtonVariant.plain}
      onClick={onToggleExpand}
      aria-expanded={isExpanded}
      aria-label={ariaLabel === '' ? `Toggle ${variantLabel || alertVariantLabel} alert: ${title}` : ariaLabel}
      {...props}
    >
      <span className={css(styles.alertToggleIcon)}>
        <AngleRightIcon aria-hidden="true" />
      </span>
    </Button>
  );
};
AlertToggleExpandButton.displayName = 'AlertToggleExpandButton';
