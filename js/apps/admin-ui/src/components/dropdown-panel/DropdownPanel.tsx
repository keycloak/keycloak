import { useEffect, useRef } from "react";
import { Icon } from "@patternfly/react-core";
import { CaretDownIcon } from "@patternfly/react-icons";
import "./dropdown-panel.css";

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
    <span ref={dropdownRef}>
      <button
        className="kc-dropdown-panel"
        onClick={() => setSearchDropdownOpen(!searchDropdownOpen)}
        aria-label={buttonText}
        style={{ width, marginRight }}
        data-testid="dropdown-panel-btn"
      >
        {buttonText}
        <Icon className="kc-dropdown-panel-icon">
          <CaretDownIcon />
        </Icon>
      </button>
      {searchDropdownOpen && (
        <div className="kc-dropdown-panel-content">{children}</div>
      )}
    </span>
  );
};

export default DropdownPanel;
