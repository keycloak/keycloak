import { __rest } from "tslib";
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import { ToolbarItem } from './ToolbarItem';
import { ChipGroup } from '../ChipGroup';
import { Chip } from '../Chip';
import { ToolbarContentContext, ToolbarContext } from './ToolbarUtils';
export class ToolbarFilter extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            isMounted: false
        };
    }
    componentDidMount() {
        const { categoryName, chips } = this.props;
        this.context.updateNumberFilters(typeof categoryName !== 'string' && categoryName.hasOwnProperty('key')
            ? categoryName.key
            : categoryName.toString(), chips.length);
        this.setState({ isMounted: true });
    }
    componentDidUpdate() {
        const { categoryName, chips } = this.props;
        this.context.updateNumberFilters(typeof categoryName !== 'string' && categoryName.hasOwnProperty('key')
            ? categoryName.key
            : categoryName.toString(), chips.length);
    }
    render() {
        const _a = this.props, { children, chips, deleteChipGroup, deleteChip, chipGroupExpandedText, chipGroupCollapsedText, categoryName, showToolbarItem } = _a, props = __rest(_a, ["children", "chips", "deleteChipGroup", "deleteChip", "chipGroupExpandedText", "chipGroupCollapsedText", "categoryName", "showToolbarItem"]);
        const { isExpanded, chipGroupContentRef } = this.context;
        const categoryKey = typeof categoryName !== 'string' && categoryName.hasOwnProperty('key')
            ? categoryName.key
            : categoryName.toString();
        const chipGroup = chips.length ? (React.createElement(ToolbarItem, { variant: "chip-group" },
            React.createElement(ChipGroup, { key: categoryKey, categoryName: typeof categoryName === 'string' ? categoryName : categoryName.name, isClosable: deleteChipGroup !== undefined, onClick: () => deleteChipGroup(categoryName), collapsedText: chipGroupCollapsedText, expandedText: chipGroupExpandedText }, chips.map(chip => typeof chip === 'string' ? (React.createElement(Chip, { key: chip, onClick: () => deleteChip(categoryKey, chip) }, chip)) : (React.createElement(Chip, { key: chip.key, onClick: () => deleteChip(categoryKey, chip) }, chip.node)))))) : null;
        if (!isExpanded && this.state.isMounted) {
            return (React.createElement(React.Fragment, null,
                showToolbarItem && React.createElement(ToolbarItem, Object.assign({}, props), children),
                ReactDOM.createPortal(chipGroup, chipGroupContentRef.current.firstElementChild)));
        }
        return (React.createElement(ToolbarContentContext.Consumer, null, ({ chipContainerRef }) => (React.createElement(React.Fragment, null,
            showToolbarItem && React.createElement(ToolbarItem, Object.assign({}, props), children),
            chipContainerRef.current && ReactDOM.createPortal(chipGroup, chipContainerRef.current)))));
    }
}
ToolbarFilter.displayName = 'ToolbarFilter';
ToolbarFilter.contextType = ToolbarContext;
ToolbarFilter.defaultProps = {
    chips: [],
    showToolbarItem: true
};
//# sourceMappingURL=ToolbarFilter.js.map