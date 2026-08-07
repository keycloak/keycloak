import { useRef } from "react";
import { MenuToggle, Panel, PanelMain, Popper } from "@patternfly/react-core";

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
}) => {
  const toggleRef = useRef<HTMLButtonElement>(null);
  const panelRef = useRef<HTMLDivElement>(null);

  const toggle = (
    <MenuToggle
      ref={toggleRef}
      onClick={() => setSearchDropdownOpen(!searchDropdownOpen)}
      isExpanded={searchDropdownOpen}
      style={{ width, marginRight }}
      data-testid="dropdown-panel-btn"
    >
      {buttonText}
    </MenuToggle>
  );

  const panel = (
    <Panel ref={panelRef} variant="raised">
      <PanelMain>{children}</PanelMain>
    </Panel>
  );

  return (
    <Popper
      trigger={toggle}
      triggerRef={toggleRef}
      popper={panel}
      popperRef={panelRef}
      isVisible={searchDropdownOpen}
      onDocumentClick={(event) => {
        if (
          event &&
          toggleRef.current &&
          !toggleRef.current.contains(event.target as Node)
        ) {
          setSearchDropdownOpen(false);
        }
      }}
    />
  );
};

export default DropdownPanel;
