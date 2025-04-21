import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Toolbar/toolbar';
import { GenerateId } from '../../helpers/GenerateId/GenerateId';
import { css } from '@patternfly/react-styles';
import { ToolbarContext } from './ToolbarUtils';
import { ToolbarChipGroupContent } from './ToolbarChipGroupContent';
import { formatBreakpointMods, canUseDOM } from '../../helpers/util';
import { getDefaultOUIAId, getOUIAProps, OUIAProps } from '../../helpers';
import { PageContext } from '../Page/Page';

export interface ToolbarProps extends React.HTMLProps<HTMLDivElement>, OUIAProps {
  /** Optional callback for clearing all filters in the toolbar */
  clearAllFilters?: () => void;
  /** Text to display in the clear all filters button */
  clearFiltersButtonText?: string;
  /** Custom content appended to the filter generated chip group. To maintain spacing and styling, each node should be wrapped in a ToolbarItem or ToolbarGroup. This property will remove the default "Clear all filters" button. */
  customChipGroupContent?: React.ReactNode;
  /** The breakpoint at which the listed filters in chip groups are collapsed down to a summary */
  collapseListedFiltersBreakpoint?: 'all' | 'md' | 'lg' | 'xl' | '2xl';
  /** Flag indicating if a data toolbar toggle group's expandable content is expanded */
  isExpanded?: boolean;
  /** A callback for setting the isExpanded flag */
  toggleIsExpanded?: () => void;
  /** Classes applied to root element of the data toolbar */
  className?: string;
  /** Content to be rendered as rows in the data toolbar */
  children?: React.ReactNode;
  /** Id of the data toolbar */
  id?: string;
  /** Flag indicating the toolbar height should expand to the full height of the container */
  isFullHeight?: boolean;
  /** Flag indicating the toolbar is static */
  isStatic?: boolean;
  /** Flag indicating the toolbar should use the Page insets */
  usePageInsets?: boolean;
  /** Flag indicating the toolbar should stick to the top of its container */
  isSticky?: boolean;
  /** Insets at various breakpoints. */
  inset?: {
    default?: 'insetNone' | 'insetSm' | 'insetMd' | 'insetLg' | 'insetXl' | 'inset2xl';
    sm?: 'insetNone' | 'insetSm' | 'insetMd' | 'insetLg' | 'insetXl' | 'inset2xl';
    md?: 'insetNone' | 'insetSm' | 'insetMd' | 'insetLg' | 'insetXl' | 'inset2xl';
    lg?: 'insetNone' | 'insetSm' | 'insetMd' | 'insetLg' | 'insetXl' | 'inset2xl';
    xl?: 'insetNone' | 'insetSm' | 'insetMd' | 'insetLg' | 'insetXl' | 'inset2xl';
    '2xl'?: 'insetNone' | 'insetSm' | 'insetMd' | 'insetLg' | 'insetXl' | 'inset2xl';
  };
  /** Text to display in the total number of applied filters ToolbarFilter */
  numberOfFiltersText?: (numberOfFilters: number) => string;
}

export interface ToolbarState {
  /** Flag used if the user has opted NOT to manage the 'isExpanded' state of the toggle group.
   *  Indicates whether or not the toggle group is expanded. */
  isManagedToggleExpanded: boolean;
  /** Object managing information about how many chips are in each chip group */
  filterInfo: FilterInfo;
  /** Used to keep track of window width so we can collapse expanded content when window is resizing */
  windowWidth: number;
  ouiaStateId: string;
}

interface FilterInfo {
  [key: string]: number;
}

export class Toolbar extends React.Component<ToolbarProps, ToolbarState> {
  static displayName = 'Toolbar';
  chipGroupContentRef = React.createRef<HTMLDivElement>();
  staticFilterInfo = {};
  state = {
    isManagedToggleExpanded: false,
    filterInfo: {},
    windowWidth: canUseDOM ? window.innerWidth : 1200,
    ouiaStateId: getDefaultOUIAId(Toolbar.displayName)
  };

  isToggleManaged = () => !(this.props.isExpanded || !!this.props.toggleIsExpanded);

  toggleIsExpanded = () => {
    this.setState(prevState => ({
      isManagedToggleExpanded: !prevState.isManagedToggleExpanded
    }));
  };

  closeExpandableContent = (e: any) => {
    if (e.target.innerWidth !== this.state.windowWidth) {
      this.setState(() => ({
        isManagedToggleExpanded: false,
        windowWidth: e.target.innerWidth
      }));
    }
  };

  componentDidMount() {
    if (this.isToggleManaged() && canUseDOM) {
      window.addEventListener('resize', this.closeExpandableContent);
    }
  }

  componentWillUnmount() {
    if (this.isToggleManaged() && canUseDOM) {
      window.removeEventListener('resize', this.closeExpandableContent);
    }
  }

  updateNumberFilters = (categoryName: string, numberOfFilters: number) => {
    const filterInfoToUpdate: FilterInfo = { ...this.staticFilterInfo };
    if (!filterInfoToUpdate.hasOwnProperty(categoryName) || filterInfoToUpdate[categoryName] !== numberOfFilters) {
      filterInfoToUpdate[categoryName] = numberOfFilters;
      this.staticFilterInfo = filterInfoToUpdate;
      this.setState({ filterInfo: filterInfoToUpdate });
    }
  };

  getNumberOfFilters = () =>
    Object.values(this.state.filterInfo).reduce((acc: any, cur: any) => acc + cur, 0) as number;

  renderToolbar = (randomId: string) => {
    const {
      clearAllFilters,
      clearFiltersButtonText,
      collapseListedFiltersBreakpoint,
      isExpanded: isExpandedProp,
      toggleIsExpanded,
      className,
      children,
      isFullHeight,
      isStatic,
      inset,
      usePageInsets,
      isSticky,
      ouiaId,
      numberOfFiltersText,
      customChipGroupContent,
      ...props
    } = this.props;

    const { isManagedToggleExpanded } = this.state;

    const isToggleManaged = this.isToggleManaged();
    const isExpanded = isToggleManaged ? isManagedToggleExpanded : isExpandedProp;
    const numberOfFilters = this.getNumberOfFilters();
    const showClearFiltersButton = numberOfFilters > 0;

    return (
      <PageContext.Consumer>
        {({ width, getBreakpoint }) => (
          <div
            className={css(
              styles.toolbar,
              isFullHeight && styles.modifiers.fullHeight,
              isStatic && styles.modifiers.static,
              usePageInsets && styles.modifiers.pageInsets,
              isSticky && styles.modifiers.sticky,
              formatBreakpointMods(inset, styles, '', getBreakpoint(width)),
              className
            )}
            id={randomId}
            {...getOUIAProps(Toolbar.displayName, ouiaId !== undefined ? ouiaId : this.state.ouiaStateId)}
            {...props}
          >
            <ToolbarContext.Provider
              value={{
                isExpanded,
                toggleIsExpanded: isToggleManaged ? this.toggleIsExpanded : toggleIsExpanded,
                chipGroupContentRef: this.chipGroupContentRef,
                updateNumberFilters: this.updateNumberFilters,
                numberOfFilters,
                clearAllFilters,
                clearFiltersButtonText,
                showClearFiltersButton,
                toolbarId: randomId,
                customChipGroupContent
              }}
            >
              {children}
              <ToolbarChipGroupContent
                isExpanded={isExpanded}
                chipGroupContentRef={this.chipGroupContentRef}
                clearAllFilters={clearAllFilters}
                showClearFiltersButton={showClearFiltersButton}
                clearFiltersButtonText={clearFiltersButtonText}
                numberOfFilters={numberOfFilters}
                numberOfFiltersText={numberOfFiltersText}
                collapseListedFiltersBreakpoint={collapseListedFiltersBreakpoint}
                customChipGroupContent={customChipGroupContent}
              />
            </ToolbarContext.Provider>
          </div>
        )}
      </PageContext.Consumer>
    );
  };

  render() {
    return this.props.id ? (
      this.renderToolbar(this.props.id)
    ) : (
      <GenerateId>{randomId => this.renderToolbar(randomId)}</GenerateId>
    );
  }
}
