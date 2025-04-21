"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.CodeBlock = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const code_block_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/CodeBlock/code-block"));
const react_styles_1 = require("@patternfly/react-styles");
const CodeBlock = (_a) => {
    var { children = null, className, actions = null } = _a, props = tslib_1.__rest(_a, ["children", "className", "actions"]);
    return (React.createElement("div", Object.assign({ className: react_styles_1.css(code_block_1.default.codeBlock, className) }, props),
        React.createElement("div", { className: react_styles_1.css(code_block_1.default.codeBlockHeader) },
            React.createElement("div", { className: react_styles_1.css(code_block_1.default.codeBlockActions) }, actions && actions)),
        React.createElement("div", { className: react_styles_1.css(code_block_1.default.codeBlockContent) }, children)));
};
exports.CodeBlock = CodeBlock;
exports.CodeBlock.displayName = 'CodeBlock';
//# sourceMappingURL=CodeBlock.js.map