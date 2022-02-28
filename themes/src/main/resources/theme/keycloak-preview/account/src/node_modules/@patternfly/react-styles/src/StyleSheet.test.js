import { StyleSheet, css } from './StyleSheet';

test('converts a css string into an object', () => {
  const styles = StyleSheet.parse(`
  .pf-c-button {
    color: red;
  }
  .pf-c-alert {
    color: black;
  }
  `);
  expect(styles).toMatchSnapshot();
});

test('places modifers in the modifers object', () => {
  const styles = StyleSheet.parse(`
  .pf-m-active {}
  .pf-m-secondary-alt {}
  `);
  expect(styles).toMatchSnapshot();
});

describe('css', () => {
  const stringValue = 'value';
  const parseValue = StyleSheet.parse('.parseValue {}');
  const createValue = StyleSheet.create({ createValue: {} });

  test('removes falsy values', () => {
    const result = css(stringValue, null, undefined, false, 0);
    expect(result).toBe(stringValue);
  });

  test('handles values from StyleSheet.parse', () => {
    expect(css(parseValue.parseValue)).toMatchSnapshot();
  });

  test('handles values from StyleSheet.create', () => {
    expect(css(createValue.createValue)).toMatchSnapshot();
  });

  test('concatenates values together', () => {
    expect(css(parseValue.parseValue, createValue.createValue, stringValue)).toMatchSnapshot();
  });
});
