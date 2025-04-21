/* eslint global-require: 0 */

import fs from 'fs';
import path from 'path';
import expect from 'expect';
import plugin from '../src';

const rules = fs.readdirSync(path.resolve(__dirname, '../src/rules/'))
  .map((f) => path.basename(f, '.js'));

describe('all rule files should be exported by the plugin', () => {
  rules.forEach((ruleName) => {
    it(`should export ${ruleName}`, () => {
      expect(plugin.rules[ruleName]).toEqual(
        require(path.join('../src/rules', ruleName)) // eslint-disable-line
      );
    });
  });
});

describe('configurations', () => {
  it('should export a \'recommended\' configuration', () => {
    expect(plugin.configs.recommended).toBeDefined();
  });
});

describe('schemas', () => {
  rules.forEach((ruleName) => {
    it(`${ruleName} should export a schema with type object`, () => {
      const rule = require(path.join('../src/rules', ruleName)); // eslint-disable-line
      const schema = rule.meta && rule.meta.schema && rule.meta.schema[0];
      const { type } = schema;

      expect(type).toEqual('object');
    });
  });
});
