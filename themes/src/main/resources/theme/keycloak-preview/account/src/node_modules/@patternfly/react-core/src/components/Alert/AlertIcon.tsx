import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Alert/alert';
import CheckCircleIcon from '@patternfly/react-icons/dist/js/icons/check-circle-icon';
import ExclamationCircleIcon from '@patternfly/react-icons/dist/js/icons/exclamation-circle-icon';
import ExclamationTriangleIcon from '@patternfly/react-icons/dist/js/icons/exclamation-triangle-icon';
import InfoCircleIcon from '@patternfly/react-icons/dist/js/icons/info-circle-icon';
import BellIcon from '@patternfly/react-icons/dist/js/icons/bell-icon';

export const variantIcons = {
  success: CheckCircleIcon,
  danger: ExclamationCircleIcon,
  warning: ExclamationTriangleIcon,
  info: InfoCircleIcon,
  default: BellIcon
};

export interface AlertIconProps extends React.HTMLProps<HTMLDivElement> {
  /** variant */
  variant: 'success' | 'danger' | 'warning' | 'info' | 'default';
  /** className */
  className?: string;
}

export const AlertIcon = ({ variant, className = '', ...props }: AlertIconProps) => {
  const Icon = variantIcons[variant];
  return (
    <div {...props} className={css(styles.alertIcon, className)}>
      <Icon />
    </div>
  );
};
