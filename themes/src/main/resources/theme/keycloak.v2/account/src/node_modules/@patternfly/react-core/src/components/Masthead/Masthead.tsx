import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Masthead/masthead';
import { css } from '@patternfly/react-styles';
import { formatBreakpointMods } from '../../helpers/util';
import { PageContext } from '../Page/Page';

export interface MastheadProps extends React.DetailedHTMLProps<React.HTMLProps<HTMLDivElement>, HTMLDivElement> {
  /** Content rendered inside of the masthead */
  children?: React.ReactNode;
  /** Additional classes added to the masthead */
  className?: string;
  /** Background theme color of the masthead */
  backgroundColor?: 'dark' | 'light' | 'light200';
  /** Display type at various breakpoints */
  display?: {
    default?: 'inline' | 'stack';
    sm?: 'inline' | 'stack';
    md?: 'inline' | 'stack';
    lg?: 'inline' | 'stack';
    xl?: 'inline' | 'stack';
    '2xl'?: 'inline' | 'stack';
  };
  /** Insets at various breakpoints */
  inset?: {
    default?: 'insetNone' | 'insetXs' | 'insetSm' | 'insetMd' | 'insetLg' | 'insetXl' | 'inset2xl' | 'inset3xl';
    sm?: 'insetNone' | 'insetXs' | 'insetSm' | 'insetMd' | 'insetLg' | 'insetXl' | 'inset2xl' | 'inset3xl';
    md?: 'insetNone' | 'insetXs' | 'insetSm' | 'insetMd' | 'insetLg' | 'insetXl' | 'inset2xl' | 'inset3xl';
    lg?: 'insetNone' | 'insetXs' | 'insetSm' | 'insetMd' | 'insetLg' | 'insetXl' | 'inset2xl' | 'inset3xl';
    xl?: 'insetNone' | 'insetXs' | 'insetSm' | 'insetMd' | 'insetLg' | 'insetXl' | 'inset2xl' | 'inset3xl';
    '2xl'?: 'insetNone' | 'insetXs' | 'insetSm' | 'insetMd' | 'insetLg' | 'insetXl' | 'inset2xl' | 'inset3xl';
  };
}

export const Masthead: React.FunctionComponent<MastheadProps> = ({
  children,
  className,
  backgroundColor = 'dark',
  display = {
    md: 'inline'
  },
  inset,
  ...props
}: MastheadProps) => {
  const { width, getBreakpoint } = React.useContext(PageContext);
  return (
    <header
      className={css(
        styles.masthead,
        formatBreakpointMods(display, styles, 'display-', getBreakpoint(width)),
        formatBreakpointMods(inset, styles, '', getBreakpoint(width)),
        backgroundColor === 'light' && styles.modifiers.light,
        backgroundColor === 'light200' && styles.modifiers.light_200,
        className
      )}
      {...props}
    >
      {children}
    </header>
  );
};
Masthead.displayName = 'Masthead';
