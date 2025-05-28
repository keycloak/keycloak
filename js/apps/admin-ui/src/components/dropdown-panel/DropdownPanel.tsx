import { Dropdown, MenuToggle } from "@patternfly/react-core";

type DropdownPanelProps = {
  buttonText: string;
  children: React.ReactNode;
  setSearchDropdownOpen: (open: boolean) => void;
  searchDropdownOpen: boolean;
  marginRight?: string;
  width: string;
};

const DropdownPanel: React.FC<DropdownPanelProps> = ({
  buttonText,
  children,
  setSearchDropdownOpen,
  searchDropdownOpen,
  marginRight,
  width,
}) => (
  <Dropdown
    onOpenChange={setSearchDropdownOpen}
    toggle={(ref) => (
      <MenuToggle
        data-testid="searchdropdown_dorpdown"
        ref={ref}
        onClick={() => setSearchDropdownOpen(!searchDropdownOpen)}
        style={{ width, marginRight: marginRight ? marginRight : "0" }}
      >
        {buttonText}
      </MenuToggle>
    )}
    isOpen={searchDropdownOpen}
  >
    {children}
  </Dropdown>
);

export default DropdownPanel;
