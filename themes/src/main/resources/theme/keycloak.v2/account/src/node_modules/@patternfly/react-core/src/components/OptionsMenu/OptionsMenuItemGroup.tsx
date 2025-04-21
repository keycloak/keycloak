import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/OptionsMenu/options-menu';
import { css } from '@patternfly/react-styles';
import { Divider } from '../Divider';

export interface OptionsMenuItemGroupProps extends React.HTMLProps<HTMLElement> {
  /** Content to be rendered in the options menu items component */
  children?: React.ReactNode;
  /** Classes applied to root element of the options menu items group */
  className?: string;
  /** Provides an accessible name for the options menu items group */
  'aria-label'?: string;
  /** Optional title for the options menu items group */
  groupTitle?: string | React.ReactNode;
  /** Flag indicating this options menu items group will be followed by a horizontal separator */
  hasSeparator?: boolean;
}

export const OptionsMenuItemGroup: React.FunctionComponent<OptionsMenuItemGroupProps> = ({
  className = '',
  'aria-label': ariaLabel = '',
  groupTitle = '',
  children = null,
  hasSeparator = false,
  ...props
}: OptionsMenuItemGroupProps) => (
  <section {...props} className={css(styles.optionsMenuGroup)}>
    {groupTitle && <h1 className={css(styles.optionsMenuGroupTitle)}>{groupTitle}</h1>}
    <ul className={className} aria-label={ariaLabel}>
      {children}
      {hasSeparator && <Divider component="li" role="separator" />}
    </ul>
  </section>
);
OptionsMenuItemGroup.displayName = 'OptionsMenuItemGroup';
