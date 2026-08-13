import { useHelp } from "@keycloak/keycloak-ui-shared";
import {
  Divider,
  Dropdown,
  DropdownItem,
  DropdownList,
  MenuToggle,
  Split,
  SplitItem,
  Switch,
} from "@patternfly/react-core";
import { HelpIcon } from "@patternfly/react-icons";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import helpUrls from "../../help-urls";
import { FormattedLink } from "../external-link/FormattedLink";

import "./help-header.css";

export const HelpHeader = () => {
  const [open, setOpen] = useState(false);
  const help = useHelp();
  const { t } = useTranslation();

  const dropdownItems = [
    <DropdownItem key="link" id="link">
      <FormattedLink
        data-testId="documentation-link"
        href={helpUrls.documentationUrl}
        title={t("documentation")}
      />
    </DropdownItem>,
    <Divider key="divide" />,
    <DropdownItem key="enable" id="enable" description={t("helpToggleInfo")}>
      <Split>
        <SplitItem isFilled>{t("enableHelpMode")}</SplitItem>
        <SplitItem>
          <Switch
            id="enableHelp"
            aria-label={t("enableHelp")}
            isChecked={help.enabled}
            label=""
            className="keycloak_help-header-switch"
            onChange={() => help.toggleHelp()}
          />
        </SplitItem>
      </Split>
    </DropdownItem>,
  ];
  return (
    <Dropdown
      popperProps={{
        position: "right",
      }}
      onOpenChange={(isOpen) => setOpen(isOpen)}
      isOpen={open}
      toggle={(ref) => (
        <MenuToggle
          ref={ref}
          variant="plain"
          onClick={() => setOpen(!open)}
          aria-label="Help"
          id="help"
          data-testid="help-toggle"
        >
          <HelpIcon />
        </MenuToggle>
      )}
    >
      <DropdownList>{dropdownItems}</DropdownList>
    </Dropdown>
  );
};
