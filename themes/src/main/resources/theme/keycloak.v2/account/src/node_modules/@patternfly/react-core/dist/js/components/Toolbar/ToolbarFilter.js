"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ToolbarFilter = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const ReactDOM = tslib_1.__importStar(require("react-dom"));
const ToolbarItem_1 = require("./ToolbarItem");
const ChipGroup_1 = require("../ChipGroup");
const Chip_1 = require("../Chip");
const ToolbarUtils_1 = require("./ToolbarUtils");
class ToolbarFilter extends React.Component {
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
        const _a = this.props, { children, chips, deleteChipGroup, deleteChip, chipGroupExpandedText, chipGroupCollapsedText, categoryName, showToolbarItem } = _a, props = tslib_1.__rest(_a, ["children", "chips", "deleteChipGroup", "deleteChip", "chipGroupExpandedText", "chipGroupCollapsedText", "categoryName", "showToolbarItem"]);
        const { isExpanded, chipGroupContentRef } = this.context;
        const categoryKey = typeof categoryName !== 'string' && categoryName.hasOwnProperty('key')
            ? categoryName.key
            : categoryName.toString();
        const chipGroup = chips.length ? (React.createElement(ToolbarItem_1.ToolbarItem, { variant: "chip-group" },
            React.createElement(ChipGroup_1.ChipGroup, { key: categoryKey, categoryName: typeof categoryName === 'string' ? categoryName : categoryName.name, isClosable: deleteChipGroup !== undefined, onClick: () => deleteChipGroup(categoryName), collapsedText: chipGroupCollapsedText, expandedText: chipGroupExpandedText }, chips.map(chip => typeof chip === 'string' ? (React.createElement(Chip_1.Chip, { key: chip, onClick: () => deleteChip(categoryKey, chip) }, chip)) : (React.createElement(Chip_1.Chip, { key: chip.key, onClick: () => deleteChip(categoryKey, chip) }, chip.node)))))) : null;
        if (!isExpanded && this.state.isMounted) {
            return (React.createElement(React.Fragment, null,
                showToolbarItem && React.createElement(ToolbarItem_1.ToolbarItem, Object.assign({}, props), children),
                ReactDOM.createPortal(chipGroup, chipGroupContentRef.current.firstElementChild)));
        }
        return (React.createElement(ToolbarUtils_1.ToolbarContentContext.Consumer, null, ({ chipContainerRef }) => (React.createElement(React.Fragment, null,
            showToolbarItem && React.createElement(ToolbarItem_1.ToolbarItem, Object.assign({}, props), children),
            chipContainerRef.current && ReactDOM.createPortal(chipGroup, chipContainerRef.current)))));
    }
}
exports.ToolbarFilter = ToolbarFilter;
ToolbarFilter.displayName = 'ToolbarFilter';
ToolbarFilter.contextType = ToolbarUtils_1.ToolbarContext;
ToolbarFilter.defaultProps = {
    chips: [],
    showToolbarItem: true
};
//# sourceMappingURL=ToolbarFilter.js.map