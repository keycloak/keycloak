import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Toolbar/toolbar';
import { css } from '@patternfly/react-styles';
import { canUseDOM } from '../../helpers/util';

import { RefObject } from 'react';
import { ToolbarItem } from './ToolbarItem';
import { Button } from '../Button';
import { ToolbarGroup } from './ToolbarGroup';
import { globalBreakpoints } from './ToolbarUtils';
import { PickOptional } from '../../helpers/typeUtils';

export interface ToolbarChipGroupContentProps extends React.HTMLProps<HTMLDivElement> {
  /** Classes applied to root element of the data toolbar content row */
  className?: string;
  /** Flag indicating if a data toolbar toggle group's expandable content is expanded */
  isExpanded?: boolean;
  /** Chip group content reference for passing to data toolbar children */
  chipGroupContentRef?: RefObject<any>;
  /** optional callback for clearing all filters in the toolbar */
  clearAllFilters?: () => void;
  /** Flag indicating that the clear all filters button should be visible */
  showClearFiltersButton: boolean;
  /** Text to display in the clear all filters button */
  clearFiltersButtonText?: string;
  /** Total number of filters currently being applied across all ToolbarFilter components */
  numberOfFilters: number;
  /** Text to display in the total number of applied filters ToolbarFilter */
  numberOfFiltersText?: (numberOfFilters: number) => string;
  /** The breakpoint at which the listed filters in chip groups are collapsed down to a summary */
  collapseListedFiltersBreakpoint?: 'all' | 'md' | 'lg' | 'xl' | '2xl';
  /** Custom additional content appended to the generated chips. To maintain spacing and styling, each node should be a ToolbarItem or ToolbarGroup. This property will remove the built in "Clear all filters" button. */
  customChipGroupContent?: React.ReactNode;
}

export class ToolbarChipGroupContent extends React.Component<ToolbarChipGroupContentProps> {
  static displayName = 'ToolbarChipGroupContent';
  static defaultProps: PickOptional<ToolbarChipGroupContentProps> = {
    clearFiltersButtonText: 'Clear all filters',
    collapseListedFiltersBreakpoint: 'lg',
    numberOfFiltersText: (numberOfFilters: number) => `${numberOfFilters} filters applied`
  };

  render() {
    const {
      className,
      isExpanded,
      chipGroupContentRef,
      clearAllFilters,
      showClearFiltersButton,
      clearFiltersButtonText,
      collapseListedFiltersBreakpoint,
      numberOfFilters,
      numberOfFiltersText,
      customChipGroupContent,
      ...props
    } = this.props;

    const clearChipGroups = () => {
      clearAllFilters();
    };

    let collapseListedFilters = false;
    if (collapseListedFiltersBreakpoint === 'all') {
      collapseListedFilters = true;
    } else if (canUseDOM) {
      collapseListedFilters =
        (canUseDOM ? window.innerWidth : 1200) < globalBreakpoints[collapseListedFiltersBreakpoint];
    }

    return (
      <div
        className={css(
          styles.toolbarContent,
          (numberOfFilters === 0 || isExpanded) && styles.modifiers.hidden,
          className
        )}
        {...((numberOfFilters === 0 || isExpanded) && { hidden: true })}
        ref={chipGroupContentRef}
        {...props}
      >
        <ToolbarGroup
          className={css(collapseListedFilters && styles.modifiers.hidden)}
          {...(collapseListedFilters && { hidden: true })}
          {...(collapseListedFilters && { 'aria-hidden': true })}
        />
        {collapseListedFilters && numberOfFilters > 0 && !isExpanded && (
          <ToolbarGroup>
            <ToolbarItem>{numberOfFiltersText(numberOfFilters)}</ToolbarItem>
          </ToolbarGroup>
        )}
        {showClearFiltersButton && !isExpanded && !customChipGroupContent && (
          <ToolbarItem>
            <Button variant="link" onClick={clearChipGroups} isInline>
              {clearFiltersButtonText}
            </Button>
          </ToolbarItem>
        )}
        {customChipGroupContent && customChipGroupContent}
      </div>
    );
  }
}
