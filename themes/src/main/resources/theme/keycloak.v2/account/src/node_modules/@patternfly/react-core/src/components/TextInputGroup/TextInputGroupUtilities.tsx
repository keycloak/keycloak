import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/TextInputGroup/text-input-group';
import { css } from '@patternfly/react-styles';

export interface TextInputGroupUtilitiesProps extends React.HTMLProps<HTMLDivElement> {
  /** Content rendered inside the text input group utilities div */
  children?: React.ReactNode;
  /** Additional classes applied to the text input group utilities container */
  className?: string;
}

export const TextInputGroupUtilities: React.FunctionComponent<TextInputGroupUtilitiesProps> = ({
  children,
  className,
  ...props
}: TextInputGroupUtilitiesProps) => (
  <div className={css(styles.textInputGroupUtilities, className)} {...props}>
    {children}
  </div>
);

TextInputGroupUtilities.displayName = 'TextInputGroupUtilities';
