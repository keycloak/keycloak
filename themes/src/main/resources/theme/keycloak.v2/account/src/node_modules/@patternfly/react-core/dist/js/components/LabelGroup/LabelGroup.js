"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.LabelGroup = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const label_group_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/LabelGroup/label-group"));
const label_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Label/label"));
const react_styles_1 = require("@patternfly/react-styles");
const Button_1 = require("../Button");
const Label_1 = require("../Label");
const Tooltip_1 = require("../Tooltip");
const times_circle_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/times-circle-icon'));
const helpers_1 = require("../../helpers");
const GenerateId_1 = require("../../helpers/GenerateId/GenerateId");
class LabelGroup extends React.Component {
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
            React.createElement("span", { tabIndex: 0, ref: this.headingRef, className: react_styles_1.css(label_group_1.default.labelGroupLabel) },
                React.createElement("span", { "aria-hidden": "true", id: id }, categoryName)))) : (React.createElement("span", { ref: this.headingRef, className: react_styles_1.css(label_group_1.default.labelGroupLabel), "aria-hidden": "true", id: id }, categoryName));
    }
    render() {
        const _a = this.props, { categoryName, children, className, isClosable, isCompact, closeBtnAriaLabel, 'aria-label': ariaLabel, onClick, numLabels, expandedText, collapsedText, 
        /* eslint-disable @typescript-eslint/no-unused-vars */
        defaultIsOpen, tooltipPosition, isVertical, isEditable, hasEditableTextArea, editableTextAreaProps, addLabelControl } = _a, 
        /* eslint-enable @typescript-eslint/no-unused-vars */
        rest = tslib_1.__rest(_a, ["categoryName", "children", "className", "isClosable", "isCompact", "closeBtnAriaLabel", 'aria-label', "onClick", "numLabels", "expandedText", "collapsedText", "defaultIsOpen", "tooltipPosition", "isVertical", "isEditable", "hasEditableTextArea", "editableTextAreaProps", "addLabelControl"]);
        const { isOpen } = this.state;
        const numChildren = React.Children.count(children);
        const collapsedTextResult = helpers_1.fillTemplate(collapsedText, {
            remaining: React.Children.count(children) - numLabels
        });
        const renderLabelGroup = (id) => {
            const labelArray = !isOpen
                ? React.Children.toArray(children).slice(0, numLabels)
                : React.Children.toArray(children);
            const content = (React.createElement(React.Fragment, null,
                categoryName && this.renderLabel(id),
                React.createElement("ul", Object.assign({ className: react_styles_1.css(label_group_1.default.labelGroupList) }, (categoryName && { 'aria-labelledby': id }), (!categoryName && { 'aria-label': ariaLabel }), { role: "list" }, rest),
                    labelArray.map((child, i) => (React.createElement("li", { className: react_styles_1.css(label_group_1.default.labelGroupListItem), key: i }, child))),
                    numChildren > numLabels && (React.createElement("li", { className: react_styles_1.css(label_group_1.default.labelGroupListItem) },
                        React.createElement(Label_1.Label, { isOverflowLabel: true, onClick: this.toggleCollapse, className: react_styles_1.css(isCompact && label_1.default.modifiers.compact) }, isOpen ? expandedText : collapsedTextResult))),
                    addLabelControl && React.createElement("li", { className: react_styles_1.css(label_group_1.default.labelGroupListItem) }, addLabelControl),
                    isEditable && hasEditableTextArea && (React.createElement("li", { className: react_styles_1.css(label_group_1.default.labelGroupListItem, label_group_1.default.modifiers.textarea) },
                        React.createElement("textarea", Object.assign({ className: react_styles_1.css(label_group_1.default.labelGroupTextarea), rows: 1, tabIndex: 0 }, editableTextAreaProps)))))));
            const close = (React.createElement("div", { className: react_styles_1.css(label_group_1.default.labelGroupClose) },
                React.createElement(Button_1.Button, { variant: "plain", "aria-label": closeBtnAriaLabel, onClick: onClick, id: `remove_group_${id}`, "aria-labelledby": `remove_group_${id} ${id}` },
                    React.createElement(times_circle_icon_1.default, { "aria-hidden": "true" }))));
            return (React.createElement("div", { className: react_styles_1.css(label_group_1.default.labelGroup, className, categoryName && label_group_1.default.modifiers.category, isVertical && label_group_1.default.modifiers.vertical, isEditable && label_group_1.default.modifiers.editable) },
                React.createElement("div", { className: react_styles_1.css(label_group_1.default.labelGroupMain) }, content),
                isClosable && close));
        };
        return numChildren === 0 && addLabelControl === undefined ? null : (React.createElement(GenerateId_1.GenerateId, null, randomId => renderLabelGroup(this.props.id || randomId)));
    }
}
exports.LabelGroup = LabelGroup;
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