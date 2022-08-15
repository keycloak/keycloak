import { useTranslation } from "react-i18next";
import {
  Button,
  Dropdown,
  DropdownItem,
  KebabToggle,
  ToggleGroup,
  ToggleGroupItem,
  ToolbarItem,
} from "@patternfly/react-core";
import { TableIcon, DomainIcon } from "@patternfly/react-icons";

import { useSubGroups } from "../SubGroupsContext";
import { useAccess } from "../../context/access/Access";
import useToggle from "../../utils/useToggle";

export enum ViewType {
  Table,
  Tree,
}

type GroupToolbarProps = {
  toggleCreate: () => void;
  toggleDelete: () => void;
  currentView?: ViewType;
  toggleView?: (type: ViewType) => void;
  kebabDisabled: boolean;
};

export const GroupToolbar = ({
  toggleCreate,
  toggleDelete,
  currentView,
  toggleView,
  kebabDisabled,
}: GroupToolbarProps) => {
  const { t } = useTranslation("groups");
  const { currentGroup } = useSubGroups();
  const { hasAccess } = useAccess();
  const isManager = hasAccess("manage-users") || currentGroup()?.access?.manage;

  const [openKebab, toggleKebab] = useToggle();

  if (!isManager) return <div />;

  return (
    <>
      {toggleView && (
        <ToolbarItem>
          <ToggleGroup>
            <ToggleGroupItem
              icon={<TableIcon />}
              aria-label={t("tableView")}
              buttonId="tableView"
              isSelected={currentView === ViewType.Table}
              onChange={() => toggleView(ViewType.Table)}
            />
            <ToggleGroupItem
              icon={<DomainIcon />}
              aria-label={t("diagramView")}
              buttonId="diagramView"
              isSelected={currentView === ViewType.Tree}
              onChange={() => toggleView(ViewType.Tree)}
            />
          </ToggleGroup>
        </ToolbarItem>
      )}
      <ToolbarItem>
        <Button
          data-testid="openCreateGroupModal"
          variant="primary"
          onClick={toggleCreate}
        >
          {t("createGroup")}
        </Button>
      </ToolbarItem>
      <ToolbarItem>
        <Dropdown
          toggle={
            <KebabToggle onToggle={toggleKebab} isDisabled={kebabDisabled} />
          }
          isOpen={openKebab}
          isPlain
          dropdownItems={[
            <DropdownItem
              key="action"
              component="button"
              onClick={() => {
                toggleDelete();
                toggleKebab();
              }}
            >
              {t("common:delete")}
            </DropdownItem>,
          ]}
        />
      </ToolbarItem>
    </>
  );
};
