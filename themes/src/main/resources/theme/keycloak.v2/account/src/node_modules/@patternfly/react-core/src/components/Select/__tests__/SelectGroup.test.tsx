import React from 'react';

import { render } from '@testing-library/react';

import { SelectGroup } from '../SelectGroup';
import { SelectProvider } from '../selectConstants';

describe('SelectGroup', () => {
  test('renders with children successfully', () => {
    const { asFragment } = render(
      <SelectProvider
        value={{
          onSelect: () => {},
          onFavorite: () => {},
          onClose: () => {},
          variant: 'single',
          inputIdPrefix: '',
          shouldResetOnSelect: true
        }}
      >
        <SelectGroup label="test">
          <div>child</div>
        </SelectGroup>
      </SelectProvider>
    );
    expect(asFragment()).toMatchSnapshot();
  });
});
