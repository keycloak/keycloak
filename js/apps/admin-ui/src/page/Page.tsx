import { ButtonVariant, DropdownItem } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { useNavigate, useParams } from "react-router-dom";
import { useAdminClient } from "../admin-client";
import { useAlerts } from "../components/alert/Alerts";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useRealm } from "../context/realm-context/RealmContext";
import { useServerInfo } from "../context/server-info/ServerInfoProvider";
import { PageHandler } from "./PageHandler";
import { PAGE_PROVIDER } from "./PageList";
import { PageParams, toPage } from "./routes";

export default function Page() {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { componentTypes } = useServerInfo();
  const { realm } = useRealm();
  const pages = componentTypes?.[PAGE_PROVIDER];
  const navigate = useNavigate();
  const { id, providerId } = useParams<PageParams>();
  const { addAlert, addError } = useAlerts();

  const page = pages?.find((p) => p.id === providerId);
  if (!page) {
    throw new Error(t("notFound"));
  }

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "itemDeleteConfirmTitle",
    messageKey: "itemDeleteConfirm",
    continueButtonLabel: "delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.components.del({
          id: id!,
        });
        addAlert(t("itemDeletedSuccess"));
        navigate(toPage({ realm, providerId: providerId! }));
      } catch (error) {
        addError("itemSaveError", error);
      }
    },
  });
  return (
    <>
      <DeleteConfirm />
      <ViewHeader
        titleKey={id || t("createItem")}
        dropdownItems={
          id
            ? [
                <DropdownItem
                  data-testid="delete-item"
                  key="delete"
                  onClick={() => toggleDeleteDialog()}
                >
                  {t("delete")}
                </DropdownItem>,
              ]
            : undefined
        }
      />
      <PageHandler providerType={PAGE_PROVIDER} id={id} page={page} />
    </>
  );
}
