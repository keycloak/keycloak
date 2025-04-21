"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.TreeViewRoot = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const tree_view_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/TreeView/tree-view"));
const util_1 = require("../../helpers/util");
const helpers_1 = require("../../helpers");
class TreeViewRoot extends React.Component {
    constructor() {
        super(...arguments);
        this.displayName = 'TreeViewRoot';
        this.treeRef = React.createRef();
        this.handleKeys = (event) => {
            if (this.treeRef.current !== event.target.closest('.pf-c-tree-view')) {
                return;
            }
            const activeElement = document.activeElement;
            const key = event.key;
            const treeItems = Array.from(this.treeRef.current.getElementsByClassName('pf-c-tree-view__node')).filter(el => !el.classList.contains('pf-m-disabled'));
            if (key === 'Space') {
                document.activeElement.click();
                event.preventDefault();
            }
            helpers_1.handleArrows(event, treeItems, (element) => activeElement === element, undefined, [], undefined, true, true);
            if (['ArrowLeft', 'ArrowRight'].includes(key)) {
                const isExpandable = activeElement.firstElementChild.firstElementChild.classList.contains('pf-c-tree-view__node-toggle');
                const isExpanded = activeElement.closest('li').classList.contains('pf-m-expanded');
                if (key === 'ArrowLeft') {
                    if (isExpandable && isExpanded) {
                        activeElement.click();
                    }
                    else {
                        const parentList = activeElement.closest('ul').parentElement;
                        if (parentList.tagName !== 'DIV') {
                            const parentButton = parentList.querySelector('button');
                            activeElement.tabIndex = -1;
                            parentButton.tabIndex = 0;
                            parentButton.focus();
                        }
                    }
                }
                else {
                    if (isExpandable && !isExpanded) {
                        activeElement.tabIndex = -1;
                        activeElement.click();
                        const childElement = activeElement
                            .closest('li')
                            .querySelector('ul > li')
                            .querySelector('button');
                        childElement.tabIndex = 0;
                        childElement.focus();
                    }
                }
                event.preventDefault();
            }
        };
        this.handleKeysCheckbox = (event) => {
            if (this.treeRef.current !== event.target.closest('.pf-c-tree-view')) {
                return;
            }
            const activeElement = document.activeElement;
            const key = event.key;
            if (key === 'Space') {
                document.activeElement.click();
                event.preventDefault();
            }
            const treeNodes = Array.from(this.treeRef.current.getElementsByClassName('pf-c-tree-view__node'));
            helpers_1.handleArrows(event, treeNodes, (element) => element.contains(activeElement), (element) => element.querySelector('BUTTON,INPUT'), [], undefined, true, true);
            if (['ArrowLeft', 'ArrowRight'].includes(key)) {
                if (key === 'ArrowLeft') {
                    if (activeElement.tagName === 'INPUT') {
                        activeElement.parentElement.previousSibling &&
                            activeElement.parentElement.previousSibling.focus();
                    }
                    else if (activeElement.previousSibling) {
                        if (activeElement.previousElementSibling.tagName === 'SPAN') {
                            activeElement.previousSibling.firstChild.focus();
                        }
                        else {
                            activeElement.previousSibling.focus();
                        }
                    }
                }
                else {
                    if (activeElement.tagName === 'INPUT') {
                        activeElement.parentElement.nextSibling && activeElement.parentElement.nextSibling.focus();
                    }
                    else if (activeElement.nextSibling) {
                        if (activeElement.nextElementSibling.tagName === 'SPAN') {
                            activeElement.nextSibling.firstChild.focus();
                        }
                        else {
                            activeElement.nextSibling.focus();
                        }
                    }
                }
                event.preventDefault();
            }
        };
        this.variantStyleModifiers = {
            default: '',
            compact: tree_view_1.default.modifiers.compact,
            compactNoBackground: [tree_view_1.default.modifiers.compact, tree_view_1.default.modifiers.noBackground]
        };
    }
    componentDidMount() {
        if (util_1.canUseDOM) {
            window.addEventListener('keydown', this.props.hasChecks ? this.handleKeysCheckbox : this.handleKeys);
        }
        if (this.props.hasChecks) {
            const firstToggle = this.treeRef.current.getElementsByClassName('pf-c-tree-view__node-toggle')[0];
            if (firstToggle) {
                firstToggle.tabIndex = 0;
            }
            const firstInput = this.treeRef.current.getElementsByTagName('INPUT')[0];
            if (firstInput) {
                firstInput.tabIndex = 0;
            }
        }
        else {
            this.treeRef.current.getElementsByClassName('pf-c-tree-view__node')[0].tabIndex = 0;
        }
    }
    componentWillUnmount() {
        if (util_1.canUseDOM) {
            window.removeEventListener('keydown', this.props.hasChecks ? this.handleKeysCheckbox : this.handleKeys);
        }
    }
    render() {
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        const _a = this.props, { children, hasChecks, hasGuides, variant, className } = _a, props = tslib_1.__rest(_a, ["children", "hasChecks", "hasGuides", "variant", "className"]);
        return (React.createElement("div", Object.assign({ className: react_styles_1.css(tree_view_1.default.treeView, hasGuides && tree_view_1.default.modifiers.guides, this.variantStyleModifiers[variant], className), ref: this.treeRef }, props), children));
    }
}
exports.TreeViewRoot = TreeViewRoot;
//# sourceMappingURL=TreeViewRoot.js.map