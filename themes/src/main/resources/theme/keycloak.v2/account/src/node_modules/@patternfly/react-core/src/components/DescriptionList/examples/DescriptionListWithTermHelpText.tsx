import React from 'react';
import {
  Button,
  DescriptionList,
  DescriptionListGroup,
  DescriptionListDescription,
  DescriptionListTermHelpText,
  DescriptionListTermHelpTextButton,
  Popover
} from '@patternfly/react-core';
import PlusCircleIcon from '@patternfly/react-icons/dist/esm/icons/plus-circle-icon';

export const DescriptionListWithTermHelpText: React.FunctionComponent = () => (
  <DescriptionList>
    <DescriptionListGroup>
      <DescriptionListTermHelpText>
        <Popover headerContent={<div>Name</div>} bodyContent={<div>Additional name info</div>}>
          <DescriptionListTermHelpTextButton> Name </DescriptionListTermHelpTextButton>
        </Popover>
      </DescriptionListTermHelpText>
      <DescriptionListDescription>Example</DescriptionListDescription>
    </DescriptionListGroup>
    <DescriptionListGroup>
      <DescriptionListTermHelpText>
        <Popover headerContent={<div>Namespace</div>} bodyContent={<div>Additional namespace info</div>}>
          <DescriptionListTermHelpTextButton> Namespace </DescriptionListTermHelpTextButton>
        </Popover>
      </DescriptionListTermHelpText>
      <DescriptionListDescription>
        <a href="#">mary-test</a>
      </DescriptionListDescription>
    </DescriptionListGroup>
    <DescriptionListGroup>
      <DescriptionListTermHelpText>
        <Popover headerContent={<div>Labels</div>} bodyContent={<div>Additional labels info</div>}>
          <DescriptionListTermHelpTextButton> Labels </DescriptionListTermHelpTextButton>
        </Popover>
      </DescriptionListTermHelpText>
      <DescriptionListDescription>example</DescriptionListDescription>
    </DescriptionListGroup>
    <DescriptionListGroup>
      <DescriptionListTermHelpText>
        <Popover headerContent={<div>Pod selector</div>} bodyContent={<div>Additional pod selector info</div>}>
          <DescriptionListTermHelpTextButton> Pod selector </DescriptionListTermHelpTextButton>
        </Popover>
      </DescriptionListTermHelpText>
      <DescriptionListDescription>
        <Button variant="link" isInline icon={<PlusCircleIcon />}>
          app=MyApp
        </Button>
      </DescriptionListDescription>
    </DescriptionListGroup>
    <DescriptionListGroup>
      <DescriptionListTermHelpText>
        <Popover headerContent={<div>Annotation</div>} bodyContent={<div>Additional annotation info</div>}>
          <DescriptionListTermHelpTextButton> Annotation </DescriptionListTermHelpTextButton>
        </Popover>
      </DescriptionListTermHelpText>
      <DescriptionListDescription>2 Annotations</DescriptionListDescription>
    </DescriptionListGroup>
  </DescriptionList>
);
