import React from 'react';
import { LabelGroup, Label, Menu, MenuContent, MenuList, MenuItem, Popper } from '@patternfly/react-core';

export const LabelGroupEditableAddDropdown: React.FunctionComponent = () => {
  const toggleRef = React.useRef<HTMLDivElement>();
  const menuRef = React.useRef<HTMLDivElement>();
  const containerRef = React.useRef<HTMLDivElement>();

  const [idIndex, setIdIndex] = React.useState<number>(3);
  const [isOpen, setIsOpen] = React.useState<boolean>(false);
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

  const onAdd = (labelText: string) => {
    setLabels([
      {
        name: labelText,
        id: idIndex
      },
      ...labels
    ]);
    setIdIndex(idIndex + 1);
    setIsOpen(!isOpen);
  };

  const handleMenuKeys = (event: KeyboardEvent) => {
    if (isOpen && menuRef.current.contains(event.target as Node)) {
      if (event.key === 'Escape' || event.key === 'Tab') {
        setIsOpen(!isOpen);
        toggleRef.current.focus();
      }
    }
  };

  const handleClickOutside = (event: MouseEvent) => {
    if (
      isOpen &&
      !(menuRef.current.contains(event.target as Node) || toggleRef.current.contains(event.target as Node))
    ) {
      setIsOpen(false);
    }
  };

  React.useEffect(() => {
    window.addEventListener('keydown', handleMenuKeys);
    window.addEventListener('click', handleClickOutside);
    return () => {
      window.removeEventListener('keydown', handleMenuKeys);
      window.removeEventListener('click', handleClickOutside);
    };
  }, [isOpen, menuRef]);

  const onToggleClick = () => {
    setTimeout(() => {
      if (menuRef.current) {
        const firstElement = menuRef.current.querySelector('li > button:not(:disabled)');
        firstElement && (firstElement as HTMLElement).focus();
      }
    }, 0);
    setIsOpen(!isOpen);
  };

  const menu = (
    <Menu ref={menuRef} onSelect={(_ev, itemId) => onAdd(itemId.toString())}>
      <MenuContent>
        <MenuList>
          <MenuItem itemId="Label text 1">Label text 1</MenuItem>
          <MenuItem itemId="Label text 2">Label text 2</MenuItem>
          <MenuItem itemId="Label text 3">Label text 3</MenuItem>
          <MenuItem itemId="Label text 4">Label text 4</MenuItem>
        </MenuList>
      </MenuContent>
    </Menu>
  );

  const toggle = (
    <div ref={toggleRef}>
      <Label color="blue" variant="outline" isOverflowLabel onClick={onToggleClick}>
        Add label
      </Label>
    </div>
  );

  return (
    <div ref={containerRef}>
      <LabelGroup
        categoryName="Label group 1"
        numLabels={5}
        isEditable
        addLabelControl={
          <Popper
            trigger={toggle}
            popper={menu}
            appendTo={containerRef.current}
            isVisible={isOpen}
            popperMatchesTriggerWidth={false}
          />
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
    </div>
  );
};
