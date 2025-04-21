import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/layouts/Flex/flex';
import * as flexToken from '@patternfly/react-tokens/dist/esm/l_flex_item_Order';

import { formatBreakpointMods, setBreakpointCssVars } from '../../helpers/util';

export interface FlexProps extends React.HTMLProps<HTMLDivElement> {
  /** content rendered inside the Flex layout */
  children?: React.ReactNode;
  /** additional classes added to the Flex layout */
  className?: string;
  /** Spacers at various breakpoints */
  spacer?: {
    default?:
      | 'spacerNone'
      | 'spacerXs'
      | 'spacerSm'
      | 'spacerMd'
      | 'spacerLg'
      | 'spacerXl'
      | 'spacer2xl'
      | 'spacer3xl'
      | 'spacer4xl';
    sm?:
      | 'spacerNone'
      | 'spacerXs'
      | 'spacerSm'
      | 'spacerMd'
      | 'spacerLg'
      | 'spacerXl'
      | 'spacer2xl'
      | 'spacer3xl'
      | 'spacer4xl';
    md?:
      | 'spacerNone'
      | 'spacerXs'
      | 'spacerSm'
      | 'spacerMd'
      | 'spacerLg'
      | 'spacerXl'
      | 'spacer2xl'
      | 'spacer3xl'
      | 'spacer4xl';
    lg?:
      | 'spacerNone'
      | 'spacerXs'
      | 'spacerSm'
      | 'spacerMd'
      | 'spacerLg'
      | 'spacerXl'
      | 'spacer2xl'
      | 'spacer3xl'
      | 'spacer4xl';
    xl?:
      | 'spacerNone'
      | 'spacerXs'
      | 'spacerSm'
      | 'spacerMd'
      | 'spacerLg'
      | 'spacerXl'
      | 'spacer2xl'
      | 'spacer3xl'
      | 'spacer4xl';
    '2xl'?:
      | 'spacerNone'
      | 'spacerXs'
      | 'spacerSm'
      | 'spacerMd'
      | 'spacerLg'
      | 'spacerXl'
      | 'spacer2xl'
      | 'spacer3xl'
      | 'spacer4xl';
  };
  /** Space items at various breakpoints */
  spaceItems?: {
    default?:
      | 'spaceItemsNone'
      | 'spaceItemsXs'
      | 'spaceItemsSm'
      | 'spaceItemsMd'
      | 'spaceItemsLg'
      | 'spaceItemsXl'
      | 'spaceItems2xl'
      | 'spaceItems3xl'
      | 'spaceItems4xl';
    sm?:
      | 'spaceItemsNone'
      | 'spaceItemsXs'
      | 'spaceItemsSm'
      | 'spaceItemsMd'
      | 'spaceItemsLg'
      | 'spaceItemsXl'
      | 'spaceItems2xl'
      | 'spaceItems3xl'
      | 'spaceItems4xl';
    md?:
      | 'spaceItemsNone'
      | 'spaceItemsXs'
      | 'spaceItemsSm'
      | 'spaceItemsMd'
      | 'spaceItemsLg'
      | 'spaceItemsXl'
      | 'spaceItems2xl'
      | 'spaceItems3xl'
      | 'spaceItems4xl';
    lg?:
      | 'spaceItemsNone'
      | 'spaceItemsXs'
      | 'spaceItemsSm'
      | 'spaceItemsMd'
      | 'spaceItemsLg'
      | 'spaceItemsXl'
      | 'spaceItems2xl'
      | 'spaceItems3xl'
      | 'spaceItems4xl';
    xl?:
      | 'spaceItemsNone'
      | 'spaceItemsXs'
      | 'spaceItemsSm'
      | 'spaceItemsMd'
      | 'spaceItemsLg'
      | 'spaceItemsXl'
      | 'spaceItems2xl'
      | 'spaceItems3xl'
      | 'spaceItems4xl';
    '2xl'?:
      | 'spaceItemsNone'
      | 'spaceItemsXs'
      | 'spaceItemsSm'
      | 'spaceItemsMd'
      | 'spaceItemsLg'
      | 'spaceItemsXl'
      | 'spaceItems2xl'
      | 'spaceItems3xl'
      | 'spaceItems4xl';
  };
  /** Whether to add flex: grow at various breakpoints */
  grow?: {
    default?: 'grow';
    sm?: 'grow';
    md?: 'grow';
    lg?: 'grow';
    xl?: 'grow';
    '2xl'?: 'grow';
  };
  /** Whether to add flex: shrink at various breakpoints */
  shrink?: {
    default?: 'shrink';
    sm?: 'shrink';
    md?: 'shrink';
    lg?: 'shrink';
    xl?: 'shrink';
    '2xl'?: 'shrink';
  };
  /** Value to add for flex property at various breakpoints */
  flex?: {
    default?: 'flexDefault' | 'flexNone' | 'flex_1' | 'flex_2' | 'flex_3' | 'flex_4';
    sm?: 'flexDefault' | 'flexNone' | 'flex_1' | 'flex_2' | 'flex_3' | 'flex_4';
    md?: 'flexDefault' | 'flexNone' | 'flex_1' | 'flex_2' | 'flex_3' | 'flex_4';
    lg?: 'flexDefault' | 'flexNone' | 'flex_1' | 'flex_2' | 'flex_3' | 'flex_4';
    xl?: 'flexDefault' | 'flexNone' | 'flex_1' | 'flex_2' | 'flex_3' | 'flex_4';
    '2xl'?: 'flexDefault' | 'flexNone' | 'flex_1' | 'flex_2' | 'flex_3' | 'flex_4';
  };
  /** Value to add for flex-direction property at various breakpoints */
  direction?: {
    default?: 'column' | 'columnReverse' | 'row' | 'rowReverse';
    sm?: 'column' | 'columnReverse' | 'row' | 'rowReverse';
    md?: 'column' | 'columnReverse' | 'row' | 'rowReverse';
    lg?: 'column' | 'columnReverse' | 'row' | 'rowReverse';
    xl?: 'column' | 'columnReverse' | 'row' | 'rowReverse';
    '2xl'?: 'column' | 'columnReverse' | 'row' | 'rowReverse';
  };
  /** Value to add for align-items property at various breakpoints */
  alignItems?: {
    default?:
      | 'alignItemsFlexStart'
      | 'alignItemsFlexEnd'
      | 'alignItemsCenter'
      | 'alignItemsStretch'
      | 'alignItemsBaseline';
    sm?: 'alignItemsFlexStart' | 'alignItemsFlexEnd' | 'alignItemsCenter' | 'alignItemsStretch' | 'alignItemsBaseline';
    md?: 'alignItemsFlexStart' | 'alignItemsFlexEnd' | 'alignItemsCenter' | 'alignItemsStretch' | 'alignItemsBaseline';
    lg?: 'alignItemsFlexStart' | 'alignItemsFlexEnd' | 'alignItemsCenter' | 'alignItemsStretch' | 'alignItemsBaseline';
    xl?: 'alignItemsFlexStart' | 'alignItemsFlexEnd' | 'alignItemsCenter' | 'alignItemsStretch' | 'alignItemsBaseline';
    '2xl'?:
      | 'alignItemsFlexStart'
      | 'alignItemsFlexEnd'
      | 'alignItemsCenter'
      | 'alignItemsStretch'
      | 'alignItemsBaseline';
  };
  /** Value to add for align-content property at various breakpoints */
  alignContent?: {
    default?:
      | 'alignContentFlexStart'
      | 'alignContentFlexEnd'
      | 'alignContentCenter'
      | 'alignContentStretch'
      | 'alignContentSpaceBetween'
      | 'alignContentSpaceAround';
    sm?:
      | 'alignContentFlexStart'
      | 'alignContentFlexEnd'
      | 'alignContentCenter'
      | 'alignContentStretch'
      | 'alignContentSpaceBetween'
      | 'alignContentSpaceAround';
    md?:
      | 'alignContentFlexStart'
      | 'alignContentFlexEnd'
      | 'alignContentCenter'
      | 'alignContentStretch'
      | 'alignContentSpaceBetween'
      | 'alignContentSpaceAround';
    lg?:
      | 'alignContentFlexStart'
      | 'alignContentFlexEnd'
      | 'alignContentCenter'
      | 'alignContentStretch'
      | 'alignContentSpaceBetween'
      | 'alignContentSpaceAround';
    xl?:
      | 'alignContentFlexStart'
      | 'alignContentFlexEnd'
      | 'alignContentCenter'
      | 'alignContentStretch'
      | 'alignContentSpaceBetween'
      | 'alignContentSpaceAround';
    '2xl'?:
      | 'alignContentFlexStart'
      | 'alignContentFlexEnd'
      | 'alignContentCenter'
      | 'alignContentStretch'
      | 'alignContentSpaceBetween'
      | 'alignContentSpaceAround';
  };
  /** Value to add for align-self property at various breakpoints */
  alignSelf?: {
    default?: 'alignSelfFlexStart' | 'alignSelfFlexEnd' | 'alignSelfCenter' | 'alignSelfStretch' | 'alignSelfBaseline';
    sm?: 'alignSelfFlexStart' | 'alignSelfFlexEnd' | 'alignSelfCenter' | 'alignSelfStretch' | 'alignSelfBaseline';
    md?: 'alignSelfFlexStart' | 'alignSelfFlexEnd' | 'alignSelfCenter' | 'alignSelfStretch' | 'alignSelfBaseline';
    lg?: 'alignSelfFlexStart' | 'alignSelfFlexEnd' | 'alignSelfCenter' | 'alignSelfStretch' | 'alignSelfBaseline';
    xl?: 'alignSelfFlexStart' | 'alignSelfFlexEnd' | 'alignSelfCenter' | 'alignSelfStretch' | 'alignSelfBaseline';
    '2xl'?: 'alignSelfFlexStart' | 'alignSelfFlexEnd' | 'alignSelfCenter' | 'alignSelfStretch' | 'alignSelfBaseline';
  };
  /** Value to use for margin: auto at various breakpoints */
  align?: {
    default?: 'alignLeft' | 'alignRight';
    sm?: 'alignLeft' | 'alignRight';
    md?: 'alignLeft' | 'alignRight';
    lg?: 'alignLeft' | 'alignRight';
    xl?: 'alignLeft' | 'alignRight';
    '2xl'?: 'alignLeft' | 'alignRight';
  };
  /** Value to add for justify-content property at various breakpoints */
  justifyContent?: {
    default?:
      | 'justifyContentFlexStart'
      | 'justifyContentFlexEnd'
      | 'justifyContentCenter'
      | 'justifyContentSpaceBetween'
      | 'justifyContentSpaceAround'
      | 'justifyContentSpaceEvenly';
    sm?:
      | 'justifyContentFlexStart'
      | 'justifyContentFlexEnd'
      | 'justifyContentCenter'
      | 'justifyContentSpaceBetween'
      | 'justifyContentSpaceAround'
      | 'justifyContentSpaceEvenly';
    md?:
      | 'justifyContentFlexStart'
      | 'justifyContentFlexEnd'
      | 'justifyContentCenter'
      | 'justifyContentSpaceBetween'
      | 'justifyContentSpaceAround'
      | 'justifyContentSpaceEvenly';
    lg?:
      | 'justifyContentFlexStart'
      | 'justifyContentFlexEnd'
      | 'justifyContentCenter'
      | 'justifyContentSpaceBetween'
      | 'justifyContentSpaceAround'
      | 'justifyContentSpaceEvenly';
    xl?:
      | 'justifyContentFlexStart'
      | 'justifyContentFlexEnd'
      | 'justifyContentCenter'
      | 'justifyContentSpaceBetween'
      | 'justifyContentSpaceAround'
      | 'justifyContentSpaceEvenly';
    '2xl'?:
      | 'justifyContentFlexStart'
      | 'justifyContentFlexEnd'
      | 'justifyContentCenter'
      | 'justifyContentSpaceBetween'
      | 'justifyContentSpaceAround'
      | 'justifyContentSpaceEvenly';
  };
  /** Value to set to display property at various breakpoints */
  display?: {
    default?: 'inlineFlex';
    sm?: 'flex' | 'inlineFlex';
    md?: 'flex' | 'inlineFlex';
    lg?: 'flex' | 'inlineFlex';
    xl?: 'flex' | 'inlineFlex';
    '2xl'?: 'flex' | 'inlineFlex';
  };
  /** Whether to set width: 100% at various breakpoints */
  fullWidth?: {
    default?: 'fullWidth';
    sm?: 'fullWidth';
    md?: 'fullWidth';
    lg?: 'fullWidth';
    xl?: 'fullWidth';
    '2xl'?: 'fullWidth';
  };
  /** Value to set for flex-wrap property at various breakpoints */
  flexWrap?: {
    default?: 'wrap' | 'wrapReverse' | 'nowrap';
    sm?: 'wrap' | 'wrapReverse' | 'nowrap';
    md?: 'wrap' | 'wrapReverse' | 'nowrap';
    lg?: 'wrap' | 'wrapReverse' | 'nowrap';
    xl?: 'wrap' | 'wrapReverse' | 'nowrap';
    '2xl'?: 'wrap' | 'wrapReverse' | 'nowrap';
  };
  /** Modifies the flex layout element order property */
  order?: {
    default?: string;
    md?: string;
    lg?: string;
    xl?: string;
    '2xl'?: string;
  };
  /** Sets the base component to render. defaults to div */
  component?: React.ElementType<any> | React.ComponentType<any>;
}

export const Flex: React.FunctionComponent<FlexProps> = ({
  children = null,
  className = '',
  component = 'div',
  spacer,
  spaceItems,
  grow,
  shrink,
  flex,
  direction,
  alignItems,
  alignContent,
  alignSelf,
  align,
  justifyContent,
  display,
  fullWidth,
  flexWrap,
  order,
  style,
  ...props
}: FlexProps) => {
  const Component: any = component;

  return (
    <Component
      className={css(
        styles.flex,
        formatBreakpointMods(spacer, styles),
        formatBreakpointMods(spaceItems, styles),
        formatBreakpointMods(grow, styles),
        formatBreakpointMods(shrink, styles),
        formatBreakpointMods(flex, styles),
        formatBreakpointMods(direction, styles),
        formatBreakpointMods(alignItems, styles),
        formatBreakpointMods(alignContent, styles),
        formatBreakpointMods(alignSelf, styles),
        formatBreakpointMods(align, styles),
        formatBreakpointMods(justifyContent, styles),
        formatBreakpointMods(display, styles),
        formatBreakpointMods(fullWidth, styles),
        formatBreakpointMods(flexWrap, styles),
        className
      )}
      style={
        style || order ? { ...style, ...setBreakpointCssVars(order, flexToken.l_flex_item_Order.name) } : undefined
      }
      {...props}
    >
      {children}
    </Component>
  );
};
Flex.displayName = 'Flex';
