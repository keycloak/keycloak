import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Pagination/pagination';
import { css } from '@patternfly/react-styles';
import AngleLeftIcon from '@patternfly/react-icons/dist/esm/icons/angle-left-icon';
import AngleDoubleLeftIcon from '@patternfly/react-icons/dist/esm/icons/angle-double-left-icon';
import AngleRightIcon from '@patternfly/react-icons/dist/esm/icons/angle-right-icon';
import AngleDoubleRightIcon from '@patternfly/react-icons/dist/esm/icons/angle-double-right-icon';
import { Button, ButtonVariant } from '../Button';
import { pluralize } from '../../helpers';
import { KEY_CODES } from '../../helpers/constants';
export class Navigation extends React.Component {
    constructor(props) {
        super(props);
        this.handleNewPage = (_evt, newPage) => {
            const { perPage, onSetPage } = this.props;
            const startIdx = (newPage - 1) * perPage;
            const endIdx = newPage * perPage;
            return onSetPage(_evt, newPage, perPage, startIdx, endIdx);
        };
        this.state = { userInputPage: this.props.page };
    }
    static parseInteger(input, lastPage) {
        // eslint-disable-next-line radix
        let inputPage = Number.parseInt(input, 10);
        if (!Number.isNaN(inputPage)) {
            inputPage = inputPage > lastPage ? lastPage : inputPage;
            inputPage = inputPage < 1 ? 1 : inputPage;
        }
        return inputPage;
    }
    onChange(event, lastPage) {
        const inputPage = Navigation.parseInteger(event.target.value, lastPage);
        this.setState({ userInputPage: Number.isNaN(inputPage) ? event.target.value : inputPage });
    }
    onKeyDown(event, page, lastPage, onPageInput) {
        if (event.keyCode === KEY_CODES.ENTER) {
            const inputPage = Navigation.parseInteger(this.state.userInputPage, lastPage);
            onPageInput(event, Number.isNaN(inputPage) ? page : inputPage);
            this.handleNewPage(event, Number.isNaN(inputPage) ? page : inputPage);
        }
    }
    componentDidUpdate(lastState) {
        if (this.props.page !== lastState.page &&
            this.props.page <= this.props.lastPage &&
            this.state.userInputPage !== this.props.page) {
            this.setState({ userInputPage: this.props.page });
        }
    }
    render() {
        const _a = this.props, { page, 
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        perPage, 
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        onSetPage, isDisabled, itemCount, lastPage, firstPage, pagesTitle, pagesTitlePlural, toLastPage, toNextPage, toFirstPage, toPreviousPage, currPage, paginationTitle, ofWord, onNextClick, onPreviousClick, onFirstClick, onLastClick, onPageInput, className, isCompact } = _a, props = __rest(_a, ["page", "perPage", "onSetPage", "isDisabled", "itemCount", "lastPage", "firstPage", "pagesTitle", "pagesTitlePlural", "toLastPage", "toNextPage", "toFirstPage", "toPreviousPage", "currPage", "paginationTitle", "ofWord", "onNextClick", "onPreviousClick", "onFirstClick", "onLastClick", "onPageInput", "className", "isCompact"]);
        const { userInputPage } = this.state;
        return (React.createElement("nav", Object.assign({ className: css(styles.paginationNav, className), "aria-label": paginationTitle }, props),
            !isCompact && (React.createElement("div", { className: css(styles.paginationNavControl, styles.modifiers.first) },
                React.createElement(Button, { variant: ButtonVariant.plain, isDisabled: isDisabled || page === firstPage || page === 0, "aria-label": toFirstPage, "data-action": "first", onClick: event => {
                        onFirstClick(event, 1);
                        this.handleNewPage(event, 1);
                        this.setState({ userInputPage: 1 });
                    } },
                    React.createElement(AngleDoubleLeftIcon, null)))),
            React.createElement("div", { className: styles.paginationNavControl },
                React.createElement(Button, { variant: ButtonVariant.plain, isDisabled: isDisabled || page === firstPage || page === 0, "data-action": "previous", onClick: event => {
                        const newPage = page - 1 >= 1 ? page - 1 : 1;
                        onPreviousClick(event, newPage);
                        this.handleNewPage(event, newPage);
                        this.setState({ userInputPage: newPage });
                    }, "aria-label": toPreviousPage },
                    React.createElement(AngleLeftIcon, null))),
            !isCompact && (React.createElement("div", { className: styles.paginationNavPageSelect },
                React.createElement("input", { className: css(styles.formControl), "aria-label": currPage, type: "number", disabled: isDisabled || (itemCount && page === firstPage && page === lastPage && itemCount >= 0) || page === 0, min: lastPage <= 0 && firstPage <= 0 ? 0 : 1, max: lastPage, value: userInputPage, onKeyDown: event => this.onKeyDown(event, page, lastPage, onPageInput), onChange: event => this.onChange(event, lastPage) }),
                (itemCount || itemCount === 0) && (React.createElement("span", { "aria-hidden": "true" },
                    ofWord,
                    " ",
                    pagesTitle ? pluralize(lastPage, pagesTitle, pagesTitlePlural) : lastPage)))),
            React.createElement("div", { className: styles.paginationNavControl },
                React.createElement(Button, { variant: ButtonVariant.plain, isDisabled: isDisabled || page === lastPage, "aria-label": toNextPage, "data-action": "next", onClick: event => {
                        const newPage = page + 1 <= lastPage ? page + 1 : lastPage;
                        onNextClick(event, newPage);
                        this.handleNewPage(event, newPage);
                        this.setState({ userInputPage: newPage });
                    } },
                    React.createElement(AngleRightIcon, null))),
            !isCompact && (React.createElement("div", { className: css(styles.paginationNavControl, styles.modifiers.last) },
                React.createElement(Button, { variant: ButtonVariant.plain, isDisabled: isDisabled || page === lastPage, "aria-label": toLastPage, "data-action": "last", onClick: event => {
                        onLastClick(event, lastPage);
                        this.handleNewPage(event, lastPage);
                        this.setState({ userInputPage: lastPage });
                    } },
                    React.createElement(AngleDoubleRightIcon, null))))));
    }
}
Navigation.displayName = 'Navigation';
Navigation.defaultProps = {
    className: '',
    isDisabled: false,
    isCompact: false,
    lastPage: 0,
    firstPage: 0,
    pagesTitle: '',
    pagesTitlePlural: '',
    toLastPage: 'Go to last page',
    toNextPage: 'Go to next page',
    toFirstPage: 'Go to first page',
    toPreviousPage: 'Go to previous page',
    currPage: 'Current page',
    paginationTitle: 'Pagination',
    ofWord: 'of',
    onNextClick: () => undefined,
    onPreviousClick: () => undefined,
    onFirstClick: () => undefined,
    onLastClick: () => undefined,
    onPageInput: () => undefined
};
//# sourceMappingURL=Navigation.js.map