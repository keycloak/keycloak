import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/DescriptionList/description-list';
import { css } from '@patternfly/react-styles';

export interface DescriptionListDescriptionProps extends React.HTMLProps<HTMLElement> {
  /** Anything that can be rendered inside of list description */
  children: React.ReactNode;
  /** Additional classes added to the DescriptionListDescription */
  className?: string;
}

export const DescriptionListDescription: React.FunctionComponent<DescriptionListDescriptionProps> = ({
  children = null,
  className,
  ...props
}: DescriptionListDescriptionProps) => (
  <dd className={css(styles.descriptionListDescription, className)} {...props}>
    <div className={'pf-c-description-list__text'}>{children}</div>
  </dd>
);
DescriptionListDescription.displayName = 'DescriptionListDescription';
