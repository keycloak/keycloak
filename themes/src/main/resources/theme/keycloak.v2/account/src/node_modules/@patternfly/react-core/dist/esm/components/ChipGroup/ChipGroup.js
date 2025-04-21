import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/ChipGroup/chip-group';
import { css } from '@patternfly/react-styles';
import { Button } from '../Button';
import { Chip } from '../Chip';
import { Tooltip } from '../Tooltip';
import TimesCircleIcon from '@patternfly/react-icons/dist/esm/icons/times-circle-icon';
import { fillTemplate } from '../../helpers';
import { GenerateId } from '../../helpers/GenerateId/GenerateId';
import { getOUIAProps } from '../../helpers';
export class ChipGroup extends React.Component {
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
        return isTooltipVisible ? (React.createElement(Tooltip, { position: tooltipPosition, content: categoryName },
            React.createElement("span", { tabIndex: 0, ref: this.headingRef, className: css(styles.chipGroupLabel) },
                React.createElement("span", { id: id }, categoryName)))) : (React.createElement("span", { ref: this.headingRef, className: css(styles.chipGroupLabel), id: id }, categoryName));
    }
    render() {
        const _a = this.props, { categoryName, children, className, isClosable, closeBtnAriaLabel, 'aria-label': ariaLabel, onClick, onOverflowChipClick, numChips, expandedText, collapsedText, ouiaId, 
        /* eslint-disable @typescript-eslint/no-unused-vars */
        defaultIsOpen, tooltipPosition } = _a, 
        /* eslint-enable @typescript-eslint/no-unused-vars */
        rest = __rest(_a, ["categoryName", "children", "className", "isClosable", "closeBtnAriaLabel", 'aria-label', "onClick", "onOverflowChipClick", "numChips", "expandedText", "collapsedText", "ouiaId", "defaultIsOpen", "tooltipPosition"]);
        const { isOpen } = this.state;
        const numChildren = React.Children.count(children);
        const collapsedTextResult = fillTemplate(collapsedText, {
            remaining: React.Children.count(children) - numChips
        });
        const renderChipGroup = (id) => {
            const chipArray = !isOpen
                ? React.Children.toArray(children).slice(0, numChips)
                : React.Children.toArray(children);
            return (React.createElement("div", Object.assign({ className: css(styles.chipGroup, className, categoryName && styles.modifiers.category), role: "group" }, (categoryName && { 'aria-labelledby': id }), (!categoryName && { 'aria-label': ariaLabel }), getOUIAProps(ChipGroup.displayName, ouiaId)),
                React.createElement("div", { className: css(styles.chipGroupMain) },
                    categoryName && this.renderLabel(id),
                    React.createElement("ul", Object.assign({ className: css(styles.chipGroupList) }, (categoryName && { 'aria-labelledby': id }), (!categoryName && { 'aria-label': ariaLabel }), { role: "list" }, rest),
                        chipArray.map((child, i) => (React.createElement("li", { className: css(styles.chipGroupListItem), key: i }, child))),
                        numChildren > numChips && (React.createElement("li", { className: css(styles.chipGroupListItem) },
                            React.createElement(Chip, { isOverflowChip: true, onClick: event => {
                                    this.toggleCollapse();
                                    onOverflowChipClick(event);
                                }, component: "button" }, isOpen ? expandedText : collapsedTextResult))))),
                isClosable && (React.createElement("div", { className: css(styles.chipGroupClose) },
                    React.createElement(Button, { variant: "plain", "aria-label": closeBtnAriaLabel, onClick: onClick, id: `remove_group_${id}`, "aria-labelledby": `remove_group_${id} ${id}`, ouiaId: ouiaId || closeBtnAriaLabel },
                        React.createElement(TimesCircleIcon, { "aria-hidden": "true" }))))));
        };
        return numChildren === 0 ? null : React.createElement(GenerateId, null, randomId => renderChipGroup(this.props.id || randomId));
    }
}
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