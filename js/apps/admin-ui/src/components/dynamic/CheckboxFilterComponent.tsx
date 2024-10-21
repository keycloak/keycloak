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
  onSelect: (event: any, selection: any) => void;
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
  const toggle = (toggleRef: React.RefObject<HTMLButtonElement>) => (
    <MenuToggle
      ref={toggleRef}
      onClick={onToggleClick}
      isExpanded={isOpen}
      style={{
        width,
      }}
    >
      {filterPlaceholderText}
      {selectedItems.length > 0 && (
        <Badge isRead className="pf-v5-u-m-xs">
          {selectedItems.length}
        </Badge>
      )}
    </MenuToggle>
  );

  return (
    <Select
      role="menu"
      id="checkbox-select"
      isOpen={isOpen}
      selected={selectedItems}
      onSelect={(event, value) => {
        onSelect(event, value?.toString());
      }}
      onOpenChange={onOpenChange}
      toggle={toggle}
      data-testid="checkbox-filter-select"
    >
      <SelectList>
        {options.map((option) => (
          <SelectOption
            key={option.value}
            hasCheckbox
            value={option.value}
            isSelected={selectedItems.includes(option.value)}
            data-testid={`checkbox-filter-option-${option.value}`}
          >
            {option.label}
          </SelectOption>
        ))}
      </SelectList>
    </Select>
  );
};
