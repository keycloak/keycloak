import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Nav/nav';
import { css } from '@patternfly/react-styles';
import { getUniqueId } from '../../helpers/util';

export interface NavGroupProps extends React.HTMLProps<HTMLDivElement> {
  /** Title shown for the group */
  title: string;
  /** Anything that can be rendered inside of the group */
  children?: React.ReactNode;
  /** Additional classes added to the container */
  className?: string;
  /** Identifier to use for the section aria label */
  id?: string;
}

export const NavGroup: React.FunctionComponent<NavGroupProps> = ({
  title,
  children = null,
  className = '',
  id = getUniqueId(),
  ...props
}: NavGroupProps) => (
  <section className={css(styles.navSection, className)} aria-labelledby={id} {...props}>
    <h2 className={css(styles.navSectionTitle)} id={id}>
      {title}
    </h2>
    <ul className={css(styles.navList)}>{children}</ul>
  </section>
);
