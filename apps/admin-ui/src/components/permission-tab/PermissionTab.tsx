import { useState } from "react";
import { Link, useNavigate } from "react-router-dom-v5-compat";
import { Trans, useTranslation } from "react-i18next";
import {
  Card,
  CardBody,
  CardTitle,
  Form,
  FormGroup,
  PageSection,
  Switch,
} from "@patternfly/react-core";
import {
  ActionsColumn,
  TableComposable,
  Tbody,
  Td,
  Th,
  Thead,
  Tr,
} from "@patternfly/react-table";

import type { ManagementPermissionReference } from "@keycloak/keycloak-admin-client/lib/defs/managementPermissionReference";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import { useRealm } from "../../context/realm-context/RealmContext";
import { toPermissionDetails } from "../../clients/routes/PermissionDetails";
import { KeycloakSpinner } from "../../components/keycloak-spinner/KeycloakSpinner";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import useLocaleSort from "../../utils/useLocaleSort";
import { useConfirmDialog } from "../confirm-dialog/ConfirmDialog";

import "./permissions-tab.css";

type PermissionScreenType =
  | "clients"
  | "users"
  | "groups"
  | "roles"
  | "identityProviders";

type PermissionsTabProps = {
  id?: string;
  type: PermissionScreenType;
};

export const PermissionsTab = ({ id, type }: PermissionsTabProps) => {
  const { t } = useTranslation("common");
  const navigate = useNavigate();
  const { adminClient } = useAdminClient();
  const { realm } = useRealm();
  const [realmId, setRealmId] = useState("");
  const [permission, setPermission] = useState<ManagementPermissionReference>();
  const localeSort = useLocaleSort();

  const togglePermissionEnabled = (enabled: boolean) => {
    switch (type) {
      case "clients":
        return adminClient.clients.updateFineGrainPermission(
          { id: id! },
          { enabled }
        );
      case "users":
        return adminClient.realms.updateUsersManagementPermissions({
          realm,
          enabled,
        });
      case "groups":
        return adminClient.groups.updatePermission({ id: id! }, { enabled });
      case "roles":
        return adminClient.roles.updatePermission({ id: id! }, { enabled });
      case "identityProviders":
        return adminClient.identityProviders.updatePermission(
          { alias: id! },
          { enabled }
        );
    }
  };

  useFetch(
    () =>
      Promise.all([
        adminClient.clients.find({
          search: true,
          clientId: realm === "master" ? "master-realm" : "realm-management",
        }),
        (() => {
          switch (type) {
            case "clients":
              return adminClient.clients.listFineGrainPermissions({ id: id! });
            case "users":
              return adminClient.realms.getUsersManagementPermissions({
                realm,
              });
            case "groups":
              return adminClient.groups.listPermissions({ id: id! });
            case "roles":
              return adminClient.roles.listPermissions({ id: id! });
            case "identityProviders":
              return adminClient.identityProviders.listPermissions({
                alias: id!,
              });
          }
        })(),
      ]),
    ([clients, permission]) => {
      setRealmId(clients[0]?.id!);
      setPermission(permission);
    },
    []
  );

  const [toggleDisableDialog, DisableConfirm] = useConfirmDialog({
    titleKey: "common:permissionsDisable",
    messageKey: "common:permissionsDisableConfirm",
    continueButtonLabel: "common:confirm",
    onConfirm: async () => {
      const permission = await togglePermissionEnabled(false);
      setPermission(permission);
    },
  });

  if (!permission) {
    return <KeycloakSpinner />;
  }

  return (
    <PageSection variant="light">
      <DisableConfirm />
      <Card isFlat>
        <CardTitle>{t("permissions")}</CardTitle>
        <CardBody>
          {t(`${type}PermissionsHint`)}
          <Form isHorizontal className="pf-u-pt-md">
            <FormGroup
              hasNoPaddingTop
              className="permission-label"
              label={t("permissionsEnabled")}
              fieldId="permissionsEnabled"
              labelIcon={
                <HelpItem
                  helpText="clients-help:permissionsEnabled"
                  fieldLabelId="clients:permissionsEnabled"
                />
              }
            >
              <Switch
                id="permissionsEnabled"
                data-testid="permissionSwitch"
                label={t("common:on")}
                labelOff={t("common:off")}
                isChecked={permission.enabled}
                onChange={async (enabled) => {
                  if (enabled) {
                    const permission = await togglePermissionEnabled(enabled);
                    setPermission(permission);
                  } else {
                    toggleDisableDialog();
                  }
                }}
                aria-label={t("permissionsEnabled")}
              />
            </FormGroup>
          </Form>
        </CardBody>
      </Card>
      {permission.enabled && (
        <>
          <Card isFlat className="pf-u-mt-lg">
            <CardTitle>{t("permissionsList")}</CardTitle>
            <CardBody>
              <Trans i18nKey="common:permissionsListIntro">
                {" "}
                <strong>
                  {{
                    realm:
                      realm === "master" ? "master-realm" : "realm-management",
                  }}
                </strong>
                .
              </Trans>
            </CardBody>
          </Card>
          <Card isFlat className="keycloak__permission__permission-table">
            <CardBody className="pf-u-p-0">
              <TableComposable
                aria-label={t("permissionsList")}
                variant="compact"
              >
                <Thead>
                  <Tr>
                    <Th id="permissionsScopeName">
                      {t("permissionsScopeName")}
                    </Th>
                    <Th id="description">{t("description")}</Th>
                  </Tr>
                </Thead>
                <Tbody>
                  {localeSort(
                    Object.entries(permission.scopePermissions || {}),
                    ([name]) => name
                  ).map(([name, id]) => (
                    <Tr key={id}>
                      <Td>
                        <Link
                          to={toPermissionDetails({
                            realm,
                            id: realmId,
                            permissionType: "scope",
                            permissionId: id,
                          })}
                        >
                          {name}
                        </Link>
                      </Td>
                      <Td>
                        {t(`scopePermissions.${type}.${name}-description`)}
                      </Td>
                      <Td isActionCell>
                        <ActionsColumn
                          items={[
                            {
                              title: t("common:edit"),
                              onClick() {
                                navigate(
                                  toPermissionDetails({
                                    realm,
                                    id: realmId,
                                    permissionType: "scope",
                                    permissionId: id,
                                  })
                                );
                              },
                            },
                          ]}
                        />
                      </Td>
                    </Tr>
                  ))}
                </Tbody>
              </TableComposable>
            </CardBody>
          </Card>
        </>
      )}
    </PageSection>
  );
};
