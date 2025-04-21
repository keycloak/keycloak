import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/LabelGroup/label-group';
import labelStyles from '@patternfly/react-styles/css/components/Label/label';
import { css } from '@patternfly/react-styles';
import { Button } from '../Button';
import { Label } from '../Label';
import { Tooltip } from '../Tooltip';
import TimesCircleIcon from '@patternfly/react-icons/dist/esm/icons/times-circle-icon';
import { fillTemplate } from '../../helpers';
import { GenerateId } from '../../helpers/GenerateId/GenerateId';
export class LabelGroup extends React.Component {
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
            React.createElement("span", { tabIndex: 0, ref: this.headingRef, className: css(styles.labelGroupLabel) },
                React.createElement("span", { "aria-hidden": "true", id: id }, categoryName)))) : (React.createElement("span", { ref: this.headingRef, className: css(styles.labelGroupLabel), "aria-hidden": "true", id: id }, categoryName));
    }
    render() {
        const _a = this.props, { categoryName, children, className, isClosable, isCompact, closeBtnAriaLabel, 'aria-label': ariaLabel, onClick, numLabels, expandedText, collapsedText, 
        /* eslint-disable @typescript-eslint/no-unused-vars */
        defaultIsOpen, tooltipPosition, isVertical, isEditable, hasEditableTextArea, editableTextAreaProps, addLabelControl } = _a, 
        /* eslint-enable @typescript-eslint/no-unused-vars */
        rest = __rest(_a, ["categoryName", "children", "className", "isClosable", "isCompact", "closeBtnAriaLabel", 'aria-label', "onClick", "numLabels", "expandedText", "collapsedText", "defaultIsOpen", "tooltipPosition", "isVertical", "isEditable", "hasEditableTextArea", "editableTextAreaProps", "addLabelControl"]);
        const { isOpen } = this.state;
        const numChildren = React.Children.count(children);
        const collapsedTextResult = fillTemplate(collapsedText, {
            remaining: React.Children.count(children) - numLabels
        });
        const renderLabelGroup = (id) => {
            const labelArray = !isOpen
                ? React.Children.toArray(children).slice(0, numLabels)
                : React.Children.toArray(children);
            const content = (React.createElement(React.Fragment, null,
                categoryName && this.renderLabel(id),
                React.createElement("ul", Object.assign({ className: css(styles.labelGroupList) }, (categoryName && { 'aria-labelledby': id }), (!categoryName && { 'aria-label': ariaLabel }), { role: "list" }, rest),
                    labelArray.map((child, i) => (React.createElement("li", { className: css(styles.labelGroupListItem), key: i }, child))),
                    numChildren > numLabels && (React.createElement("li", { className: css(styles.labelGroupListItem) },
                        React.createElement(Label, { isOverflowLabel: true, onClick: this.toggleCollapse, className: css(isCompact && labelStyles.modifiers.compact) }, isOpen ? expandedText : collapsedTextResult))),
                    addLabelControl && React.createElement("li", { className: css(styles.labelGroupListItem) }, addLabelControl),
                    isEditable && hasEditableTextArea && (React.createElement("li", { className: css(styles.labelGroupListItem, styles.modifiers.textarea) },
                        React.createElement("textarea", Object.assign({ className: css(styles.labelGroupTextarea), rows: 1, tabIndex: 0 }, editableTextAreaProps)))))));
            const close = (React.createElement("div", { className: css(styles.labelGroupClose) },
                React.createElement(Button, { variant: "plain", "aria-label": closeBtnAriaLabel, onClick: onClick, id: `remove_group_${id}`, "aria-labelledby": `remove_group_${id} ${id}` },
                    React.createElement(TimesCircleIcon, { "aria-hidden": "true" }))));
            return (React.createElement("div", { className: css(styles.labelGroup, className, categoryName && styles.modifiers.category, isVertical && styles.modifiers.vertical, isEditable && styles.modifiers.editable) },
                React.createElement("div", { className: css(styles.labelGroupMain) }, content),
                isClosable && close));
        };
        return numChildren === 0 && addLabelControl === undefined ? null : (React.createElement(GenerateId, null, randomId => renderLabelGroup(this.props.id || randomId)));
    }
}
LabelGroup.displayName = 'LabelGroup';
LabelGroup.defaultProps = {
    expandedText: 'Show Less',
    collapsedText: '${remaining} more',
    categoryName: '',
    defaultIsOpen: false,
    numLabels: 3,
    isClosable: false,
    isCompact: false,
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    onClick: (_e) => undefined,
    closeBtnAriaLabel: 'Close label group',
    tooltipPosition: 'top',
    'aria-label': 'Label group category',
    isVertical: false,
    isEditable: false,
    hasEditableTextArea: false
};
//# sourceMappingURL=LabelGroup.js.map