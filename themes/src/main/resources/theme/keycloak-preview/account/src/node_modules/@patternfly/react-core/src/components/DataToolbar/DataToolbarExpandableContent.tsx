import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/DataToolbar/data-toolbar';
import { css, getModifier } from '@patternfly/react-styles';

import { RefObject } from 'react';
import { DataToolbarGroup } from './DataToolbarGroup';
import { DataToolbarItem } from './DataToolbarItem';
import { Button } from '../../components/Button';
import { DataToolbarContext } from './DataToolbarUtils';
import { PickOptional } from '../../helpers/typeUtils';

export interface DataToolbarExpandableContentProps extends React.HTMLProps<HTMLDivElement> {
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

export class DataToolbarExpandableContent extends React.Component<DataToolbarExpandableContentProps> {
  static contextType: any = DataToolbarContext;
  static defaultProps: PickOptional<DataToolbarExpandableContentProps> = {
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
    const { numberOfFilters } = this.context;

    const clearChipGroups = () => {
      clearAllFilters();
    };

    return (
      <div className={css(styles.dataToolbarExpandableContent, className)} ref={expandableContentRef} {...props}>
        <DataToolbarGroup />
        {numberOfFilters > 0 && (
          <DataToolbarGroup className={getModifier(styles, 'chip-container')}>
            <DataToolbarGroup ref={chipContainerRef} />
            {showClearFiltersButton && (
              <DataToolbarItem className={css(getModifier(styles, 'clear'))}>
                <Button variant="link" onClick={clearChipGroups} isInline>
                  {clearFiltersButtonText}
                </Button>
              </DataToolbarItem>
            )}
          </DataToolbarGroup>
        )}
      </div>
    );
  }
}
