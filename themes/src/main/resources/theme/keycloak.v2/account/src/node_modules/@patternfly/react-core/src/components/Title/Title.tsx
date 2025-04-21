import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Title/title';
import { useOUIAProps, OUIAProps } from '../../helpers';

export enum TitleSizes {
  md = 'md',
  lg = 'lg',
  xl = 'xl',
  '2xl' = '2xl',
  '3xl' = '3xl',
  '4xl' = '4xl'
}

enum headingLevelSizeMap {
  h1 = '2xl',
  h2 = 'xl',
  h3 = 'lg',
  h4 = 'md',
  h5 = 'md',
  h6 = 'md'
}

type Size = 'md' | 'lg' | 'xl' | '2xl' | '3xl' | '4xl';

export interface TitleProps extends Omit<React.HTMLProps<HTMLHeadingElement>, 'size' | 'className'>, OUIAProps {
  /** The size of the Title  */
  size?: Size;
  /** Content rendered inside the Title */
  children?: React.ReactNode;
  /** Additional classes added to the Title */
  className?: string;
  /** The heading level to use */
  headingLevel: 'h1' | 'h2' | 'h3' | 'h4' | 'h5' | 'h6';
}

export const Title: React.FunctionComponent<TitleProps> = ({
  className = '',
  children = '',
  headingLevel: HeadingLevel,
  size = headingLevelSizeMap[HeadingLevel],
  ouiaId,
  ouiaSafe = true,
  ...props
}: TitleProps) => {
  const ouiaProps = useOUIAProps(Title.displayName, ouiaId, ouiaSafe);
  return (
    <HeadingLevel
      {...ouiaProps}
      {...props}
      className={css(styles.title, size && styles.modifiers[size as Size], className)}
    >
      {children}
    </HeadingLevel>
  );
};
Title.displayName = 'Title';
