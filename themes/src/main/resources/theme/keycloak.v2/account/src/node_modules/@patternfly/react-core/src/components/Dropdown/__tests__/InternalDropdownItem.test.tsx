import React from 'react';
import { render } from '@testing-library/react';
import { InternalDropdownItem } from '../InternalDropdownItem';

describe('InternalDropdownItem', () => {
  describe('dropdown items', () => {
    test('a', () => {
      const { asFragment } = render(<InternalDropdownItem>Something</InternalDropdownItem>);
      expect(asFragment()).toMatchSnapshot();
    });

    test('button', () => {
      const { asFragment } = render(<InternalDropdownItem component="button">Something</InternalDropdownItem>);
      expect(asFragment()).toMatchSnapshot();
    });

    describe('hover', () => {
      test('a', () => {
        const { asFragment } = render(<InternalDropdownItem isHovered>Something</InternalDropdownItem>);
        expect(asFragment()).toMatchSnapshot();
      });

      test('button', () => {
        const { asFragment } = render(
          <InternalDropdownItem isHovered component="button">
            Something
          </InternalDropdownItem>
        );
        expect(asFragment()).toMatchSnapshot();
      });
    });

    describe('disabled', () => {
      test('a', () => {
        const { asFragment } = render(<InternalDropdownItem isDisabled>Something</InternalDropdownItem>);
        expect(asFragment()).toMatchSnapshot();
      });

      test('button', () => {
        const { asFragment } = render(
          <InternalDropdownItem isDisabled component="button">
            Something
          </InternalDropdownItem>
        );
        expect(asFragment()).toMatchSnapshot();
      });
    });

    describe('aria-disabled', () => {
      test('a', () => {
        const { asFragment } = render(<InternalDropdownItem isAriaDisabled>Something</InternalDropdownItem>);
        expect(asFragment()).toMatchSnapshot();
      });

      test('button', () => {
        const { asFragment } = render(
          <InternalDropdownItem isAriaDisabled component="button">
            Something
          </InternalDropdownItem>
        );
        expect(asFragment()).toMatchSnapshot();
      });
    });

    describe('description', () => {
      test('a', () => {
        const { asFragment } = render(
          <InternalDropdownItem description="Something's link description">Something</InternalDropdownItem>
        );
        expect(asFragment()).toMatchSnapshot();
      });

      test('button', () => {
        const { asFragment } = render(
          <InternalDropdownItem description="Something's button description" component="button">
            Something
          </InternalDropdownItem>
        );
        expect(asFragment()).toMatchSnapshot();
      });
    });
  });
});
