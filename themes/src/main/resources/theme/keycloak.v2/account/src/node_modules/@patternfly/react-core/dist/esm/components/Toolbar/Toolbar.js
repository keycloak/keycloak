import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Toolbar/toolbar';
import { GenerateId } from '../../helpers/GenerateId/GenerateId';
import { css } from '@patternfly/react-styles';
import { ToolbarContext } from './ToolbarUtils';
import { ToolbarChipGroupContent } from './ToolbarChipGroupContent';
import { formatBreakpointMods, canUseDOM } from '../../helpers/util';
import { getDefaultOUIAId, getOUIAProps } from '../../helpers';
import { PageContext } from '../Page/Page';
export class Toolbar extends React.Component {
    constructor() {
        super(...arguments);
        this.chipGroupContentRef = React.createRef();
        this.staticFilterInfo = {};
        this.state = {
            isManagedToggleExpanded: false,
            filterInfo: {},
            windowWidth: canUseDOM ? window.innerWidth : 1200,
            ouiaStateId: getDefaultOUIAId(Toolbar.displayName)
        };
        this.isToggleManaged = () => !(this.props.isExpanded || !!this.props.toggleIsExpanded);
        this.toggleIsExpanded = () => {
            this.setState(prevState => ({
                isManagedToggleExpanded: !prevState.isManagedToggleExpanded
            }));
        };
        this.closeExpandableContent = (e) => {
            if (e.target.innerWidth !== this.state.windowWidth) {
                this.setState(() => ({
                    isManagedToggleExpanded: false,
                    windowWidth: e.target.innerWidth
                }));
            }
        };
        this.updateNumberFilters = (categoryName, numberOfFilters) => {
            const filterInfoToUpdate = Object.assign({}, this.staticFilterInfo);
            if (!filterInfoToUpdate.hasOwnProperty(categoryName) || filterInfoToUpdate[categoryName] !== numberOfFilters) {
                filterInfoToUpdate[categoryName] = numberOfFilters;
                this.staticFilterInfo = filterInfoToUpdate;
                this.setState({ filterInfo: filterInfoToUpdate });
            }
        };
        this.getNumberOfFilters = () => Object.values(this.state.filterInfo).reduce((acc, cur) => acc + cur, 0);
        this.renderToolbar = (randomId) => {
            const _a = this.props, { clearAllFilters, clearFiltersButtonText, collapseListedFiltersBreakpoint, isExpanded: isExpandedProp, toggleIsExpanded, className, children, isFullHeight, isStatic, inset, usePageInsets, isSticky, ouiaId, numberOfFiltersText, customChipGroupContent } = _a, props = __rest(_a, ["clearAllFilters", "clearFiltersButtonText", "collapseListedFiltersBreakpoint", "isExpanded", "toggleIsExpanded", "className", "children", "isFullHeight", "isStatic", "inset", "usePageInsets", "isSticky", "ouiaId", "numberOfFiltersText", "customChipGroupContent"]);
            const { isManagedToggleExpanded } = this.state;
            const isToggleManaged = this.isToggleManaged();
            const isExpanded = isToggleManaged ? isManagedToggleExpanded : isExpandedProp;
            const numberOfFilters = this.getNumberOfFilters();
            const showClearFiltersButton = numberOfFilters > 0;
            return (React.createElement(PageContext.Consumer, null, ({ width, getBreakpoint }) => (React.createElement("div", Object.assign({ className: css(styles.toolbar, isFullHeight && styles.modifiers.fullHeight, isStatic && styles.modifiers.static, usePageInsets && styles.modifiers.pageInsets, isSticky && styles.modifiers.sticky, formatBreakpointMods(inset, styles, '', getBreakpoint(width)), className), id: randomId }, getOUIAProps(Toolbar.displayName, ouiaId !== undefined ? ouiaId : this.state.ouiaStateId), props),
                React.createElement(ToolbarContext.Provider, { value: {
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
                    } },
                    children,
                    React.createElement(ToolbarChipGroupContent, { isExpanded: isExpanded, chipGroupContentRef: this.chipGroupContentRef, clearAllFilters: clearAllFilters, showClearFiltersButton: showClearFiltersButton, clearFiltersButtonText: clearFiltersButtonText, numberOfFilters: numberOfFilters, numberOfFiltersText: numberOfFiltersText, collapseListedFiltersBreakpoint: collapseListedFiltersBreakpoint, customChipGroupContent: customChipGroupContent }))))));
        };
    }
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
    render() {
        return this.props.id ? (this.renderToolbar(this.props.id)) : (React.createElement(GenerateId, null, randomId => this.renderToolbar(randomId)));
    }
}
Toolbar.displayName = 'Toolbar';
//# sourceMappingURL=Toolbar.js.map