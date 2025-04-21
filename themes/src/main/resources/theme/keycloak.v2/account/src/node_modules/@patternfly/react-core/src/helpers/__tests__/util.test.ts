import {
  capitalize,
  getUniqueId,
  debounce,
  isElementInView,
  sideElementIsOutOfView,
  fillTemplate,
  pluralize,
  formatBreakpointMods
} from '../util';
import { SIDE } from '../constants';
import styles from '@patternfly/react-styles/css/layouts/Flex/flex';

const createMockHtmlElement = (bounds: Partial<DOMRect>) =>
  ({
    getBoundingClientRect: () => bounds
  } as HTMLElement);

test('capitalize', () => {
  expect(capitalize('foo')).toBe('Foo');
});

test('getUniqueId', () => {
  expect(getUniqueId()).not.toBe(getUniqueId());
});

test('getUniqueId prefixed', () => {
  expect(getUniqueId().substring(0, 3)).toBe('pf-');
  expect(getUniqueId('pf-switch').substring(0, 10)).toBe('pf-switch-');
});

test('debounce', () => {
  jest.useFakeTimers();
  const callback = jest.fn();
  const debouncedFunction = debounce(callback, 50);

  debouncedFunction();
  // At this point in time, the callback should not have been called yet
  expect(callback).toHaveBeenCalledTimes(0);

  for (let i = 0; i < 10; i++) {
    jest.advanceTimersByTime(50);
    debouncedFunction();
  }

  expect(callback).toHaveBeenCalledTimes(10);
});

test('isElementInView should be true when partial out of view and with partial true', () => {
  const container = createMockHtmlElement({ left: 0, right: 200 });
  const element = createMockHtmlElement({ left: 10, right: 210 });
  expect(isElementInView(container, element, true)).toBe(true);
});

test('isElementInView should be false when partial out of view and with partial false ', () => {
  const container = createMockHtmlElement({ left: 0, right: 200 });
  const element = createMockHtmlElement({ left: 10, right: 210 });
  expect(isElementInView(container, element, false)).toBe(false);
});

test('isElementInView should be false completely out of view ', () => {
  const container = createMockHtmlElement({ left: 0, right: 200 });
  const element = createMockHtmlElement({ left: 200, right: 300 });
  expect(isElementInView(container, element, true)).toBe(false);
});

test('isElementInView should be false completely out of view when partial false ', () => {
  const container = createMockHtmlElement({ left: 0, right: 200 });
  const element = createMockHtmlElement({ left: 200, right: 300 });
  expect(isElementInView(container, element, false)).toBe(false);
});

test('sideElementIsOutOfView Returns left when off on left side', () => {
  const container = createMockHtmlElement({ left: 20, right: 220 });
  const element = createMockHtmlElement({ left: 10, right: 210 });
  expect(sideElementIsOutOfView(container, element)).toBe(SIDE.LEFT);
});

test('sideElementIsOutOfView Returns right when off on right side', () => {
  const container = createMockHtmlElement({ left: 0, right: 200 });
  const element = createMockHtmlElement({ left: 210, right: 410 });
  expect(sideElementIsOutOfView(container, element)).toBe(SIDE.RIGHT);
});

test('sideElementIsOutOfView Returns NONE when in view', () => {
  const container = createMockHtmlElement({ left: 0, right: 200 });
  const element = createMockHtmlElement({ left: 10, right: 110 });
  expect(sideElementIsOutOfView(container, element)).toBe(SIDE.NONE);
});

test('fillTemplate interpolates strings correctly', () => {
  // eslint-disable-next-line no-template-curly-in-string
  const templateString = 'My name is ${firstName} ${lastName}';
  const expected = 'My name is Jon Dough';
  const templatVars = {
    firstName: 'Jon',
    lastName: 'Dough'
  };
  const actual = fillTemplate(templateString, templatVars);
  expect(actual).toEqual(expected);
});

test('text pluralize', () => {
  expect(pluralize(1, 'dog')).toEqual('1 dog');
  expect(pluralize(2, 'dog')).toEqual('2 dogs');
  expect(pluralize(2, 'finch', 'finches')).toEqual('2 finches');
});

test('formatBreakpointMods', () => {
  expect(formatBreakpointMods({ default: 'spacerNone' }, styles)).toEqual('pf-m-spacer-none');
  expect(formatBreakpointMods({ md: 'spacerNone' }, styles)).toEqual('pf-m-spacer-none-on-md');
  expect(formatBreakpointMods({ default: 'column', lg: 'row' }, styles)).toEqual('pf-m-column pf-m-row-on-lg');
});
