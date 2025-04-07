import {
  Button,
  Dropdown,
  DropdownItem,
  DropdownList,
  MenuToggle,
  ToolbarItem,
} from "@patternfly/react-core";
import { EllipsisVIcon } from "@patternfly/react-icons";
import { useTranslation } from "react-i18next";
import { useAccess } from "../../context/access/Access";
import useToggle from "../../utils/useToggle";
import { useSubGroups } from "../SubGroupsContext";

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
          onOpenChange={toggleKebab}
          toggle={(ref) => (
            <MenuToggle
              data-testid="kebab"
              ref={ref}
              isExpanded={openKebab}
              onClick={toggleKebab}
              isDisabled={kebabDisabled}
              variant="plain"
              aria-label="Actions"
            >
              <EllipsisVIcon />
            </MenuToggle>
          )}
          isOpen={openKebab}
        >
          <DropdownList>
            <DropdownItem
              key="action"
              component="button"
              onClick={() => {
                toggleDelete();
                toggleKebab();
              }}
            >
              {t("delete")}
            </DropdownItem>
          </DropdownList>
        </Dropdown>
      </ToolbarItem>
    </>
  );
};
