import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Toolbar/toolbar';
import { css } from '@patternfly/react-styles';
import { ToolbarContentContext, ToolbarContext } from './ToolbarUtils';
import { formatBreakpointMods } from '../../helpers/util';
import { ToolbarExpandableContent } from './ToolbarExpandableContent';
import { PageContext } from '../Page/Page';

export interface ToolbarContentProps extends React.HTMLProps<HTMLDivElement> {
  /** Classes applied to root element of the data toolbar content row */
  className?: string;
  /** Visibility at various breakpoints. */
  visibility?: {
    default?: 'hidden' | 'visible';
    md?: 'hidden' | 'visible';
    lg?: 'hidden' | 'visible';
    xl?: 'hidden' | 'visible';
    '2xl'?: 'hidden' | 'visible';
  };
  /** @deprecated prop misspelled */
  visiblity?: {
    default?: 'hidden' | 'visible';
    md?: 'hidden' | 'visible';
    lg?: 'hidden' | 'visible';
    xl?: 'hidden' | 'visible';
    '2xl'?: 'hidden' | 'visible';
  };
  /** Alignment at various breakpoints. */
  alignment?: {
    default?: 'alignRight' | 'alignLeft';
    md?: 'alignRight' | 'alignLeft';
    lg?: 'alignRight' | 'alignLeft';
    xl?: 'alignRight' | 'alignLeft';
    '2xl'?: 'alignRight' | 'alignLeft';
  };
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
  /** Id of the parent Toolbar component */
  toolbarId?: string;
}

export class ToolbarContent extends React.Component<ToolbarContentProps> {
  static displayName = 'ToolbarContent';
  private expandableContentRef = React.createRef<HTMLDivElement>();
  private chipContainerRef = React.createRef<HTMLDivElement>();
  private static currentId = 0;

  static defaultProps: ToolbarContentProps = {
    isExpanded: false,
    showClearFiltersButton: false
  };

  render() {
    const {
      className,
      children,
      isExpanded,
      toolbarId,
      visibility,
      visiblity,
      alignment,
      clearAllFilters,
      showClearFiltersButton,
      clearFiltersButtonText,
      ...props
    } = this.props;

    if (visiblity !== undefined) {
      // eslint-disable-next-line no-console
      console.warn(
        'The ToolbarContent visiblity prop has been deprecated. ' +
          'Please use the correctly spelled visibility prop instead.'
      );
    }

    return (
      <PageContext.Consumer>
        {({ width, getBreakpoint }) => (
          <div
            className={css(
              styles.toolbarContent,
              formatBreakpointMods(visibility || visiblity, styles, '', getBreakpoint(width)),
              formatBreakpointMods(alignment, styles, '', getBreakpoint(width)),
              className
            )}
            {...props}
          >
            <ToolbarContext.Consumer>
              {({
                clearAllFilters: clearAllFiltersContext,
                clearFiltersButtonText: clearFiltersButtonContext,
                showClearFiltersButton: showClearFiltersButtonContext,
                toolbarId: toolbarIdContext
              }) => {
                const expandableContentId = `${toolbarId ||
                  toolbarIdContext}-expandable-content-${ToolbarContent.currentId++}`;
                return (
                  <ToolbarContentContext.Provider
                    value={{
                      expandableContentRef: this.expandableContentRef,
                      expandableContentId,
                      chipContainerRef: this.chipContainerRef
                    }}
                  >
                    <div className={css(styles.toolbarContentSection)}>{children}</div>
                    <ToolbarExpandableContent
                      id={expandableContentId}
                      isExpanded={isExpanded}
                      expandableContentRef={this.expandableContentRef}
                      chipContainerRef={this.chipContainerRef}
                      clearAllFilters={clearAllFilters || clearAllFiltersContext}
                      showClearFiltersButton={showClearFiltersButton || showClearFiltersButtonContext}
                      clearFiltersButtonText={clearFiltersButtonText || clearFiltersButtonContext}
                    />
                  </ToolbarContentContext.Provider>
                );
              }}
            </ToolbarContext.Consumer>
          </div>
        )}
      </PageContext.Consumer>
    );
  }
}
