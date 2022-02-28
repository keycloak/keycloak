import * as React from 'react';
import { shallow } from 'enzyme';
import { InternalDropdownItem } from '../InternalDropdownItem';
import { DropdownSeparator } from '../DropdownSeparator';

describe('dropdown items', () => {
  test('a', () => {
    const view = shallow(<InternalDropdownItem>Something</InternalDropdownItem>);
    expect(view).toMatchSnapshot();
  });

  test('button', () => {
    const view = shallow(<InternalDropdownItem component="button">Something</InternalDropdownItem>);
    expect(view).toMatchSnapshot();
  });

  test('separator', () => {
    const view = shallow(<DropdownSeparator />);
    expect(view).toMatchSnapshot();
  });

  describe('hover', () => {
    test('a', () => {
      const view = shallow(<InternalDropdownItem isHovered>Something</InternalDropdownItem>);
      expect(view).toMatchSnapshot();
    });
    test('button', () => {
      const view = shallow(
        <InternalDropdownItem isHovered component="button">
          Something
        </InternalDropdownItem>
      );
      expect(view).toMatchSnapshot();
    });
  });

  describe('disabled', () => {
    test('a', () => {
      const view = shallow(<InternalDropdownItem isDisabled>Something</InternalDropdownItem>);
      expect(view).toMatchSnapshot();
    });
    test('button', () => {
      const view = shallow(
        <InternalDropdownItem isDisabled component="button">
          Something
        </InternalDropdownItem>
      );
      expect(view).toMatchSnapshot();
    });
  });
});
