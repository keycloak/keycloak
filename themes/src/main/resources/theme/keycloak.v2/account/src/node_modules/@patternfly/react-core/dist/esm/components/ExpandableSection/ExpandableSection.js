import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/ExpandableSection/expandable-section';
import { css } from '@patternfly/react-styles';
import AngleRightIcon from '@patternfly/react-icons/dist/esm/icons/angle-right-icon';
export class ExpandableSection extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            isExpanded: props.isExpanded
        };
    }
    calculateToggleText(toggleText, toggleTextExpanded, toggleTextCollapsed, propOrStateIsExpanded) {
        if (propOrStateIsExpanded && toggleTextExpanded !== '') {
            return toggleTextExpanded;
        }
        if (!propOrStateIsExpanded && toggleTextCollapsed !== '') {
            return toggleTextCollapsed;
        }
        return toggleText;
    }
    render() {
        const _a = this.props, { onToggle: onToggleProp, 
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        isActive, className, toggleText, toggleTextExpanded, toggleTextCollapsed, toggleContent, children, isExpanded, isDetached, displaySize, isWidthLimited, isIndented, contentId } = _a, props = __rest(_a, ["onToggle", "isActive", "className", "toggleText", "toggleTextExpanded", "toggleTextCollapsed", "toggleContent", "children", "isExpanded", "isDetached", "displaySize", "isWidthLimited", "isIndented", "contentId"]);
        let onToggle = onToggleProp;
        let propOrStateIsExpanded = isExpanded;
        // uncontrolled
        if (isExpanded === undefined) {
            propOrStateIsExpanded = this.state.isExpanded;
            onToggle = isOpen => {
                this.setState({ isExpanded: isOpen }, () => onToggleProp(this.state.isExpanded));
            };
        }
        const computedToggleText = this.calculateToggleText(toggleText, toggleTextExpanded, toggleTextCollapsed, propOrStateIsExpanded);
        return (React.createElement("div", Object.assign({}, props, { className: css(styles.expandableSection, propOrStateIsExpanded && styles.modifiers.expanded, isActive && styles.modifiers.active, isDetached && styles.modifiers.detached, displaySize === 'large' && styles.modifiers.displayLg, isWidthLimited && styles.modifiers.limitWidth, isIndented && styles.modifiers.indented, className) }),
            !isDetached && (React.createElement("button", { className: css(styles.expandableSectionToggle), type: "button", "aria-expanded": propOrStateIsExpanded, onClick: () => onToggle(!propOrStateIsExpanded) },
                React.createElement("span", { className: css(styles.expandableSectionToggleIcon) },
                    React.createElement(AngleRightIcon, { "aria-hidden": true })),
                React.createElement("span", { className: css(styles.expandableSectionToggleText) }, toggleContent || computedToggleText))),
            React.createElement("div", { className: css(styles.expandableSectionContent), hidden: !propOrStateIsExpanded, id: contentId }, children)));
    }
}
ExpandableSection.displayName = 'ExpandableSection';
ExpandableSection.defaultProps = {
    className: '',
    toggleText: '',
    toggleTextExpanded: '',
    toggleTextCollapsed: '',
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    onToggle: (isExpanded) => undefined,
    isActive: false,
    isDetached: false,
    displaySize: 'default',
    isWidthLimited: false,
    isIndented: false,
    contentId: ''
};
//# sourceMappingURL=ExpandableSection.js.map