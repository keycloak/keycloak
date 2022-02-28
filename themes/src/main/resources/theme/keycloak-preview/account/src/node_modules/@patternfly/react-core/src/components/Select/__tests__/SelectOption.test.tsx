import React from 'react';
import { shallow, mount } from 'enzyme';
import { SelectOption, SelectOptionObject } from '../SelectOption';
import { SelectProvider } from '../selectConstants';

class User implements SelectOptionObject {
  private firstName: string;
  private lastName: string;
  private title: string;

  constructor(title: string, firstName: string, lastName: string) {
    this.title = title;
    this.firstName = firstName;
    this.lastName = lastName;
  }

  toString = (): string => `${this.title}: ${this.firstName} ${this.lastName}`;
}

describe('select options', () => {
  test('renders with value parameter successfully', () => {
    const view = mount(
      <SelectProvider value={{ onSelect: () => {}, onClose: () => {}, variant: 'single' }}>
        <SelectOption value="test" sendRef={jest.fn()} />
      </SelectProvider>
    );
    expect(view.instance().props).toHaveProperty('value', 'test');
    expect(view).toMatchSnapshot();
  });

  test('renders with custom display successfully', () => {
    const view = mount(
      <SelectProvider value={{ onSelect: () => {}, onClose: () => {}, variant: 'single' }}>
        <SelectOption value="test" sendRef={jest.fn()}>
          <div>test display</div>
        </SelectOption>
      </SelectProvider>
    );
    expect(view).toMatchSnapshot();
  });

  test('renders with custom user object successfully', () => {
    const view = mount(
      <SelectProvider value={{ onSelect: () => {}, onClose: () => {}, variant: 'single' }}>
        <SelectOption value={new User('Mr.', 'Test', 'User')} sendRef={jest.fn()} />
      </SelectProvider>
    );
    expect(view).toMatchSnapshot();
  });

  test('renders with custom display and custom user object successfully', () => {
    const view = mount(
      <SelectProvider value={{ onSelect: () => {}, onClose: () => {}, variant: 'single' }}>
        <SelectOption value={new User('Mr.', 'Test', 'User')} sendRef={jest.fn()}>
          <div>test display</div>
        </SelectOption>
      </SelectProvider>
    );
    expect(view).toMatchSnapshot();
  });

  test('renders custom component', () => {
    const view = mount(
      <SelectProvider value={{ onSelect: () => {}, onClose: () => {}, variant: 'single' }}>
        <SelectOption value={new User('Mr.', 'Test', 'User')} sendRef={jest.fn()} component="div">
          <div>test display</div>
        </SelectOption>
      </SelectProvider>
    );
    expect(view).toMatchSnapshot();
  });

  describe('disabled', () => {
    test('renders disabled successfully', () => {
      const view = mount(
        <SelectProvider value={{ onSelect: () => {}, onClose: () => {}, variant: 'single' }}>
          <SelectOption isDisabled value="test" sendRef={jest.fn()} />
        </SelectProvider>
      );
      expect(view.find('button').hasClass('pf-m-disabled')).toBe(true);
      expect(view).toMatchSnapshot();
    });
  });

  describe('is selected', () => {
    test('renders selected successfully', () => {
      const view = shallow(<SelectOption isSelected value="test" sendRef={jest.fn()} />);
      expect(view).toMatchSnapshot();
    });
  });

  describe('checked', () => {
    test('renders with checked successfully', () => {
      const view = shallow(<SelectOption isChecked value="test" sendRef={jest.fn()} />);
      expect(view).toMatchSnapshot();
    });
  });
});
