import {
  Badge,
  MenuToggle,
  Select,
  SelectList,
  SelectOption,
} from "@patternfly/react-core";

type CheckboxFilterComponentProps = {
  filterPlaceholderText: string;
  isOpen: boolean;
  options: { value: string; label: string }[];
  onOpenChange: (isOpen: boolean) => void;
  onToggleClick: () => void;
  onSelect: (event: any, value: string) => void;
  selectedItems: string[];
  width: string;
};

export const CheckboxFilterComponent = ({
  filterPlaceholderText,
  isOpen,
  options,
  onOpenChange,
  onToggleClick,
  onSelect,
  selectedItems,
  width,
}: CheckboxFilterComponentProps) => {
  const toggle = (toggleRef: any) => (
    <MenuToggle
      ref={toggleRef}
      onClick={onToggleClick}
      isExpanded={isOpen}
      style={{ width }}
    >
      {filterPlaceholderText}
      {selectedItems.length > 0 && (
        <Badge isRead className="pf-v5-u-ml-xs">
          {selectedItems.length}
        </Badge>
      )}
    </MenuToggle>
  );

  return (
    <Select
      role="menu"
      isOpen={isOpen}
      onSelect={(event, value) => {
        if (value) {
          onSelect(event, value.toString());
        }
      }}
      onOpenChange={onOpenChange}
      selected={selectedItems}
      toggle={toggle}
    >
      <SelectList>
        {options.map((option) => (
          <SelectOption
            key={option.value}
            hasCheckbox
            value={option.value}
            isSelected={selectedItems.includes(option.value)}
          >
            {option.label}
          </SelectOption>
        ))}
      </SelectList>
    </Select>
  );
};
