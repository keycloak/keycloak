import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Card/card';
import { css } from '@patternfly/react-styles';
import { CardContext } from './Card';

export interface CardExpandableContentProps extends React.HTMLProps<HTMLDivElement> {
  /** Content rendered inside the Card Body */
  children?: React.ReactNode;
  /** Additional classes added to the Card Body */
  className?: string;
}

export const CardExpandableContent: React.FunctionComponent<CardExpandableContentProps> = ({
  children = null,
  className = '',
  ...props
}: CardExpandableContentProps) => (
  <CardContext.Consumer>
    {({ isExpanded }) =>
      isExpanded ? (
        <div className={css(styles.cardExpandableContent, className)} {...props}>
          {children}
        </div>
      ) : null
    }
  </CardContext.Consumer>
);
CardExpandableContent.displayName = 'CardExpandableContent';
