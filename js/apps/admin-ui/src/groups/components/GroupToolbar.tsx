import { useTranslation } from "react-i18next";
import { Button, ToolbarItem } from "@patternfly/react-core";
import {
  Dropdown,
  DropdownItem,
  KebabToggle,
} from "@patternfly/react-core/deprecated";

import { useSubGroups } from "../SubGroupsContext";
import { useAccess } from "../../context/access/Access";
import useToggle from "../../utils/useToggle";

type GroupToolbarProps = {
  toggleCreate: () => void;
  toggleDelete: () => void;
  kebabDisabled: boolean;
};

export const GroupToolbar = ({
  toggleCreate,
  toggleDelete,
  kebabDisabled,
}: GroupToolbarProps) => {
  const { t } = useTranslation();
  const { currentGroup } = useSubGroups();
  const { hasAccess } = useAccess();
  const isManager = hasAccess("manage-users") || currentGroup()?.access?.manage;

  const [openKebab, toggleKebab] = useToggle();

  if (!isManager) return <div />;

  return (
    <>
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
              {t("delete")}
            </DropdownItem>,
          ]}
        />
      </ToolbarItem>
    </>
  );
};
