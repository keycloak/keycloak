import React from 'react';
import PopoverBase from '../PopoverBase';
import { mount } from 'enzyme';

describe('<PopoverBase />', () => {
  let component;

  afterEach(() => {
    component.unmount();
  });

  it('renders only the child element for string content', () => {
    component = mount(
      <PopoverBase content="tooltip">
        <button />
      </PopoverBase>
    );
    expect(component).toMatchSnapshot();
  });

  it('renders only the child element for element content', () => {
    component = mount(
      <PopoverBase content={<div>tooltip</div>}>
        <button />
      </PopoverBase>
    );
    expect(component).toMatchSnapshot();
  });
});
