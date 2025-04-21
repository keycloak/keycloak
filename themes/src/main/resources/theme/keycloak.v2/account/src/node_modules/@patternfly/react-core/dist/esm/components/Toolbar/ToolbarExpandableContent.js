import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Toolbar/toolbar';
import { css } from '@patternfly/react-styles';
import { ToolbarGroup } from './ToolbarGroup';
import { ToolbarItem } from './ToolbarItem';
import { Button } from '../Button';
import { ToolbarContext } from './ToolbarUtils';
export class ToolbarExpandableContent extends React.Component {
    render() {
        const _a = this.props, { className, expandableContentRef, chipContainerRef, 
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        isExpanded, clearAllFilters, clearFiltersButtonText, showClearFiltersButton } = _a, props = __rest(_a, ["className", "expandableContentRef", "chipContainerRef", "isExpanded", "clearAllFilters", "clearFiltersButtonText", "showClearFiltersButton"]);
        const { numberOfFilters, customChipGroupContent } = this.context;
        const clearChipGroups = () => {
            clearAllFilters();
        };
        return (React.createElement("div", Object.assign({ className: css(styles.toolbarExpandableContent, className), ref: expandableContentRef }, props),
            React.createElement(ToolbarGroup, null),
            numberOfFilters > 0 && (React.createElement(ToolbarGroup, { className: styles.modifiers.chipContainer },
                React.createElement(ToolbarGroup, { ref: chipContainerRef }),
                showClearFiltersButton && !customChipGroupContent && (React.createElement(ToolbarItem, null,
                    React.createElement(Button, { variant: "link", onClick: clearChipGroups, isInline: true }, clearFiltersButtonText))),
                customChipGroupContent && customChipGroupContent))));
    }
}
ToolbarExpandableContent.displayName = 'ToolbarExpandableContent';
ToolbarExpandableContent.contextType = ToolbarContext;
ToolbarExpandableContent.defaultProps = {
    isExpanded: false,
    clearFiltersButtonText: 'Clear all filters'
};
//# sourceMappingURL=ToolbarExpandableContent.js.map