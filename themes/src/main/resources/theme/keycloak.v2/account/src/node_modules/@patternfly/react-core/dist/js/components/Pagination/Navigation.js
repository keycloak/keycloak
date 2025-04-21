"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Navigation = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const pagination_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Pagination/pagination"));
const react_styles_1 = require("@patternfly/react-styles");
const angle_left_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/angle-left-icon'));
const angle_double_left_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/angle-double-left-icon'));
const angle_right_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/angle-right-icon'));
const angle_double_right_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/angle-double-right-icon'));
const Button_1 = require("../Button");
const helpers_1 = require("../../helpers");
const constants_1 = require("../../helpers/constants");
class Navigation extends React.Component {
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
        if (event.keyCode === constants_1.KEY_CODES.ENTER) {
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
        onSetPage, isDisabled, itemCount, lastPage, firstPage, pagesTitle, pagesTitlePlural, toLastPage, toNextPage, toFirstPage, toPreviousPage, currPage, paginationTitle, ofWord, onNextClick, onPreviousClick, onFirstClick, onLastClick, onPageInput, className, isCompact } = _a, props = tslib_1.__rest(_a, ["page", "perPage", "onSetPage", "isDisabled", "itemCount", "lastPage", "firstPage", "pagesTitle", "pagesTitlePlural", "toLastPage", "toNextPage", "toFirstPage", "toPreviousPage", "currPage", "paginationTitle", "ofWord", "onNextClick", "onPreviousClick", "onFirstClick", "onLastClick", "onPageInput", "className", "isCompact"]);
        const { userInputPage } = this.state;
        return (React.createElement("nav", Object.assign({ className: react_styles_1.css(pagination_1.default.paginationNav, className), "aria-label": paginationTitle }, props),
            !isCompact && (React.createElement("div", { className: react_styles_1.css(pagination_1.default.paginationNavControl, pagination_1.default.modifiers.first) },
                React.createElement(Button_1.Button, { variant: Button_1.ButtonVariant.plain, isDisabled: isDisabled || page === firstPage || page === 0, "aria-label": toFirstPage, "data-action": "first", onClick: event => {
                        onFirstClick(event, 1);
                        this.handleNewPage(event, 1);
                        this.setState({ userInputPage: 1 });
                    } },
                    React.createElement(angle_double_left_icon_1.default, null)))),
            React.createElement("div", { className: pagination_1.default.paginationNavControl },
                React.createElement(Button_1.Button, { variant: Button_1.ButtonVariant.plain, isDisabled: isDisabled || page === firstPage || page === 0, "data-action": "previous", onClick: event => {
                        const newPage = page - 1 >= 1 ? page - 1 : 1;
                        onPreviousClick(event, newPage);
                        this.handleNewPage(event, newPage);
                        this.setState({ userInputPage: newPage });
                    }, "aria-label": toPreviousPage },
                    React.createElement(angle_left_icon_1.default, null))),
            !isCompact && (React.createElement("div", { className: pagination_1.default.paginationNavPageSelect },
                React.createElement("input", { className: react_styles_1.css(pagination_1.default.formControl), "aria-label": currPage, type: "number", disabled: isDisabled || (itemCount && page === firstPage && page === lastPage && itemCount >= 0) || page === 0, min: lastPage <= 0 && firstPage <= 0 ? 0 : 1, max: lastPage, value: userInputPage, onKeyDown: event => this.onKeyDown(event, page, lastPage, onPageInput), onChange: event => this.onChange(event, lastPage) }),
                (itemCount || itemCount === 0) && (React.createElement("span", { "aria-hidden": "true" },
                    ofWord,
                    " ",
                    pagesTitle ? helpers_1.pluralize(lastPage, pagesTitle, pagesTitlePlural) : lastPage)))),
            React.createElement("div", { className: pagination_1.default.paginationNavControl },
                React.createElement(Button_1.Button, { variant: Button_1.ButtonVariant.plain, isDisabled: isDisabled || page === lastPage, "aria-label": toNextPage, "data-action": "next", onClick: event => {
                        const newPage = page + 1 <= lastPage ? page + 1 : lastPage;
                        onNextClick(event, newPage);
                        this.handleNewPage(event, newPage);
                        this.setState({ userInputPage: newPage });
                    } },
                    React.createElement(angle_right_icon_1.default, null))),
            !isCompact && (React.createElement("div", { className: react_styles_1.css(pagination_1.default.paginationNavControl, pagination_1.default.modifiers.last) },
                React.createElement(Button_1.Button, { variant: Button_1.ButtonVariant.plain, isDisabled: isDisabled || page === lastPage, "aria-label": toLastPage, "data-action": "last", onClick: event => {
                        onLastClick(event, lastPage);
                        this.handleNewPage(event, lastPage);
                        this.setState({ userInputPage: lastPage });
                    } },
                    React.createElement(angle_double_right_icon_1.default, null))))));
    }
}
exports.Navigation = Navigation;
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