import { useEffect, useRef } from "react";
import { Button, Icon } from "@patternfly/react-core";
import { CaretDownIcon } from "@patternfly/react-icons";
import "./dropdown-panel.css";

type DropdownPanelProps = {
  actionButtonText: string;
  actionButtonVariant:
    | "primary"
    | "secondary"
    | "tertiary"
    | "danger"
    | "link"
    | "plain"
    | "control";
  buttonText: string;
  children: React.ReactNode;
  setSearchDropdownOpen: (open: boolean) => void;
  searchDropdownOpen: boolean;
  onSubmitAction: () => void;
  width: string;
};

const DropdownPanel: React.FC<DropdownPanelProps> = ({
  actionButtonText,
  actionButtonVariant,
  buttonText,
  setSearchDropdownOpen,
  searchDropdownOpen,
  children,
  onSubmitAction,
  width,
}) => {
  const dropdownRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        dropdownRef.current &&
        !dropdownRef.current.contains(event.target as Node)
      ) {
        setSearchDropdownOpen(false);
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [setSearchDropdownOpen]);

  useEffect(() => {
    const handleVisibilityChange = () => {
      if (document.visibilityState === "hidden") {
        setSearchDropdownOpen(false);
      }
    };

    document.addEventListener("visibilitychange", handleVisibilityChange);
    return () =>
      document.removeEventListener("visibilitychange", handleVisibilityChange);
  }, [setSearchDropdownOpen]);

  return (
    <div ref={dropdownRef}>
      <button
        className="kc-dropdown-panel"
        onClick={() => setSearchDropdownOpen(!searchDropdownOpen)}
        aria-label={buttonText}
        style={{ width }}
      >
        {buttonText}
        <Icon className="kc-dropdown-panel-icon">
          <CaretDownIcon />
        </Icon>
      </button>
      {searchDropdownOpen && (
        <div className="kc-dropdown-panel-content">{children}</div>
      )}
      <Button
        variant={actionButtonVariant}
        className="pf-u-ml-md"
        onClick={onSubmitAction}
        data-testid="dropdownPanelBtn"
      >
        {actionButtonText}
      </Button>
    </div>
  );
};

export default DropdownPanel;
