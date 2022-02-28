"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
const requireindex_1 = __importDefault(require("requireindex"));
const path_1 = __importDefault(require("path"));
const recommended_json_1 = __importDefault(require("./configs/recommended.json"));
const rules = requireindex_1.default(path_1.default.join(__dirname, 'rules'));
// eslint expects the rule to be on rules[name], not rules[name].default
const rulesWithoutDefault = Object.keys(rules).reduce((acc, ruleName) => {
    acc[ruleName] = rules[ruleName].default;
    return acc;
}, {});
module.exports = {
    rules: rulesWithoutDefault,
    configs: {
        recommended: recommended_json_1.default,
    },
};
//# sourceMappingURL=index.js.map