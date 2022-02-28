import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Card/card';
import { css } from '@patternfly/react-styles';

export interface CardProps extends React.HTMLProps<HTMLElement> {
  /** Content rendered inside the Card */
  children?: React.ReactNode;
  /** Additional classes added to the Card */
  className?: string;
  /** Sets the base component to render. defaults to article */
  component?: keyof JSX.IntrinsicElements;
  /** Modifies the card to include hover styles on :hover */
  isHoverable?: boolean;
  /** Modifies the card to include compact styling */
  isCompact?: boolean;
  /** Modifies the card to include selectable styling */
  isSelectable?: boolean;
  /** Modifies the card to include selected styling */
  isSelected?: boolean;
}

export const Card: React.FunctionComponent<CardProps> = ({
  children = null,
  className = '',
  component = 'article',
  isHoverable = false,
  isCompact = false,
  isSelectable = false,
  isSelected = false,
  ...props
}: CardProps) => {
  const Component = component as any;
  return (
    <Component
      className={css(
        styles.card,
        isHoverable && styles.modifiers.hoverable,
        isCompact && styles.modifiers.compact,
        isSelectable && styles.modifiers.selectable,
        isSelected && isSelectable && styles.modifiers.selected,
        className
      )}
      tabIndex={isSelectable ? '0' : undefined}
      {...props}
    >
      {children}
    </Component>
  );
};
