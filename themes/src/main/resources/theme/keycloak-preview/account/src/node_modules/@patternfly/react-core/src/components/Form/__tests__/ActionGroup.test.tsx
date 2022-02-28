import React from 'react';
import { mount } from 'enzyme';
import { ActionGroup } from '../ActionGroup';
import { Form } from '../Form';

describe('ActionGroup component', () => {
  test('should render default action group variant', () => {
    const view = mount(
      <ActionGroup>
        <div>Hello</div>
      </ActionGroup>
    );
    expect(view).toMatchSnapshot();
  });

  test('should render horizontal form ActionGroup variant', () => {
    const view = mount(
      <Form isHorizontal>
        <ActionGroup />
      </Form>
    );
    expect(view).toMatchSnapshot();
  });
});
