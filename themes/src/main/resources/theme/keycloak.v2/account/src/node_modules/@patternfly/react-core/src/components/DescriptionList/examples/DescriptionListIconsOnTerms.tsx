import React from 'react';
import {
  Button,
  DescriptionList,
  DescriptionListTerm,
  DescriptionListGroup,
  DescriptionListDescription
} from '@patternfly/react-core';
import PlusCircleIcon from '@patternfly/react-icons/dist/esm/icons/plus-circle-icon';
import CubeIcon from '@patternfly/react-icons/dist/esm/icons/cube-icon';
import BookIcon from '@patternfly/react-icons/dist/esm/icons/book-icon';
import KeyIcon from '@patternfly/react-icons/dist/esm/icons/key-icon';
import GlobeIcon from '@patternfly/react-icons/dist/esm/icons/globe-icon';
import FlagIcon from '@patternfly/react-icons/dist/esm/icons/flag-icon';

export const DescriptionListIconsOnTerms: React.FunctionComponent = () => (
  <DescriptionList>
    <DescriptionListGroup>
      <DescriptionListTerm icon={<CubeIcon />}>Name</DescriptionListTerm>
      <DescriptionListDescription>Example</DescriptionListDescription>
    </DescriptionListGroup>
    <DescriptionListGroup>
      <DescriptionListTerm icon={<BookIcon />}>Namespace</DescriptionListTerm>
      <DescriptionListDescription>
        <a href="#">mary-test</a>
      </DescriptionListDescription>
    </DescriptionListGroup>
    <DescriptionListGroup>
      <DescriptionListTerm icon={<KeyIcon />}>Labels</DescriptionListTerm>
      <DescriptionListDescription>example</DescriptionListDescription>
    </DescriptionListGroup>
    <DescriptionListGroup>
      <DescriptionListTerm icon={<GlobeIcon />}>Pod selector</DescriptionListTerm>
      <DescriptionListDescription>
        <Button variant="link" isInline icon={<PlusCircleIcon />}>
          app=MyApp
        </Button>
      </DescriptionListDescription>
    </DescriptionListGroup>
    <DescriptionListGroup>
      <DescriptionListTerm icon={<FlagIcon />}>Annotation</DescriptionListTerm>
      <DescriptionListDescription>2 Annotations</DescriptionListDescription>
    </DescriptionListGroup>
  </DescriptionList>
);
