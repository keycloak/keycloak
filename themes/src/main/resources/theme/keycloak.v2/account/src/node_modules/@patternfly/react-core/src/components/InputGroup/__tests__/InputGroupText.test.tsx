import React from 'react';

import { render, screen } from '@testing-library/react';

import { InputGroupText, InputGroupTextVariant } from '../InputGroupText';

describe('InputGroupText', () => {
  test('renders', () => {
    render(
      <InputGroupText className="inpt-grp-text" variant={InputGroupTextVariant.plain} id="email-npt-grp">
        @
      </InputGroupText>
    );
    expect(screen.getByText('@')).toBeInTheDocument();
  });
});
