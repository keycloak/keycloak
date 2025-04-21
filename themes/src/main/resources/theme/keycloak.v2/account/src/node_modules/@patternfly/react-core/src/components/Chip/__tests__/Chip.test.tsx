import React from 'react';
import { render } from '@testing-library/react';
import { Chip } from '../Chip';

describe('Chip', () => {
  test('overflow', () => {
    const { asFragment } = render(
      <Chip className="my-chp-cls" isOverflowChip>
        4 more
      </Chip>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('closable', () => {
    const { asFragment } = render(
      <Chip className="my-chp-cls" id="chip_one">
        Chip
      </Chip>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('closable with tooltip', () => {
    const { asFragment } = render(
      <Chip className="my-chp-cls" id="chip_one">
        1234567890123456789
      </Chip>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('readonly', () => {
    const { asFragment } = render(
      <Chip className="my-chp-cls" isReadOnly>
        4 more
      </Chip>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('custom max-width text', () => {
    const { asFragment } = render(
      <Chip className="my-chp-cls" textMaxWidth="100px">
        4 more
      </Chip>
    );
    expect(asFragment()).toMatchSnapshot();
  });
});
