import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Content/content';
import { useOUIAProps, OUIAProps } from '../../helpers';

export enum TextVariants {
  h1 = 'h1',
  h2 = 'h2',
  h3 = 'h3',
  h4 = 'h4',
  h5 = 'h5',
  h6 = 'h6',
  p = 'p',
  a = 'a',
  small = 'small',
  blockquote = 'blockquote',
  pre = 'pre'
}

export interface TextProps extends React.HTMLProps<HTMLElement>, OUIAProps {
  /** The text component */
  component?: 'h1' | 'h2' | 'h3' | 'h4' | 'h5' | 'h6' | 'p' | 'a' | 'small' | 'blockquote' | 'pre';
  /** Content rendered within the Text */
  children?: React.ReactNode;
  /** Additional classes added to the Text */
  className?: string;
  /** Flag to indicate the link has visited styles applied if the browser determines the link has been visited */
  isVisitedLink?: boolean;
}

export const Text: React.FunctionComponent<TextProps> = ({
  children = null,
  className = '',
  component = TextVariants.p,
  isVisitedLink = false,
  ouiaId,
  ouiaSafe = true,
  ...props
}: TextProps) => {
  const Component: any = component;
  const ouiaProps = useOUIAProps(Text.displayName, ouiaId, ouiaSafe);

  return (
    <Component
      {...ouiaProps}
      {...props}
      data-pf-content
      className={css(isVisitedLink && component === TextVariants.a && styles.modifiers.visited, className)}
    >
      {children}
    </Component>
  );
};
Text.displayName = 'Text';
