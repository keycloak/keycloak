import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/OptionsMenu/options-menu';
import { css } from '@patternfly/react-styles';
import { DropdownItem, DropdownDirection, DropdownWithContext, DropdownContext } from '../Dropdown';
import CheckIcon from '@patternfly/react-icons/dist/esm/icons/check-icon';
import { OptionsToggle } from './OptionsToggle';
import { ToggleTemplate } from './ToggleTemplate';
export class PaginationOptionsMenu extends React.Component {
    constructor(props) {
        super(props);
        this.parentRef = React.createRef();
        this.onToggle = (isOpen) => {
            this.setState({ isOpen });
        };
        this.onSelect = () => {
            this.setState((prevState) => ({ isOpen: !prevState.isOpen }));
        };
        this.handleNewPerPage = (_evt, newPerPage) => {
            const { page, onPerPageSelect, itemCount, defaultToFullPage } = this.props;
            let newPage = page;
            while (Math.ceil(itemCount / newPerPage) < newPage) {
                newPage--;
            }
            if (defaultToFullPage) {
                if (itemCount / newPerPage !== newPage) {
                    while (newPage > 1 && itemCount - newPerPage * newPage < 0) {
                        newPage--;
                    }
                }
            }
            const startIdx = (newPage - 1) * newPerPage;
            const endIdx = newPage * newPerPage;
            return onPerPageSelect(_evt, newPerPage, newPage, startIdx, endIdx);
        };
        this.renderItems = () => {
            const { perPageOptions, perPage, perPageSuffix } = this.props;
            return perPageOptions.map(({ value, title }) => (React.createElement(DropdownItem, { key: value, component: "button", "data-action": `per-page-${value}`, className: css(perPage === value && 'pf-m-selected'), onClick: event => this.handleNewPerPage(event, value) },
                title,
                ` ${perPageSuffix}`,
                perPage === value && (React.createElement("div", { className: css(styles.optionsMenuMenuItemIcon) },
                    React.createElement(CheckIcon, null))))));
        };
        this.state = {
            isOpen: false
        };
    }
    render() {
        const { widgetId, isDisabled, itemsPerPageTitle, dropDirection, optionsToggle, perPageOptions, toggleTemplate, firstIndex, lastIndex, itemCount, itemsTitle, ofWord, perPageComponent } = this.props;
        const { isOpen } = this.state;
        return (React.createElement(DropdownContext.Provider, { value: {
                id: widgetId,
                onSelect: this.onSelect,
                toggleIndicatorClass: perPageComponent === 'div' ? styles.optionsMenuToggleButtonIcon : styles.optionsMenuToggleIcon,
                toggleTextClass: styles.optionsMenuToggleText,
                menuClass: styles.optionsMenuMenu,
                itemClass: styles.optionsMenuMenuItem,
                toggleClass: ' ',
                baseClass: styles.optionsMenu,
                disabledClass: styles.modifiers.disabled,
                menuComponent: 'ul',
                baseComponent: 'div',
                ouiaComponentType: PaginationOptionsMenu.displayName
            } },
            React.createElement(DropdownWithContext, { direction: dropDirection, isOpen: isOpen, toggle: React.createElement(OptionsToggle, { optionsToggle: optionsToggle, itemsPerPageTitle: itemsPerPageTitle, showToggle: perPageOptions && perPageOptions.length > 0, onToggle: this.onToggle, isOpen: isOpen, widgetId: widgetId, firstIndex: firstIndex, lastIndex: lastIndex, itemCount: itemCount, itemsTitle: itemsTitle, ofWord: ofWord, toggleTemplate: toggleTemplate, parentRef: this.parentRef.current, isDisabled: isDisabled, perPageComponent: perPageComponent }), dropdownItems: this.renderItems(), isPlain: true })));
    }
}
PaginationOptionsMenu.displayName = 'PaginationOptionsMenu';
PaginationOptionsMenu.defaultProps = {
    className: '',
    widgetId: '',
    isDisabled: false,
    dropDirection: DropdownDirection.down,
    perPageOptions: [],
    itemsPerPageTitle: 'Items per page',
    perPageSuffix: 'per page',
    optionsToggle: '',
    ofWord: 'of',
    perPage: 0,
    firstIndex: 0,
    lastIndex: 0,
    defaultToFullPage: false,
    itemsTitle: 'items',
    toggleTemplate: ToggleTemplate,
    onPerPageSelect: () => null,
    perPageComponent: 'div'
};
//# sourceMappingURL=PaginationOptionsMenu.js.map