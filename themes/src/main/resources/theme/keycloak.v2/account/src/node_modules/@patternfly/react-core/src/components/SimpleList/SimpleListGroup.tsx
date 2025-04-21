import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/SimpleList/simple-list';

export interface SimpleListGroupProps extends Omit<React.HTMLProps<HTMLTableSectionElement>, 'title'> {
  /** Content rendered inside the SimpleList group */
  children?: React.ReactNode;
  /** Additional classes added to the SimpleList <ul> */
  className?: string;
  /** Additional classes added to the SimpleList group title */
  titleClassName?: string;
  /** Title of the SimpleList group */
  title?: React.ReactNode;
  /** ID of SimpleList group */
  id?: string;
}

export const SimpleListGroup: React.FunctionComponent<SimpleListGroupProps> = ({
  children = null,
  className = '',
  title = '',
  titleClassName = '',
  id = '',
  ...props
}: SimpleListGroupProps) => (
  <section className={css(styles.simpleListSection)} {...props}>
    <h2 id={id} className={css(styles.simpleListTitle, titleClassName)} aria-hidden="true">
      {title}
    </h2>
    <ul className={css(className)} aria-labelledby={id}>
      {children}
    </ul>
  </section>
);
SimpleListGroup.displayName = 'SimpleListGroup';
