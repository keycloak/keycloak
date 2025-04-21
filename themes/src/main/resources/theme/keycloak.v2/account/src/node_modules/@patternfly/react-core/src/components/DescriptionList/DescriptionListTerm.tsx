import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/DescriptionList/description-list';
import { css } from '@patternfly/react-styles';

export interface DescriptionListTermProps extends React.HTMLProps<HTMLElement> {
  /** Anything that can be rendered inside of list term */
  children: React.ReactNode;
  /** Icon that is rendered inside of list term to the left side of the children */
  icon?: React.ReactNode;
  /** Additional classes added to the DescriptionListTerm */
  className?: string;
}

export const DescriptionListTerm: React.FunctionComponent<DescriptionListTermProps> = ({
  children,
  className,
  icon,
  ...props
}: DescriptionListTermProps) => (
  <dt className={css(styles.descriptionListTerm, className)} {...props}>
    {icon ? <span className={css(styles.descriptionListTermIcon)}>{icon}</span> : null}
    <span className={css(styles.descriptionListText)}>{children}</span>
  </dt>
);
DescriptionListTerm.displayName = 'DescriptionListTerm';
