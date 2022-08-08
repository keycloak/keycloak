import { Fragment, useEffect, useMemo, useState } from "react";
import {
  ActionGroup,
  AlertVariant,
  Button,
  ButtonVariant,
  DataList,
  DataListCell,
  DataListItem,
  DataListItemCells,
  DataListItemRow,
  Divider,
  DropdownItem,
  Flex,
  FlexItem,
  FormGroup,
  PageSection,
  Text,
  TextVariants,
  ValidatedOptions,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { useForm } from "react-hook-form";
import { FormAccess } from "../components/form-access/FormAccess";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { Link, useHistory, useParams } from "react-router-dom";
import { useAlerts } from "../components/alert/Alerts";
import { useAdminClient, useFetch } from "../context/auth/AdminClient";
import type ClientProfileRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientProfileRepresentation";
import { HelpItem } from "../components/help-enabler/HelpItem";
import { KeycloakTextInput } from "../components/keycloak-text-input/KeycloakTextInput";
import { KeycloakTextArea } from "../components/keycloak-text-area/KeycloakTextArea";
import { PlusCircleIcon, TrashIcon } from "@patternfly/react-icons";
import "./realm-settings-section.css";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { toAddExecutor } from "./routes/AddExecutor";
import { useServerInfo } from "../context/server-info/ServerInfoProvider";
import { ClientProfileParams, toClientProfile } from "./routes/ClientProfile";
import { toExecutor } from "./routes/Executor";
import { toClientPolicies } from "./routes/ClientPolicies";

type ClientProfileForm = Required<ClientProfileRepresentation>;

const defaultValues: ClientProfileForm = {
  name: "",
  description: "",
  executors: [],
};

export default function ClientProfileForm() {
  const { t } = useTranslation("realm-settings");
  const history = useHistory();
  const {
    handleSubmit,
    setValue,
    register,
    formState: { isDirty, errors },
  } = useForm<ClientProfileForm>({
    defaultValues,
    mode: "onChange",
  });

  const { addAlert, addError } = useAlerts();
  const { adminClient } = useAdminClient();
  const [globalProfiles, setGlobalProfiles] = useState<
    ClientProfileRepresentation[]
  >([]);
  const [profiles, setProfiles] = useState<ClientProfileRepresentation[]>([]);
  const { realm, profileName } = useParams<ClientProfileParams>();
  const serverInfo = useServerInfo();
  const executorTypes = useMemo(
    () =>
      serverInfo.componentTypes?.[
        "org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProvider"
      ],
    []
  );
  const [executorToDelete, setExecutorToDelete] = useState<{
    idx: number;
    name: string;
  }>();
  const editMode = profileName ? true : false;
  const [key, setKey] = useState(0);
  const reload = () => setKey(new Date().getTime());

  useFetch(
    () =>
      adminClient.clientPolicies.listProfiles({ includeGlobalProfiles: true }),
    (profiles) => {
      setGlobalProfiles(profiles.globalProfiles ?? []);
      setProfiles(profiles.profiles ?? []);
    },
    [key]
  );

  const save = async (form: ClientProfileForm) => {
    const updatedProfiles = editMode ? patchProfiles(form) : addProfile(form);

    try {
      await adminClient.clientPolicies.createProfiles({
        profiles: updatedProfiles,
        globalProfiles: globalProfiles,
      });

      addAlert(
        editMode
          ? t("realm-settings:updateClientProfileSuccess")
          : t("realm-settings:createClientProfileSuccess"),
        AlertVariant.success
      );

      history.push(toClientProfile({ realm, profileName: form.name }));
    } catch (error) {
      addError(
        editMode
          ? "realm-settings:updateClientProfileError"
          : "realm-settings:createClientProfileError",
        error
      );
    }
  };

  const patchProfiles = (data: ClientProfileRepresentation) =>
    profiles.map((profile) => {
      if (profile.name !== profileName) {
        return profile;
      }

      return {
        ...profile,
        name: data.name,
        description: data.description,
      };
    });

  const addProfile = (data: ClientProfileRepresentation) =>
    profiles.concat({
      ...data,
      executors: [],
    });

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: executorToDelete?.name!
      ? t("deleteExecutorProfileConfirmTitle")
      : t("deleteClientProfileConfirmTitle"),
    messageKey: executorToDelete?.name!
      ? t("deleteExecutorProfileConfirm", {
          executorName: executorToDelete.name!,
        })
      : t("deleteClientProfileConfirm", {
          profileName,
        }),
    continueButtonLabel: t("delete"),
    continueButtonVariant: ButtonVariant.danger,

    onConfirm: async () => {
      if (executorToDelete?.name!) {
        profileExecutors.splice(executorToDelete.idx!, 1);
        try {
          await adminClient.clientPolicies.createProfiles({
            profiles: profiles,
            globalProfiles,
          });
          addAlert(t("deleteExecutorSuccess"), AlertVariant.success);
          history.push(toClientProfile({ realm, profileName }));
        } catch (error) {
          addError(t("deleteExecutorError"), error);
        }
      } else {
        const updatedProfiles = profiles.filter(
          (profile) => profile.name !== profileName
        );

        try {
          await adminClient.clientPolicies.createProfiles({
            profiles: updatedProfiles,
            globalProfiles,
          });
          addAlert(t("deleteClientSuccess"), AlertVariant.success);
          history.push(toClientPolicies({ realm, tab: "profiles" }));
        } catch (error) {
          addError(t("deleteClientError"), error);
        }
      }
    },
  });

  const profile = profiles.find((profile) => profile.name === profileName);
  const profileExecutors = profile?.executors || [];
  const globalProfile = globalProfiles.find(
    (globalProfile) => globalProfile.name === profileName
  );
  const globalProfileExecutors = globalProfile?.executors || [];

  useEffect(() => {
    setValue("name", globalProfile?.name ?? profile?.name);
    setValue("description", globalProfile?.description ?? profile?.description);
  }, [profiles]);

  return (
    <>
      <DeleteConfirm />
      <ViewHeader
        titleKey={editMode ? profileName : t("newClientProfile")}
        badges={[
          {
            id: "global-client-profile-badge",
            text: globalProfile ? t("global") : "",
          },
        ]}
        divider
        dropdownItems={
          editMode && !globalProfile
            ? [
                <DropdownItem
                  key="delete"
                  value="delete"
                  onClick={toggleDeleteDialog}
                  data-testid="deleteClientProfileDropdown"
                >
                  {t("deleteClientProfile")}
                </DropdownItem>,
              ]
            : undefined
        }
      />
      <PageSection variant="light">
        <FormAccess isHorizontal role="view-realm" className="pf-u-mt-lg">
          <FormGroup
            label={t("newClientProfileName")}
            fieldId="kc-name"
            helperText={t("createClientProfileNameHelperText")}
            isRequired
            helperTextInvalid={t("common:required")}
            validated={
              errors.name ? ValidatedOptions.error : ValidatedOptions.default
            }
          >
            <KeycloakTextInput
              ref={register({ required: true })}
              name="name"
              type="text"
              id="name"
              aria-label={t("name")}
              data-testid="client-profile-name"
              isReadOnly={!!globalProfile}
            />
          </FormGroup>
          <FormGroup label={t("common:description")} fieldId="kc-description">
            <KeycloakTextArea
              ref={register()}
              name="description"
              type="text"
              id="description"
              aria-label={t("description")}
              data-testid="client-profile-description"
              isReadOnly={!!globalProfile}
            />
          </FormGroup>
          <ActionGroup>
            {!globalProfile && (
              <Button
                variant="primary"
                onClick={() => handleSubmit(save)()}
                data-testid="saveCreateProfile"
                isDisabled={!isDirty}
              >
                {t("common:save")}
              </Button>
            )}
            {editMode && !globalProfile && (
              <Button
                id={"reloadProfile"}
                variant="link"
                data-testid={"reloadProfile"}
                isDisabled={!isDirty}
                onClick={reload}
              >
                {t("realm-settings:reload")}
              </Button>
            )}
            {!editMode && !globalProfile && (
              <Button
                id={"cancelCreateProfile"}
                variant="link"
                component={(props) => (
                  <Link
                    {...props}
                    to={toClientPolicies({ realm, tab: "profiles" })}
                  />
                )}
                data-testid={"cancelCreateProfile"}
              >
                {t("common:cancel")}
              </Button>
            )}
          </ActionGroup>
          {editMode && (
            <>
              <Flex>
                <FlexItem>
                  <Text className="kc-executors" component={TextVariants.h1}>
                    {t("executors")}
                    <HelpItem
                      helpText="realm-settings:executorsHelpText"
                      fieldLabelId="realm-settings:executors"
                    />
                  </Text>
                </FlexItem>
                {profile && (
                  <FlexItem align={{ default: "alignRight" }}>
                    <Button
                      id="addExecutor"
                      component={(props) => (
                        <Link
                          {...props}
                          to={toAddExecutor({
                            realm,
                            profileName,
                          })}
                        />
                      )}
                      variant="link"
                      className="kc-addExecutor"
                      data-testid="addExecutor"
                      icon={<PlusCircleIcon />}
                    >
                      {t("realm-settings:addExecutor")}
                    </Button>
                  </FlexItem>
                )}
              </Flex>
              {profileExecutors.length > 0 && (
                <DataList aria-label={t("executors")} isCompact>
                  {profileExecutors.map((executor, idx) => (
                    <DataListItem
                      aria-labelledby={"executors-list-item"}
                      key={executor.executor}
                      id={executor.executor}
                    >
                      <DataListItemRow data-testid="executors-list-row">
                        <DataListItemCells
                          dataListCells={[
                            <DataListCell
                              key="executor"
                              data-testid="executor-type"
                            >
                              {executor.configuration ? (
                                <Button
                                  component={(props) => (
                                    <Link
                                      {...props}
                                      to={toExecutor({
                                        realm,
                                        profileName,
                                        executorName: executor.executor!,
                                      })}
                                    />
                                  )}
                                  variant="link"
                                  data-testid="editExecutor"
                                >
                                  {executor.executor}
                                </Button>
                              ) : (
                                <span className="kc-unclickable-executor">
                                  {executor.executor}
                                </span>
                              )}
                              {executorTypes
                                ?.filter(
                                  (type) => type.id === executor.executor
                                )
                                .map((type) => (
                                  <Fragment key={type.id}>
                                    <HelpItem
                                      key={type.id}
                                      helpText={type.helpText}
                                      fieldLabelId="realm-settings:executorTypeTextHelpText"
                                    />
                                    <Button
                                      variant="link"
                                      isInline
                                      icon={
                                        <TrashIcon
                                          key={`executorType-trash-icon-${type.id}`}
                                          className="kc-executor-trash-icon"
                                          data-testid="deleteExecutor"
                                        />
                                      }
                                      onClick={() => {
                                        toggleDeleteDialog();
                                        setExecutorToDelete({
                                          idx: idx,
                                          name: type.id,
                                        });
                                      }}
                                    ></Button>
                                  </Fragment>
                                ))}
                            </DataListCell>,
                          ]}
                        />
                      </DataListItemRow>
                    </DataListItem>
                  ))}
                </DataList>
              )}
              {globalProfileExecutors.length > 0 && (
                <>
                  <DataList aria-label={t("executors")} isCompact>
                    {globalProfileExecutors.map((executor) => (
                      <DataListItem
                        aria-labelledby={"global-executors-list-item"}
                        key={executor.executor}
                        id={executor.executor}
                      >
                        <DataListItemRow data-testid="global-executors-list-row">
                          <DataListItemCells
                            dataListCells={[
                              <DataListCell
                                key="executor"
                                data-testid="global-executor-type"
                              >
                                {Object.keys(executor.configuration!).length !==
                                0 ? (
                                  <Button
                                    component={(props) => (
                                      <Link
                                        {...props}
                                        to={toExecutor({
                                          realm,
                                          profileName,
                                          executorName: executor.executor!,
                                        })}
                                      />
                                    )}
                                    variant="link"
                                    data-testid="editExecutor"
                                  >
                                    {executor.executor}
                                  </Button>
                                ) : (
                                  <span className="kc-unclickable-executor">
                                    {executor.executor}
                                  </span>
                                )}
                                {executorTypes
                                  ?.filter(
                                    (type) => type.id === executor.executor
                                  )
                                  .map((type) => (
                                    <HelpItem
                                      key={type.id}
                                      helpText={type.helpText}
                                      fieldLabelId="realm-settings:executorTypeTextHelpText"
                                    />
                                  ))}
                              </DataListCell>,
                            ]}
                          />
                        </DataListItemRow>
                      </DataListItem>
                    ))}
                  </DataList>
                  <Button
                    id="backToClientPolicies"
                    component={(props) => (
                      <Link
                        {...props}
                        to={toClientPolicies({ realm, tab: "profiles" })}
                      />
                    )}
                    variant="primary"
                    className="kc-backToPolicies"
                    data-testid="backToClientPolicies"
                  >
                    {t("realm-settings:back")}
                  </Button>
                </>
              )}
              {profileExecutors.length === 0 &&
                globalProfileExecutors.length === 0 && (
                  <>
                    <Divider />
                    <Text
                      className="kc-emptyExecutors"
                      component={TextVariants.h6}
                    >
                      {t("realm-settings:emptyExecutors")}
                    </Text>
                  </>
                )}
            </>
          )}
        </FormAccess>
      </PageSection>
    </>
  );
}
