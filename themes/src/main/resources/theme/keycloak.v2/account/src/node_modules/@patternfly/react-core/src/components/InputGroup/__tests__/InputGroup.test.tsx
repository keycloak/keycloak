import React from 'react';

import { render, screen } from '@testing-library/react';

import { InputGroup } from '../InputGroup';
import { Button } from '../../Button';
import { TextInput } from '../../TextInput';

describe('InputGroup', () => {
  test('add aria-describedby to form-control if one of the non form-controls has id', () => {
    // In this test, TextInput is a form-control component and Button is not.
    // If Button has an id props, this should be used in aria-describedby.
    render(
      <InputGroup>
        <TextInput value="some data" aria-label="some text" />
        <Button variant="primary" id="button-id">
          hello
        </Button>
      </InputGroup>
    );
    expect(screen.getByLabelText('some text')).toHaveAttribute('aria-describedby', 'button-id');
  });
});
