import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Toolbar/toolbar';
import { css } from '@patternfly/react-styles';

import { RefObject } from 'react';
import { ToolbarGroup } from './ToolbarGroup';
import { ToolbarItem } from './ToolbarItem';
import { Button } from '../Button';
import { ToolbarContext } from './ToolbarUtils';
import { PickOptional } from '../../helpers/typeUtils';

export interface ToolbarExpandableContentProps extends React.HTMLProps<HTMLDivElement> {
  /** Classes added to the root element of the data toolbar expandable content */
  className?: string;
  /** Flag indicating the expandable content is expanded */
  isExpanded?: boolean;
  /** Expandable content reference for passing to data toolbar children */
  expandableContentRef?: RefObject<HTMLDivElement>;
  /** Chip container reference for passing to data toolbar children */
  chipContainerRef?: RefObject<any>;
  /** optional callback for clearing all filters in the toolbar */
  clearAllFilters?: () => void;
  /** Text to display in the clear all filters button */
  clearFiltersButtonText?: string;
  /** Flag indicating that the clear all filters button should be visible */
  showClearFiltersButton: boolean;
}

export class ToolbarExpandableContent extends React.Component<ToolbarExpandableContentProps> {
  static displayName = 'ToolbarExpandableContent';
  static contextType = ToolbarContext;
  context!: React.ContextType<typeof ToolbarContext>;
  static defaultProps: PickOptional<ToolbarExpandableContentProps> = {
    isExpanded: false,
    clearFiltersButtonText: 'Clear all filters'
  };

  render() {
    const {
      className,
      expandableContentRef,
      chipContainerRef,
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      isExpanded,
      clearAllFilters,
      clearFiltersButtonText,
      showClearFiltersButton,
      ...props
    } = this.props;
    const { numberOfFilters, customChipGroupContent } = this.context;

    const clearChipGroups = () => {
      clearAllFilters();
    };

    return (
      <div className={css(styles.toolbarExpandableContent, className)} ref={expandableContentRef} {...props}>
        <ToolbarGroup />
        {numberOfFilters > 0 && (
          <ToolbarGroup className={styles.modifiers.chipContainer}>
            <ToolbarGroup ref={chipContainerRef} />
            {showClearFiltersButton && !customChipGroupContent && (
              <ToolbarItem>
                <Button variant="link" onClick={clearChipGroups} isInline>
                  {clearFiltersButtonText}
                </Button>
              </ToolbarItem>
            )}
            {customChipGroupContent && customChipGroupContent}
          </ToolbarGroup>
        )}
      </div>
    );
  }
}
