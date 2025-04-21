import * as React from 'react';

import { render, screen } from '@testing-library/react';

import { Flex } from '../Flex';
import { FlexItem } from '../FlexItem';

describe('Flex', () => {
  test('Simple flex with single item', () => {
    const { asFragment } = render(
      <Flex>
        <FlexItem>Test</FlexItem>
      </Flex>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('Nested flex', () => {
    const { asFragment } = render(
      <Flex>
        <Flex>
          <FlexItem>Test</FlexItem>
        </Flex>
      </Flex>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('className is added to the root element', () => {
    render(<Flex className="extra-class" data-testid="test-id" />);
    expect(screen.getByTestId('test-id')).toHaveClass('extra-class');
  });

  const flexModifiers = {
    spacer: [
      'spacerNone',
      'spacerXs',
      'spacerSm',
      'spacerMd',
      'spacerLg',
      'spacerXl',
      'spacer2xl',
      'spacer3xl',
      'spacer4xl'
    ],
    spaceItems: [
      'spaceItemsNone',
      'spaceItemsXs',
      'spaceItemsSm',
      'spaceItemsMd',
      'spaceItemsLg',
      'spaceItemsXl',
      'spaceItems2xl',
      'spaceItems3xl',
      'spaceItems4xl'
    ],
    grow: ['grow'],
    shrink: ['shrink'],
    flex: ['flexDefault', 'flexNone', 'flex_1', 'flex_2', 'flex_3', 'flex_4'],
    direction: ['column', 'columnReverse', 'row', 'rowReverse'],
    alignItems: [
      'alignItemsFlexStart',
      'alignItemsFlexEnd',
      'alignItemsCenter',
      'alignItemsStretch',
      'alignItemsBaseline'
    ],
    alignContent: [
      'alignContentFlexStart',
      'alignContentFlexEnd',
      'alignContentCenter',
      'alignContentStretch',
      'alignContentSpaceBetween',
      'alignContentSpaceAround'
    ],
    alignSelf: ['alignSelfFlexStart', 'alignSelfFlexEnd', 'alignSelfCenter', 'alignSelfStretch', 'alignSelfBaseline'],
    align: ['alignLeft', 'alignRight'],
    justifyContent: [
      'justifyContentFlexStart',
      'justifyContentFlexEnd',
      'justifyContentCenter',
      'justifyContentSpaceBetween',
      'justifyContentSpaceAround',
      'justifyContentSpaceEvenly'
    ],
    display: ['inlineFlex'],
    fullWidth: ['fullWidth'],
    flexWrap: ['wrap', 'wrapReverse', 'nowrap']
  };

  describe('flex modifiers', () => {
    Object.entries(flexModifiers)
      .map(([mod, values]) =>
        values.map(value => ({
          [mod]: {
            default: value,
            sm: value,
            lg: value,
            xl: value,
            '2xl': value
          }
        }))
      )
      .reduce((acc, val) => acc.concat(val), [])
      .forEach(props =>
        test(`${JSON.stringify(props)} add valid classes to Flex`, () => {
          render(
            <Flex {...props} data-testid="test-id">
              {JSON.stringify(props)}
            </Flex>
          );

          const className = screen
            .getByTestId('test-id')
            .className.replace('pf-l-flex', '')
            .trim();

          expect(className).not.toBe("''");
          expect(className).not.toBe('');
        })
      );
  });

  const flexItemModifiers = {
    spacer: flexModifiers.spacer,
    grow: flexModifiers.grow,
    shrink: flexModifiers.shrink,
    flex: flexModifiers.flex,
    alignSelf: flexModifiers.alignSelf,
    align: flexModifiers.align,
    fullWidth: flexModifiers.fullWidth
  };

  describe('flexItem modifiers', () => {
    Object.entries(flexItemModifiers)
      .map(([mod, values]) =>
        values.map(value => ({
          [mod]: {
            default: value,
            sm: value,
            lg: value,
            xl: value,
            '2xl': value
          }
        }))
      )
      .reduce((acc, val) => acc.concat(val), [])
      .forEach(props =>
        test(`${JSON.stringify(props)} add valid classes to FlexItem`, () => {
          render(
            <FlexItem {...props} data-testid="test-id">
              {JSON.stringify(props)}
            </FlexItem>
          );

          const className = screen.getByTestId('test-id').className.trim();

          expect(className).not.toBe("''");
          expect(className).not.toBe('');
        })
      );
  });

  test('alternative component', () => {
    const { asFragment } = render(
      <Flex component="ul">
        <FlexItem component="li">Test</FlexItem>
      </Flex>
    );
    expect(asFragment()).toMatchSnapshot();
  });
});
