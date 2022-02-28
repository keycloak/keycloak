import React from 'react';
import { mount } from 'enzyme';
import { ChipButton } from '../ChipButton';
import { Chip } from '../Chip';

test('ChipButton', () => {
  const view = mount(
    <ChipButton id="my-chip-button" className="chip-bttn-cls">
      <b>Close</b>
    </ChipButton>
  );
  expect(view).toMatchSnapshot();
});

describe('Chip', () => {
  test('overflow', () => {
    const view = mount(
      <Chip className="my-chp-cls" isOverflowChip>
        4 more
      </Chip>
    );
    expect(view).toMatchSnapshot();
  });

  test('closable', () => {
    const view = mount(
      <Chip className="my-chp-cls" id="chip_one">
        Chip
      </Chip>
    );
    expect(view).toMatchSnapshot();
  });

  test('closable with tooltip', () => {
    const view = mount(
      <Chip className="my-chp-cls" id="chip_one">
        1234567890123456789
      </Chip>
    );
    expect(view).toMatchSnapshot();
  });

  test('readonly', () => {
    const view = mount(
      <Chip className="my-chp-cls" isReadOnly>
        4 more
      </Chip>
    );
    expect(view).toMatchSnapshot();
  });
});
