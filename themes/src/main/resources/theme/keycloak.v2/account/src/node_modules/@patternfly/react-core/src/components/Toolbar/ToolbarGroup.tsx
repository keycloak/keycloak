import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Toolbar/toolbar';
import { css } from '@patternfly/react-styles';
import { formatBreakpointMods, toCamel } from '../../helpers/util';
import { PageContext } from '../Page/Page';

export enum ToolbarGroupVariant {
  'filter-group' = 'filter-group',
  'icon-button-group' = 'icon-button-group',
  'button-group' = 'button-group'
}

export interface ToolbarGroupProps extends Omit<React.HTMLProps<HTMLDivElement>, 'ref'> {
  /** Classes applied to root element of the data toolbar group */
  className?: string;
  /** A type modifier which modifies spacing specifically depending on the type of group */
  variant?: ToolbarGroupVariant | 'filter-group' | 'icon-button-group' | 'button-group';
  /** Visibility at various breakpoints. */
  visibility?: {
    default?: 'hidden' | 'visible';
    md?: 'hidden' | 'visible';
    lg?: 'hidden' | 'visible';
    xl?: 'hidden' | 'visible';
    '2xl'?: 'hidden' | 'visible';
  };
  /** @deprecated prop misspelled */
  visiblity?: {
    default?: 'hidden' | 'visible';
    md?: 'hidden' | 'visible';
    lg?: 'hidden' | 'visible';
    xl?: 'hidden' | 'visible';
    '2xl'?: 'hidden' | 'visible';
  };
  /** Alignment at various breakpoints. */
  alignment?: {
    default?: 'alignRight' | 'alignLeft';
    md?: 'alignRight' | 'alignLeft';
    lg?: 'alignRight' | 'alignLeft';
    xl?: 'alignRight' | 'alignLeft';
    '2xl'?: 'alignRight' | 'alignLeft';
  };
  /** Spacers at various breakpoints. */
  spacer?: {
    default?: 'spacerNone' | 'spacerSm' | 'spacerMd' | 'spacerLg';
    md?: 'spacerNone' | 'spacerSm' | 'spacerMd' | 'spacerLg';
    lg?: 'spacerNone' | 'spacerSm' | 'spacerMd' | 'spacerLg';
    xl?: 'spacerNone' | 'spacerSm' | 'spacerMd' | 'spacerLg';
    '2xl'?: 'spacerNone' | 'spacerSm' | 'spacerMd' | 'spacerLg';
  };
  /** Space items at various breakpoints. */
  spaceItems?: {
    default?: 'spaceItemsNone' | 'spaceItemsSm' | 'spaceItemsMd' | 'spaceItemsLg';
    md?: 'spaceItemsNone' | 'spaceItemsSm' | 'spaceItemsMd' | 'spaceItemsLg';
    lg?: 'spaceItemsNone' | 'spaceItemsSm' | 'spaceItemsMd' | 'spaceItemsLg';
    xl?: 'spaceItemsNone' | 'spaceItemsSm' | 'spaceItemsMd' | 'spaceItemsLg';
    '2xl'?: 'spaceItemsNone' | 'spaceItemsSm' | 'spaceItemsMd' | 'spaceItemsLg';
  };
  /** Content to be rendered inside the data toolbar group */
  children?: React.ReactNode;
  /** Reference to pass to this group if it has .pf-m-chip-container modifier */
  innerRef?: React.RefObject<any>;
}

class ToolbarGroupWithRef extends React.Component<ToolbarGroupProps> {
  render() {
    const {
      visibility,
      visiblity,
      alignment,
      spacer,
      spaceItems,
      className,
      variant,
      children,
      innerRef,
      ...props
    } = this.props;

    if (visiblity !== undefined) {
      // eslint-disable-next-line no-console
      console.warn(
        'The ToolbarGroup visiblity prop has been deprecated. ' +
          'Please use the correctly spelled visibility prop instead.'
      );
    }

    return (
      <PageContext.Consumer>
        {({ width, getBreakpoint }) => (
          <div
            className={css(
              styles.toolbarGroup,
              variant && styles.modifiers[toCamel(variant) as 'filterGroup' | 'iconButtonGroup' | 'buttonGroup'],
              formatBreakpointMods(visibility || visiblity, styles, '', getBreakpoint(width)),
              formatBreakpointMods(alignment, styles, '', getBreakpoint(width)),
              formatBreakpointMods(spacer, styles, '', getBreakpoint(width)),
              formatBreakpointMods(spaceItems, styles, '', getBreakpoint(width)),
              className
            )}
            {...props}
            ref={innerRef}
          >
            {children}
          </div>
        )}
      </PageContext.Consumer>
    );
  }
}

export const ToolbarGroup = React.forwardRef((props: ToolbarGroupProps, ref: any) => (
  <ToolbarGroupWithRef {...props} innerRef={ref} />
));
