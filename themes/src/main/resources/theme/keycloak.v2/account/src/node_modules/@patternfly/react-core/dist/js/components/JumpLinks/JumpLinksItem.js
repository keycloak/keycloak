"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.JumpLinksItem = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const jump_links_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/JumpLinks/jump-links"));
const JumpLinksList_1 = require("./JumpLinksList");
const JumpLinksItem = (_a) => {
    var { isActive, href, 
    // eslint-disable-next-line
    node, children, onClick, className } = _a, props = tslib_1.__rest(_a, ["isActive", "href", "node", "children", "onClick", "className"]);
    const childrenArr = React.Children.toArray(children);
    const sublists = childrenArr.filter(child => child.type === JumpLinksList_1.JumpLinksList);
    children = childrenArr.filter(child => child.type !== JumpLinksList_1.JumpLinksList);
    return (React.createElement("li", Object.assign({ className: react_styles_1.css(jump_links_1.default.jumpLinksItem, isActive && jump_links_1.default.modifiers.current, className) }, (isActive && { 'aria-current': 'location' }), props),
        React.createElement("a", { className: jump_links_1.default.jumpLinksLink, href: href, onClick: onClick },
            React.createElement("span", { className: jump_links_1.default.jumpLinksLinkText }, children)),
        sublists));
};
exports.JumpLinksItem = JumpLinksItem;
exports.JumpLinksItem.displayName = 'JumpLinksItem';
//# sourceMappingURL=JumpLinksItem.js.map