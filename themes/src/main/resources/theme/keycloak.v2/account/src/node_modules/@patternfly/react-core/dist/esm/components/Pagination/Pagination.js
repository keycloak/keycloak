import { __rest } from "tslib";
import * as React from 'react';
import { ToggleTemplate } from './ToggleTemplate';
import styles from '@patternfly/react-styles/css/components/Pagination/pagination';
import { css } from '@patternfly/react-styles';
import { fillTemplate } from '../../helpers';
import { Navigation } from './Navigation';
import { PaginationOptionsMenu } from './PaginationOptionsMenu';
import { getOUIAProps, getDefaultOUIAId } from '../../helpers';
import widthChars from '@patternfly/react-tokens/dist/esm/c_pagination__nav_page_select_c_form_control_width_chars';
export var PaginationVariant;
(function (PaginationVariant) {
    PaginationVariant["top"] = "top";
    PaginationVariant["bottom"] = "bottom";
})(PaginationVariant || (PaginationVariant = {}));
const defaultPerPageOptions = [
    {
        title: '10',
        value: 10
    },
    {
        title: '20',
        value: 20
    },
    {
        title: '50',
        value: 50
    },
    {
        title: '100',
        value: 100
    }
];
const handleInputWidth = (lastPage, node) => {
    if (!node) {
        return;
    }
    const len = String(lastPage).length;
    if (len >= 3) {
        node.style.setProperty(widthChars.name, `${len}`);
    }
    else {
        node.style.setProperty(widthChars.name, '2');
    }
};
let paginationId = 0;
export class Pagination extends React.Component {
    constructor() {
        super(...arguments);
        this.paginationRef = React.createRef();
        this.state = {
            ouiaStateId: getDefaultOUIAId(Pagination.displayName, this.props.variant)
        };
    }
    getLastPage() {
        const { itemCount, perPage, page } = this.props;
        // when itemCount is not known let's set lastPage as page+1 as we don't know the total count
        return itemCount || itemCount === 0 ? Math.ceil(itemCount / perPage) || 0 : page + 1;
    }
    componentDidMount() {
        const node = this.paginationRef.current;
        handleInputWidth(this.getLastPage(), node);
    }
    componentDidUpdate(prevProps) {
        const node = this.paginationRef.current;
        if (prevProps.perPage !== this.props.perPage || prevProps.itemCount !== this.props.itemCount) {
            handleInputWidth(this.getLastPage(), node);
        }
    }
    render() {
        const _a = this.props, { children, className, variant, isDisabled, isCompact, isStatic, isSticky, perPage, titles, firstPage, page: propPage, offset, defaultToFullPage, itemCount, itemsStart, itemsEnd, perPageOptions, dropDirection: dropDirectionProp, widgetId, toggleTemplate, onSetPage, onPerPageSelect, onFirstClick, onPreviousClick, onNextClick, onPageInput, onLastClick, ouiaId, ouiaSafe, perPageComponent } = _a, props = __rest(_a, ["children", "className", "variant", "isDisabled", "isCompact", "isStatic", "isSticky", "perPage", "titles", "firstPage", "page", "offset", "defaultToFullPage", "itemCount", "itemsStart", "itemsEnd", "perPageOptions", "dropDirection", "widgetId", "toggleTemplate", "onSetPage", "onPerPageSelect", "onFirstClick", "onPreviousClick", "onNextClick", "onPageInput", "onLastClick", "ouiaId", "ouiaSafe", "perPageComponent"]);
        const dropDirection = dropDirectionProp || (variant === 'bottom' && !isStatic ? 'up' : 'down');
        let page = propPage;
        if (!page && offset) {
            page = Math.ceil(offset / perPage);
        }
        if (page === 0 && !itemCount) {
            page = 1;
        }
        const lastPage = this.getLastPage();
        let firstIndex = (page - 1) * perPage + 1;
        let lastIndex = page * perPage;
        if (itemCount || itemCount === 0) {
            firstIndex = itemCount <= 0 ? 0 : (page - 1) * perPage + 1;
            if (page < firstPage && itemCount > 0) {
                page = firstPage;
            }
            else if (page > lastPage) {
                page = lastPage;
            }
            if (itemCount >= 0) {
                lastIndex = page === lastPage || itemCount === 0 ? itemCount : page * perPage;
            }
        }
        const toggleTemplateProps = { firstIndex, lastIndex, itemCount, itemsTitle: titles.items, ofWord: titles.ofWord };
        return (React.createElement("div", Object.assign({ ref: this.paginationRef, className: css(styles.pagination, variant === PaginationVariant.bottom && styles.modifiers.bottom, isCompact && styles.modifiers.compact, isStatic && styles.modifiers.static, isSticky && styles.modifiers.sticky, className), id: `${widgetId}-${paginationId++}` }, getOUIAProps(Pagination.displayName, ouiaId !== undefined ? ouiaId : this.state.ouiaStateId, ouiaSafe), props),
            variant === PaginationVariant.top && (React.createElement("div", { className: css(styles.paginationTotalItems) },
                toggleTemplate && typeof toggleTemplate === 'string' && fillTemplate(toggleTemplate, toggleTemplateProps),
                toggleTemplate &&
                    typeof toggleTemplate !== 'string' &&
                    toggleTemplate(toggleTemplateProps),
                !toggleTemplate && (React.createElement(ToggleTemplate, { firstIndex: firstIndex, lastIndex: lastIndex, itemCount: itemCount, itemsTitle: titles.items, ofWord: titles.ofWord })))),
            React.createElement(PaginationOptionsMenu, { itemsPerPageTitle: titles.itemsPerPage, perPageSuffix: titles.perPageSuffix, itemsTitle: isCompact ? '' : titles.items, optionsToggle: titles.optionsToggle, perPageOptions: perPageOptions, firstIndex: itemsStart !== null ? itemsStart : firstIndex, lastIndex: itemsEnd !== null ? itemsEnd : lastIndex, ofWord: titles.ofWord, defaultToFullPage: defaultToFullPage, itemCount: itemCount, page: page, perPage: perPage, lastPage: lastPage, onPerPageSelect: onPerPageSelect, dropDirection: dropDirection, widgetId: widgetId, toggleTemplate: toggleTemplate, isDisabled: isDisabled, perPageComponent: perPageComponent }),
            React.createElement(Navigation, { pagesTitle: titles.page, pagesTitlePlural: titles.pages, toLastPage: titles.toLastPage, toPreviousPage: titles.toPreviousPage, toNextPage: titles.toNextPage, toFirstPage: titles.toFirstPage, currPage: titles.currPage, paginationTitle: titles.paginationTitle, ofWord: titles.ofWord, page: itemCount && itemCount <= 0 ? 0 : page, perPage: perPage, itemCount: itemCount, firstPage: itemsStart !== null ? itemsStart : 1, lastPage: lastPage, onSetPage: onSetPage, onFirstClick: onFirstClick, onPreviousClick: onPreviousClick, onNextClick: onNextClick, onLastClick: onLastClick, onPageInput: onPageInput, isDisabled: isDisabled, isCompact: isCompact }),
            children));
    }
}
Pagination.displayName = 'Pagination';
Pagination.defaultProps = {
    children: null,
    className: '',
    variant: PaginationVariant.top,
    isDisabled: false,
    isCompact: false,
    isSticky: false,
    perPage: defaultPerPageOptions[0].value,
    titles: {
        items: '',
        page: '',
        pages: '',
        itemsPerPage: 'Items per page',
        perPageSuffix: 'per page',
        toFirstPage: 'Go to first page',
        toPreviousPage: 'Go to previous page',
        toLastPage: 'Go to last page',
        toNextPage: 'Go to next page',
        optionsToggle: '',
        currPage: 'Current page',
        paginationTitle: 'Pagination',
        ofWord: 'of'
    },
    firstPage: 1,
    page: 0,
    offset: 0,
    defaultToFullPage: false,
    itemsStart: null,
    itemsEnd: null,
    perPageOptions: defaultPerPageOptions,
    widgetId: 'pagination-options-menu',
    onSetPage: () => undefined,
    onPerPageSelect: () => undefined,
    onFirstClick: () => undefined,
    onPreviousClick: () => undefined,
    onNextClick: () => undefined,
    onPageInput: () => undefined,
    onLastClick: () => undefined,
    ouiaSafe: true,
    perPageComponent: 'div'
};
//# sourceMappingURL=Pagination.js.map