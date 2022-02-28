import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/DataToolbar/data-toolbar';
import { css } from '@patternfly/react-styles';

import { DataToolbarBreakpointMod, DataToolbarContentContext } from './DataToolbarUtils';
import { formatBreakpointMods } from '../../helpers/util';
import { DataToolbarExpandableContent } from './DataToolbarExpandableContent';

export interface DataToolbarContentProps extends React.HTMLProps<HTMLDivElement> {
  /** Classes applied to root element of the data toolbar content row */
  className?: string;
  /** An array of objects representing the various modifiers to apply to the content row at various breakpoints */
  breakpointMods?: DataToolbarBreakpointMod[];
  /** Content to be rendered as children of the content row */
  children?: React.ReactNode;
  /** Flag indicating if a data toolbar toggle group's expandable content is expanded */
  isExpanded?: boolean;
  /** Optional callback for clearing all filters in the toolbar */
  clearAllFilters?: () => void;
  /** Flag indicating that the clear all filters button should be visible */
  showClearFiltersButton?: boolean;
  /** Text to display in the clear all filters button */
  clearFiltersButtonText?: string;
  /** Id of the parent DataToolbar component */
  toolbarId?: string;
}

export class DataToolbarContent extends React.Component<DataToolbarContentProps> {
  private expandableContentRef = React.createRef<HTMLDivElement>();
  private chipContainerRef = React.createRef<HTMLDivElement>();
  private static currentId = 0;

  static defaultProps: DataToolbarContentProps = {
    isExpanded: false,
    breakpointMods: [] as DataToolbarBreakpointMod[],
    showClearFiltersButton: false
  };

  render() {
    const {
      className,
      children,
      isExpanded,
      toolbarId,
      breakpointMods,
      clearAllFilters,
      showClearFiltersButton,
      clearFiltersButtonText,
      ...props
    } = this.props;

    const expandableContentId = `${toolbarId}-expandable-content-${DataToolbarContent.currentId++}`;

    return (
      <div
        className={css(styles.dataToolbarContent, formatBreakpointMods(breakpointMods, styles), className)}
        {...props}
      >
        <DataToolbarContentContext.Provider
          value={{
            expandableContentRef: this.expandableContentRef,
            expandableContentId,
            chipContainerRef: this.chipContainerRef
          }}
        >
          <div className={css(styles.dataToolbarContentSection)}>{children}</div>
          <DataToolbarExpandableContent
            id={expandableContentId}
            isExpanded={isExpanded}
            expandableContentRef={this.expandableContentRef}
            chipContainerRef={this.chipContainerRef}
            clearAllFilters={clearAllFilters}
            showClearFiltersButton={showClearFiltersButton}
            clearFiltersButtonText={clearFiltersButtonText}
          />
        </DataToolbarContentContext.Provider>
      </div>
    );
  }
}
