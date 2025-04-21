import type ScopeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/scopeRepresentation";
import { TextControl, useAlerts, useFetch } from "@keycloak/keycloak-ui-shared";
import {
  ActionGroup,
  AlertVariant,
  Button,
  ButtonVariant,
  DropdownItem,
  PageSection,
} from "@patternfly/react-core";
import { useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";
import { useAdminClient } from "../../admin-client";
import { FormAccess } from "../../components/form/FormAccess";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { useParams } from "../../utils/useParams";
import useToggle from "../../utils/useToggle";
import { toAuthorizationTab } from "../routes/AuthenticationTab";
import type { ScopeDetailsParams } from "../routes/Scope";
import { DeleteScopeDialog } from "./DeleteScopeDialog";

type FormFields = Omit<ScopeRepresentation, "resources">;

export default function ScopeDetails() {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { id, scopeId, realm } = useParams<ScopeDetailsParams>();
  const navigate = useNavigate();
  const { addAlert, addError } = useAlerts();
  const [deleteDialog, toggleDeleteDialog] = useToggle();
  const [scope, setScope] = useState<ScopeRepresentation>();
  const form = useForm<FormFields>({
    mode: "onChange",
  });
  const { reset, handleSubmit } = form;

  useFetch(
    async () => {
      if (scopeId) {
        const scope = await adminClient.clients.getAuthorizationScope({
          id,
          scopeId,
        });
        if (!scope) {
          throw new Error(t("notFound"));
        }
        return scope;
      }
    },
    (scope) => {
      setScope(scope);
      reset({ ...scope });
    },
    [],
  );

  const onSubmit = async (scope: ScopeRepresentation) => {
    try {
      if (scopeId) {
        await adminClient.clients.updateAuthorizationScope(
          { id, scopeId },
          scope,
        );
        setScope(scope);
      } else {
        await adminClient.clients.createAuthorizationScope(
          { id },
          {
            name: scope.name!,
            displayName: scope.displayName,
            iconUri: scope.iconUri,
          },
        );
        navigate(toAuthorizationTab({ realm, clientId: id, tab: "scopes" }));
      }
      addAlert(
        t((scopeId ? "update" : "create") + "ScopeSuccess"),
        AlertVariant.success,
      );
    } catch (error) {
      addError("scopeSaveError", error);
    }
  };

  return (
    <>
      <DeleteScopeDialog
        clientId={id}
        open={deleteDialog}
        toggleDialog={toggleDeleteDialog}
        selectedScope={scope}
        refresh={() =>
          navigate(toAuthorizationTab({ realm, clientId: id, tab: "scopes" }))
        }
      />
      <ViewHeader
        titleKey={scopeId ? scope?.name! : t("createAuthorizationScope")}
        dropdownItems={
          scopeId
            ? [
                <DropdownItem
                  key="delete"
                  data-testid="delete-resource"
                  onClick={() => toggleDeleteDialog()}
                >
                  {t("delete")}
                </DropdownItem>,
              ]
            : undefined
        }
      />
      <PageSection variant="light">
        <FormProvider {...form}>
          <FormAccess
            isHorizontal
            role="manage-authorization"
            onSubmit={handleSubmit(onSubmit)}
          >
            <TextControl
              name="name"
              label={t("name")}
              labelIcon={t("scopeNameHelp")}
              rules={{ required: t("required") }}
            />
            <TextControl
              name="displayName"
              label={t("displayName")}
              labelIcon={t("scopeDisplayNameHelp")}
            />
            <TextControl
              name="iconUri"
              label={t("iconUri")}
              labelIcon={t("iconUriHelp")}
            />
            <ActionGroup>
              <div className="pf-v5-u-mt-md">
                <Button
                  variant={ButtonVariant.primary}
                  type="submit"
                  data-testid="save"
                >
                  {t("save")}
                </Button>

                {!scope ? (
                  <Button
                    variant="link"
                    data-testid="cancel"
                    component={(props) => (
                      <Link
                        {...props}
                        to={toAuthorizationTab({
                          realm,
                          clientId: id,
                          tab: "scopes",
                        })}
                      ></Link>
                    )}
                  >
                    {t("cancel")}
                  </Button>
                ) : (
                  <Button
                    variant="link"
                    data-testid="revert"
                    onClick={() => reset({ ...scope })}
                  >
                    {t("revert")}
                  </Button>
                )}
              </div>
            </ActionGroup>
          </FormAccess>
        </FormProvider>
      </PageSection>
    </>
  );
}
