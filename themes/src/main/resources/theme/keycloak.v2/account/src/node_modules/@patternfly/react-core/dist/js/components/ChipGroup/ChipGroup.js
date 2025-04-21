"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ChipGroup = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const chip_group_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/ChipGroup/chip-group"));
const react_styles_1 = require("@patternfly/react-styles");
const Button_1 = require("../Button");
const Chip_1 = require("../Chip");
const Tooltip_1 = require("../Tooltip");
const times_circle_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/times-circle-icon'));
const helpers_1 = require("../../helpers");
const GenerateId_1 = require("../../helpers/GenerateId/GenerateId");
const helpers_2 = require("../../helpers");
class ChipGroup extends React.Component {
    constructor(props) {
        super(props);
        this.headingRef = React.createRef();
        this.toggleCollapse = () => {
            this.setState(prevState => ({
                isOpen: !prevState.isOpen,
                isTooltipVisible: Boolean(this.headingRef.current && this.headingRef.current.offsetWidth < this.headingRef.current.scrollWidth)
            }));
        };
        this.state = {
            isOpen: this.props.defaultIsOpen,
            isTooltipVisible: false
        };
    }
    componentDidMount() {
        this.setState({
            isTooltipVisible: Boolean(this.headingRef.current && this.headingRef.current.offsetWidth < this.headingRef.current.scrollWidth)
        });
    }
    renderLabel(id) {
        const { categoryName, tooltipPosition } = this.props;
        const { isTooltipVisible } = this.state;
        return isTooltipVisible ? (React.createElement(Tooltip_1.Tooltip, { position: tooltipPosition, content: categoryName },
            React.createElement("span", { tabIndex: 0, ref: this.headingRef, className: react_styles_1.css(chip_group_1.default.chipGroupLabel) },
                React.createElement("span", { id: id }, categoryName)))) : (React.createElement("span", { ref: this.headingRef, className: react_styles_1.css(chip_group_1.default.chipGroupLabel), id: id }, categoryName));
    }
    render() {
        const _a = this.props, { categoryName, children, className, isClosable, closeBtnAriaLabel, 'aria-label': ariaLabel, onClick, onOverflowChipClick, numChips, expandedText, collapsedText, ouiaId, 
        /* eslint-disable @typescript-eslint/no-unused-vars */
        defaultIsOpen, tooltipPosition } = _a, 
        /* eslint-enable @typescript-eslint/no-unused-vars */
        rest = tslib_1.__rest(_a, ["categoryName", "children", "className", "isClosable", "closeBtnAriaLabel", 'aria-label', "onClick", "onOverflowChipClick", "numChips", "expandedText", "collapsedText", "ouiaId", "defaultIsOpen", "tooltipPosition"]);
        const { isOpen } = this.state;
        const numChildren = React.Children.count(children);
        const collapsedTextResult = helpers_1.fillTemplate(collapsedText, {
            remaining: React.Children.count(children) - numChips
        });
        const renderChipGroup = (id) => {
            const chipArray = !isOpen
                ? React.Children.toArray(children).slice(0, numChips)
                : React.Children.toArray(children);
            return (React.createElement("div", Object.assign({ className: react_styles_1.css(chip_group_1.default.chipGroup, className, categoryName && chip_group_1.default.modifiers.category), role: "group" }, (categoryName && { 'aria-labelledby': id }), (!categoryName && { 'aria-label': ariaLabel }), helpers_2.getOUIAProps(ChipGroup.displayName, ouiaId)),
                React.createElement("div", { className: react_styles_1.css(chip_group_1.default.chipGroupMain) },
                    categoryName && this.renderLabel(id),
                    React.createElement("ul", Object.assign({ className: react_styles_1.css(chip_group_1.default.chipGroupList) }, (categoryName && { 'aria-labelledby': id }), (!categoryName && { 'aria-label': ariaLabel }), { role: "list" }, rest),
                        chipArray.map((child, i) => (React.createElement("li", { className: react_styles_1.css(chip_group_1.default.chipGroupListItem), key: i }, child))),
                        numChildren > numChips && (React.createElement("li", { className: react_styles_1.css(chip_group_1.default.chipGroupListItem) },
                            React.createElement(Chip_1.Chip, { isOverflowChip: true, onClick: event => {
                                    this.toggleCollapse();
                                    onOverflowChipClick(event);
                                }, component: "button" }, isOpen ? expandedText : collapsedTextResult))))),
                isClosable && (React.createElement("div", { className: react_styles_1.css(chip_group_1.default.chipGroupClose) },
                    React.createElement(Button_1.Button, { variant: "plain", "aria-label": closeBtnAriaLabel, onClick: onClick, id: `remove_group_${id}`, "aria-labelledby": `remove_group_${id} ${id}`, ouiaId: ouiaId || closeBtnAriaLabel },
                        React.createElement(times_circle_icon_1.default, { "aria-hidden": "true" }))))));
        };
        return numChildren === 0 ? null : React.createElement(GenerateId_1.GenerateId, null, randomId => renderChipGroup(this.props.id || randomId));
    }
}
exports.ChipGroup = ChipGroup;
ChipGroup.displayName = 'ChipGroup';
ChipGroup.defaultProps = {
    expandedText: 'Show Less',
    collapsedText: '${remaining} more',
    categoryName: '',
    defaultIsOpen: false,
    numChips: 3,
    isClosable: false,
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    onClick: (_e) => undefined,
    onOverflowChipClick: (_e) => undefined,
    closeBtnAriaLabel: 'Close chip group',
    tooltipPosition: 'top',
    'aria-label': 'Chip group category'
};
//# sourceMappingURL=ChipGroup.js.map