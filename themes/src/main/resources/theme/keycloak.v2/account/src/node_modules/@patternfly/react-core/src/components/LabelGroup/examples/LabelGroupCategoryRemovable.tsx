import React from 'react';
import { Label, LabelGroup, LabelProps } from '@patternfly/react-core';
import InfoCircleIcon from '@patternfly/react-icons/dist/esm/icons/info-circle-icon';

export const LabelGroupCategoryRemovable: React.FunctionComponent = () => {
  const [labels, setLabels] = React.useState([
    ['Label 1', 'grey'],
    ['Label 2', 'blue'],
    ['Label 3', 'green'],
    ['Label 4', 'orange'],
    ['Label 5', 'red']
  ]);
  const deleteCategory = () => setLabels([]);

  return (
    <LabelGroup categoryName="Group label" isClosable onClick={deleteCategory}>
      {labels.map(([labelText, labelColor]) => (
        <Label icon={<InfoCircleIcon />} color={labelColor as LabelProps['color']} key={labelText}>
          {labelText}
        </Label>
      ))}
    </LabelGroup>
  );
};
