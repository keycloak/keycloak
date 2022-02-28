"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const applyDefault_1 = require("./applyDefault");
// note - cannot migrate this to an import statement because it will make TSC copy the package.json to the dist folder
const version = require('../../package.json').version;
// This function will get much easier to call when this is merged https://github.com/Microsoft/TypeScript/pull/26349
// TODO - when the above rule lands; add type checking for the context.report `data` property
function createRule({ name, meta, defaultOptions, create, }) {
    return {
        meta: Object.assign({}, meta, { docs: Object.assign({}, meta.docs, { url: `https://github.com/typescript-eslint/typescript-eslint/blob/v${version}/packages/eslint-plugin/docs/rules/${name}.md`, extraDescription: meta.docs.tslintName
                    ? [`\`${meta.docs.tslintName}\` from TSLint`]
                    : undefined }) }),
        create(context) {
            const optionsWithDefault = applyDefault_1.applyDefault(defaultOptions, context.options);
            return create(context, optionsWithDefault);
        },
    };
}
exports.createRule = createRule;
//# sourceMappingURL=createRule.js.map