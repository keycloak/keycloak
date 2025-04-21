import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Nav/nav';
import { css } from '@patternfly/react-styles';
import { getUniqueId } from '../../helpers/util';

export interface NavGroupProps extends React.HTMLProps<HTMLDivElement> {
  /** Title shown for the group */
  title?: string;
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
}: NavGroupProps) => {
  if (!title && !props['aria-label']) {
    // eslint-disable-next-line no-console
    console.warn("For accessibility reasons an aria-label should be specified on nav groups if a title isn't");
  }

  const labelledBy = title ? id : undefined;

  return (
    <section className={css(styles.navSection, className)} aria-labelledby={labelledBy} {...props}>
      {title && (
        <h2 className={css(styles.navSectionTitle)} id={id}>
          {title}
        </h2>
      )}
      <ul className={css(styles.navList, className)}>{children}</ul>
    </section>
  );
};
NavGroup.displayName = 'NavGroup';
