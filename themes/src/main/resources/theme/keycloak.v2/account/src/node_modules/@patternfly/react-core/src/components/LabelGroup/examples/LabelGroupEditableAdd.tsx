import React from 'react';
import { LabelGroup, Label } from '@patternfly/react-core';

export const LabelGroupEditableAdd: React.FunctionComponent = () => {
  const [idIndex, setIdIndex] = React.useState<number>(3);
  const [labels, setLabels] = React.useState<any>([
    { name: 'Label 1', id: 0 },
    { name: 'Label 2', id: 1 },
    {
      name: 'Label 3',
      props: {
        isEditable: true,
        editableProps: {
          'aria-label': 'label editable text'
        }
      },
      id: 2
    }
  ]);

  const onClose = (labelId: string) => {
    setLabels(labels.filter((l: any) => l.id !== labelId));
  };

  const onEdit = (nextText: string, index: number) => {
    const copy = [...labels];
    copy[index] = { name: nextText, props: labels[index].props, id: labels[index].id };
    setLabels(copy);
  };

  const onAdd = () => {
    setLabels([
      {
        name: 'New Label',
        props: {
          isEditable: true,
          editableProps: {
            'aria-label': 'label editable text'
          }
        },
        id: idIndex
      },
      ...labels
    ]);
    setIdIndex(idIndex + 1);
  };

  return (
    <LabelGroup
      categoryName="Label group 1"
      numLabels={5}
      isEditable
      addLabelControl={
        <Label color="blue" variant="outline" isOverflowLabel onClick={onAdd}>
          Add label
        </Label>
      }
    >
      {labels.map((label, index) => (
        <Label
          key={`${label.name}-${index}`}
          id={`${label.name}-${index}`}
          color="blue"
          onClose={() => onClose(label.id)}
          onEditCancel={prevText => onEdit(prevText, index)}
          onEditComplete={newText => onEdit(newText, index)}
          {...label.props}
        >
          {label.name}
        </Label>
      ))}
    </LabelGroup>
  );
};
