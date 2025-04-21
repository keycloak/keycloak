'use strict';

require('@babel/register');

const {
  configs: {
    recommended: { rules: recommended },
    strict: { rules: strict },
  },
  rules,
} = require('./src');

const ruleTableRows = Object.keys(rules)
  .sort()
  .map((id) => {
    const { meta } = rules[id];
    const { url, errorOptions } = meta.docs;

    const recSev = [].concat(recommended[`jsx-a11y/${id}`] || 'off')[0];
    const strictSev = [].concat(strict[`jsx-a11y/${id}`] || 'off')[0];

    return [
      `[${id}](${url})`,
      recSev === 'error' ? (errorOptions ? 'error, with options' : 'error') : recSev, // eslint-disable-line no-nested-ternary
      strictSev,
    ].join(' | ');
  });

const buildRulesTable = (rows) => {
  const header = 'Rule | Recommended | Strict';
  const separator = ':--- | :--- | :---';

  return [header, separator].concat(rows)
    .map((row) => `| ${row} |`)
    .join('\n');
};

const ruleList = Object.keys(rules)
  .sort()
  .map((id) => {
    const { meta } = rules[id];
    const { description, url } = meta.docs;
    return description ? [`- [${id}](${url}): ${description}`] : null;
  });

const buildRuleList = (listItems) => listItems.join('\n');

const LIST = () => buildRuleList(ruleList);
const TABLE = () => buildRulesTable(ruleTableRows);

module.exports = {
  transforms: {
    TABLE,
    LIST,
  },
  callback: () => {
    console.log('The auto-generating of rules finished!');
  },
};
