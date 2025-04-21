"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.NavExpandable = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const nav_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Nav/nav"));
const accessibility_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/utilities/Accessibility/accessibility"));
const react_styles_1 = require("@patternfly/react-styles");
const angle_right_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/angle-right-icon'));
const util_1 = require("../../helpers/util");
const Nav_1 = require("./Nav");
const PageSidebar_1 = require("../Page/PageSidebar");
const helpers_1 = require("../../helpers");
class NavExpandable extends React.Component {
    constructor() {
        super(...arguments);
        this.id = this.props.id || util_1.getUniqueId();
        this.state = {
            expandedState: this.props.isExpanded,
            ouiaStateId: helpers_1.getDefaultOUIAId(NavExpandable.displayName)
        };
        this.onExpand = (e, onToggle) => {
            const { expandedState } = this.state;
            if (this.props.onExpand) {
                this.props.onExpand(e, !expandedState);
            }
            else {
                this.setState(prevState => ({ expandedState: !prevState.expandedState }));
                const { groupId } = this.props;
                onToggle(e, groupId, !expandedState);
            }
        };
    }
    componentDidMount() {
        this.setState({ expandedState: this.props.isExpanded });
    }
    componentDidUpdate(prevProps) {
        if (this.props.isExpanded !== prevProps.isExpanded) {
            this.setState({ expandedState: this.props.isExpanded });
        }
    }
    render() {
        const _a = this.props, { title, srText, children, className, isActive, ouiaId, 
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        groupId, 
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        id, 
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        isExpanded, buttonProps, 
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        onExpand } = _a, props = tslib_1.__rest(_a, ["title", "srText", "children", "className", "isActive", "ouiaId", "groupId", "id", "isExpanded", "buttonProps", "onExpand"]);
        const { expandedState, ouiaStateId } = this.state;
        return (React.createElement(Nav_1.NavContext.Consumer, null, context => (React.createElement("li", Object.assign({ className: react_styles_1.css(nav_1.default.navItem, nav_1.default.modifiers.expandable, expandedState && nav_1.default.modifiers.expanded, isActive && nav_1.default.modifiers.current, className) }, helpers_1.getOUIAProps(NavExpandable.displayName, ouiaId !== undefined ? ouiaId : ouiaStateId), props),
            React.createElement(PageSidebar_1.PageSidebarContext.Consumer, null, ({ isNavOpen }) => (React.createElement("button", Object.assign({ className: nav_1.default.navLink, id: srText ? null : this.id, onClick: e => this.onExpand(e, context.onToggle), "aria-expanded": expandedState, tabIndex: isNavOpen ? null : -1 }, buttonProps),
                title,
                React.createElement("span", { className: react_styles_1.css(nav_1.default.navToggle) },
                    React.createElement("span", { className: react_styles_1.css(nav_1.default.navToggleIcon) },
                        React.createElement(angle_right_icon_1.default, { "aria-hidden": "true" })))))),
            React.createElement("section", { className: react_styles_1.css(nav_1.default.navSubnav), "aria-labelledby": this.id, hidden: expandedState ? null : true },
                srText && (React.createElement("h2", { className: react_styles_1.css(accessibility_1.default.screenReader), id: this.id }, srText)),
                React.createElement("ul", { className: react_styles_1.css(nav_1.default.navList) }, children))))));
    }
}
exports.NavExpandable = NavExpandable;
NavExpandable.displayName = 'NavExpandable';
NavExpandable.defaultProps = {
    srText: '',
    isExpanded: false,
    children: '',
    className: '',
    groupId: null,
    isActive: false,
    id: ''
};
//# sourceMappingURL=NavExpandable.js.map