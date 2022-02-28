import * as React from 'react';
import { Flex } from '../Flex';
import { FlexItem } from '../FlexItem';
import { shallow, mount } from 'enzyme';
import { FlexModifiers, FlexItemModifiers } from '../FlexUtils';

test('Simple flex with single item', () => {
  const view = shallow(
    <Flex>
      <FlexItem>Test</FlexItem>
    </Flex>
  );
  expect(view).toMatchSnapshot();
});

test('Nested flex', () => {
  const view = shallow(
    <Flex>
      <Flex>
        <FlexItem>Test</FlexItem>
      </Flex>
    </Flex>
  );
  expect(view).toMatchSnapshot();
});

test('className is added to the root element', () => {
  const view = shallow(<Flex className="extra-class" />);
  expect(view.prop('className')).toMatchSnapshot();
});

test('extra props are spread to the root element', () => {
  const testId = 'flex';
  const view = shallow(<Flex data-testid={testId} />);
  expect(view.prop('data-testid')).toBe(testId);
});

describe('flex modifiers', () => {
  Object.values(FlexModifiers).forEach(mod => {
    test(`${mod} is a valid modifier`, () => {
      const view = mount(<Flex breakpointMods={[{ modifier: mod as keyof typeof FlexModifiers }]}>{mod}</Flex>);
      expect(view.find('div').prop('className')).not.toMatch(/undefined/);
    });
  });
});

describe('flex item modifiers', () => {
  Object.values(FlexItemModifiers).forEach(mod => {
    test(`${mod} is a valid modifier`, () => {
      const view = mount(
        <FlexItem breakpointMods={[{ modifier: mod as keyof typeof FlexItemModifiers }]}>{mod}</FlexItem>
      );
      expect(view.find('div').prop('className')).not.toMatch(/undefined/);
    });
  });
});

test('flex modifier as string literal', () => {
  const view = mount(<Flex breakpointMods={[{ modifier: 'flex-1', breakpoint: 'sm' }]} />);
  expect(view.find('div').prop('className')).not.toMatch(/undefined/);
});

test('flex item modifier as string literal', () => {
  const view = mount(<FlexItem breakpointMods={[{ modifier: 'flex-1', breakpoint: 'sm' }]} />);
  expect(view.find('div').prop('className')).not.toMatch(/undefined/);
});
