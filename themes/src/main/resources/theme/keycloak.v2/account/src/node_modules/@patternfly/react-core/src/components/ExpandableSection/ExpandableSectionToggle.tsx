import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/ExpandableSection/expandable-section';
import { css } from '@patternfly/react-styles';
import AngleRightIcon from '@patternfly/react-icons/dist/esm/icons/angle-right-icon';

export interface ExpandableSectionToggleProps extends React.HTMLProps<HTMLDivElement> {
  /** Content rendered inside the expandable toggle. */
  children?: React.ReactNode;
  /** Additional classes added to the expandable toggle. */
  className?: string;
  /** Flag indicating if the expandable section is expanded. */
  isExpanded?: boolean;
  /** Callback function to toggle the expandable content. */
  onToggle?: (isExpanded: boolean) => void;
  /** ID of the toggle's respective expandable section content. */
  contentId?: string;
  /** Direction the toggle arrow should point when the expandable section is expanded. */
  direction?: 'up' | 'down';
}

export const ExpandableSectionToggle: React.FunctionComponent<ExpandableSectionToggleProps> = ({
  children,
  className = '',
  isExpanded = false,
  onToggle,
  contentId,
  direction = 'down',
  ...props
}: ExpandableSectionToggleProps) => (
  <div
    {...props}
    className={css(
      styles.expandableSection,
      isExpanded && styles.modifiers.expanded,
      styles.modifiers.detached,
      className
    )}
  >
    <button
      className={css(styles.expandableSectionToggle)}
      type="button"
      aria-expanded={isExpanded}
      aria-controls={contentId}
      onClick={() => onToggle(!isExpanded)}
    >
      <span
        className={css(
          styles.expandableSectionToggleIcon,
          isExpanded && direction === 'up' && styles.modifiers.expandTop
        )}
      >
        <AngleRightIcon aria-hidden />
      </span>
      <span className={css(styles.expandableSectionToggleText)}>{children}</span>
    </button>
  </div>
);

ExpandableSectionToggle.displayName = 'ExpandableSectionToggle';
