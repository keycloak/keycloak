import React from 'react';
import { ClipboardCopy } from '@patternfly/react-core';

export const ClipboardCopyInlineCompactInSentence: React.FunctionComponent = () => (
  <React.Fragment>
    <b>Basic</b>
    <br />
    Lorem ipsum{' '}
    {
      <ClipboardCopy hoverTip="Copy" clickTip="Copied" variant="inline-compact">
        2.3.4-2-redhat
      </ClipboardCopy>
    }
    dolor sit amet.
    <br /> <br />
    <b>Long copy string</b>
    <br />
    Lorem ipsum dolor sit amet, consectetur adipiscing elit.{' '}
    {
      <ClipboardCopy hoverTip="Copy" clickTip="Copied" variant="inline-compact">
        https://app.openshift.io/path/sub-path/sub-sub-path/?runtime=quarkus/12345678901234567890/abcdefghijklmnopqrstuvwxyz1234567890
      </ClipboardCopy>
    }{' '}
    Mauris luctus, libero nec dapibus ultricies, urna purus pretium mauris, ullamcorper pharetra lacus nibh vitae enim.
    <br /> <br />
    <b>Long copy string in block</b>
    <br />
    Lorem ipsum dolor sit amet, consectetur adipiscing elit.{' '}
    {
      <ClipboardCopy hoverTip="Copy" clickTip="Copied" variant="inline-compact" isBlock>
        https://app.openshift.io/path/sub-path/sub-sub-path/?runtime=quarkus/12345678901234567890/abcdefghijklmnopqrstuvwxyz1234567890
      </ClipboardCopy>
    }{' '}
    Mauris luctus, libero nec dapibus ultricies, urna purus pretium mauris, ullamcorper pharetra lacus nibh vitae enim.
  </React.Fragment>
);
