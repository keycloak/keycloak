import React from 'react';
import { render } from '@testing-library/react';
import { ActionGroup } from '../ActionGroup';
import { Form } from '../Form';

describe('ActionGroup component', () => {
  test('should render default action group variant', () => {
    const { asFragment } = render(
      <ActionGroup>
        <div>Hello</div>
      </ActionGroup>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('should render horizontal form ActionGroup variant', () => {
    const { asFragment } = render(
      <Form isHorizontal>
        <ActionGroup />
      </Form>
    );
    expect(asFragment()).toMatchSnapshot();
  });
});
