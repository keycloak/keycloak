import type ClientPolicyRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientPolicyRepresentation";
import type ClientProfileRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientProfileRepresentation";
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
import { PlusCircleIcon, TrashIcon } from "@patternfly/react-icons";
import { useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom-v5-compat";

import { useAlerts } from "../components/alert/Alerts";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { FormAccess } from "../components/form-access/FormAccess";
import { HelpItem } from "../components/help-enabler/HelpItem";
import { KeycloakSpinner } from "../components/keycloak-spinner/KeycloakSpinner";
import { KeycloakTextArea } from "../components/keycloak-text-area/KeycloakTextArea";
import { KeycloakTextInput } from "../components/keycloak-text-input/KeycloakTextInput";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useAdminClient, useFetch } from "../context/auth/AdminClient";
import { useRealm } from "../context/realm-context/RealmContext";
import { useServerInfo } from "../context/server-info/ServerInfoProvider";
import { AddClientProfileModal } from "./AddClientProfileModal";
import { toNewClientPolicyCondition } from "./routes/AddCondition";
import { toClientPolicies } from "./routes/ClientPolicies";
import { toClientProfile } from "./routes/ClientProfile";
import {
  EditClientPolicyParams,
  toEditClientPolicy,
} from "./routes/EditClientPolicy";
import { toEditClientPolicyCondition } from "./routes/EditCondition";

import { useParams } from "../utils/useParams";
import "./realm-settings-section.css";

type NewClientPolicyForm = Required<ClientPolicyRepresentation>;

const defaultValues: NewClientPolicyForm = {
  name: "",
  description: "",
  conditions: [],
  enabled: true,
  profiles: [],
};

type PolicyDetailAttributes = {
  idx: number;
  name: string;
};

export default function NewClientPolicyForm() {
  const { t } = useTranslation("realm-settings");
  const { realm } = useRealm();
  const { addAlert, addError } = useAlerts();
  const { adminClient } = useAdminClient();
  const [policies, setPolicies] = useState<ClientPolicyRepresentation[]>();
  const [clientProfiles, setClientProfiles] = useState<
    ClientProfileRepresentation[]
  >([]);

  const [currentPolicy, setCurrentPolicy] =
    useState<ClientPolicyRepresentation>();
  const [
    showAddConditionsAndProfilesForm,
    setShowAddConditionsAndProfilesForm,
  ] = useState(false);

  const [conditionToDelete, setConditionToDelete] =
    useState<PolicyDetailAttributes>();

  const [profilesModalOpen, setProfilesModalOpen] = useState(false);

  const [profileToDelete, setProfileToDelete] =
    useState<PolicyDetailAttributes>();

  const { policyName } = useParams<EditClientPolicyParams>();

  const navigate = useNavigate();
  const form = useForm<NewClientPolicyForm>({
    mode: "onChange",
    defaultValues,
  });
  const { handleSubmit } = form;

  const formValues = form.getValues();

  type ClientPoliciesHeaderProps = {
    onChange: (value: boolean) => void;
    value: boolean;
    save: () => void;
    realmName: string;
  };

  const ClientPoliciesHeader = ({
    save,
    onChange,
    value,
  }: ClientPoliciesHeaderProps) => {
    const { t } = useTranslation("realm-settings");

    const [toggleDisableDialog, DisableConfirm] = useConfirmDialog({
      titleKey: "realm-settings:disablePolicyConfirmTitle",
      messageKey: "realm-settings:disablePolicyConfirm",
      continueButtonLabel: "common:disable",
      onConfirm: () => {
        onChange(!value);
        save();
      },
    });

    if (!policies) {
      return <KeycloakSpinner />;
    }

    return (
      <>
        <DisableConfirm />
        <DeleteConfirm />
        <ViewHeader
          titleKey={
            showAddConditionsAndProfilesForm || policyName
              ? policyName
              : "realm-settings:createPolicy"
          }
          divider
          dropdownItems={
            showAddConditionsAndProfilesForm || policyName
              ? [
                  <DropdownItem
                    key="delete"
                    value="delete"
                    onClick={() => {
                      toggleDeleteDialog();
                    }}
                    data-testid="deleteClientPolicyDropdown"
                  >
                    {t("deleteClientPolicy")}
                  </DropdownItem>,
                ]
              : undefined
          }
          isEnabled={value}
          onToggle={(value) => {
            if (!value) {
              toggleDisableDialog();
            } else {
              onChange(value);
              save();
            }
          }}
        />
      </>
    );
  };

  useFetch(
    async () => {
      const [policies, profiles] = await Promise.all([
        adminClient.clientPolicies.listPolicies(),
        adminClient.clientPolicies.listProfiles({
          includeGlobalProfiles: true,
        }),
      ]);

      return { policies, profiles };
    },
    ({ policies, profiles }) => {
      const currentPolicy = policies.policies?.find(
        (item) => item.name === policyName
      );

      const allClientProfiles = [
        ...(profiles.globalProfiles ?? []),
        ...(profiles.profiles ?? []),
      ];

      setPolicies(policies.policies ?? []);
      if (currentPolicy) {
        setupForm(currentPolicy);
        setClientProfiles(allClientProfiles);
        setCurrentPolicy(currentPolicy);
        setShowAddConditionsAndProfilesForm(true);
      }
    },
    []
  );

  const setupForm = (policy: ClientPolicyRepresentation) => {
    form.reset();
    Object.entries(policy).map(([key, value]) => {
      form.setValue(key, value);
    });
  };

  const policy = (policies || []).filter(
    (policy) => policy.name === policyName
  );
  const policyConditions = policy[0]?.conditions || [];
  const policyProfiles = policy[0]?.profiles || [];

  const serverInfo = useServerInfo();

  const conditionTypes =
    serverInfo.componentTypes?.[
      "org.keycloak.services.clientpolicy.condition.ClientPolicyConditionProvider"
    ];

  const save = async () => {
    const createdForm = form.getValues();
    const createdPolicy = {
      ...createdForm,
      profiles: [],
      conditions: [],
    };

    const getAllPolicies = () => {
      const policyNameExists = policies?.some(
        (policy) => policy.name === createdPolicy.name
      );

      if (policyNameExists) {
        return policies?.map((policy) =>
          policy.name === createdPolicy.name ? createdPolicy : policy
        );
      } else if (createdForm.name !== policyName) {
        return policies
          ?.filter((item) => item.name !== policyName)
          .concat(createdForm);
      }
      return policies?.concat(createdForm);
    };

    try {
      await adminClient.clientPolicies.updatePolicy({
        policies: getAllPolicies(),
      });
      addAlert(
        policyName
          ? t("realm-settings:updateClientPolicySuccess")
          : t("realm-settings:createClientPolicySuccess"),
        AlertVariant.success
      );
      navigate(toEditClientPolicy({ realm, policyName: createdForm.name! }));
      setShowAddConditionsAndProfilesForm(true);
    } catch (error) {
      addError("realm-settings:createClientPolicyError", error);
    }
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: t("deleteClientPolicyConfirmTitle"),
    messageKey: t("deleteClientPolicyConfirm", {
      policyName: policyName,
    }),
    continueButtonLabel: t("delete"),
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      const updatedPolicies = policies?.filter(
        (policy) => policy.name !== policyName
      );

      try {
        await adminClient.clientPolicies.updatePolicy({
          policies: updatedPolicies,
        });
        addAlert(t("deleteClientPolicySuccess"), AlertVariant.success);
        navigate(
          toClientPolicies({
            realm,
            tab: "policies",
          })
        );
      } catch (error) {
        addError(t("deleteClientPolicyError"), error);
      }
    },
  });

  const [toggleDeleteConditionDialog, DeleteConditionConfirm] =
    useConfirmDialog({
      titleKey: t("deleteClientPolicyConditionConfirmTitle"),
      messageKey: t("deleteClientPolicyConditionConfirm", {
        condition: conditionToDelete?.name,
      }),
      continueButtonLabel: t("delete"),
      continueButtonVariant: ButtonVariant.danger,
      onConfirm: async () => {
        if (conditionToDelete?.name) {
          currentPolicy?.conditions?.splice(conditionToDelete.idx!, 1);
          try {
            await adminClient.clientPolicies.updatePolicy({
              policies: policies,
            });
            addAlert(t("deleteConditionSuccess"), AlertVariant.success);
            navigate(
              toEditClientPolicy({ realm, policyName: formValues.name! })
            );
          } catch (error) {
            addError(t("deleteConditionError"), error);
          }
        } else {
          const updatedPolicies = policies?.filter(
            (policy) => policy.name !== policyName
          );

          try {
            await adminClient.clientPolicies.updatePolicy({
              policies: updatedPolicies,
            });
            addAlert(t("deleteClientSuccess"), AlertVariant.success);
            navigate(
              toClientPolicies({
                realm,
                tab: "policies",
              })
            );
          } catch (error) {
            addError(t("deleteClientError"), error);
          }
        }
      },
    });

  const [toggleDeleteProfileDialog, DeleteProfileConfirm] = useConfirmDialog({
    titleKey: t("deleteClientPolicyProfileConfirmTitle"),
    messageKey: t("deleteClientPolicyProfileConfirm", {
      profileName: profileToDelete?.name,
      policyName,
    }),
    continueButtonLabel: t("delete"),
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      if (profileToDelete?.name) {
        currentPolicy?.profiles?.splice(profileToDelete.idx!, 1);
        try {
          await adminClient.clientPolicies.updatePolicy({
            policies: policies,
          });
          addAlert(t("deleteClientPolicyProfileSuccess"), AlertVariant.success);
          navigate(toEditClientPolicy({ realm, policyName: formValues.name! }));
        } catch (error) {
          addError(t("deleteClientPolicyProfileError"), error);
        }
      } else {
        const updatedPolicies = policies?.filter(
          (policy) => policy.name !== policyName
        );

        try {
          await adminClient.clientPolicies.updatePolicy({
            policies: updatedPolicies,
          });
          addAlert(t("deleteClientSuccess"), AlertVariant.success);
          navigate(
            toClientPolicies({
              realm,
              tab: "policies",
            })
          );
        } catch (error) {
          addError(t("deleteClientError"), error);
        }
      }
    },
  });

  const reset = () => {
    form.setValue("name", currentPolicy?.name);
    form.setValue("description", currentPolicy?.description);
  };

  const toggleModal = () => {
    setProfilesModalOpen(!profilesModalOpen);
  };

  const addProfiles = async (profiles: string[]) => {
    const createdPolicy = {
      ...currentPolicy,
      profiles: policyProfiles.concat(profiles),
      conditions: currentPolicy?.conditions,
    };

    const index = policies?.findIndex(
      (policy) => createdPolicy.name === policy.name
    );

    if (index === undefined || index === -1) {
      return;
    }

    const newPolicies = [
      ...(policies || []).slice(0, index),
      createdPolicy,
      ...(policies || []).slice(index + 1),
    ];

    try {
      await adminClient.clientPolicies.updatePolicy({
        policies: newPolicies,
      });
      setPolicies(newPolicies);
      navigate(toEditClientPolicy({ realm, policyName: formValues.name! }));
      addAlert(
        t("realm-settings:addClientProfileSuccess"),
        AlertVariant.success
      );
    } catch (error) {
      addError("realm-settings:addClientProfileError", error);
    }
  };

  return (
    <>
      <DeleteConditionConfirm />
      <DeleteProfileConfirm />
      <AddClientProfileModal
        onConfirm={(profiles: ClientProfileRepresentation[]) => {
          addProfiles(profiles.map((item) => item.name!));
        }}
        allProfiles={policyProfiles}
        open={profilesModalOpen}
        toggleDialog={toggleModal}
      />
      <Controller
        name="enabled"
        defaultValue={true}
        control={form.control}
        render={({ onChange, value }) => (
          <ClientPoliciesHeader
            value={value}
            onChange={onChange}
            realmName={realm}
            save={save}
          />
        )}
      />
      <PageSection variant="light">
        <FormAccess
          onSubmit={handleSubmit(save)}
          isHorizontal
          role="view-realm"
          className="pf-u-mt-lg"
        >
          <FormGroup
            label={t("common:name")}
            fieldId="kc-client-profile-name"
            isRequired
            helperTextInvalid={form.errors.name?.message}
            validated={
              form.errors.name
                ? ValidatedOptions.error
                : ValidatedOptions.default
            }
          >
            <KeycloakTextInput
              ref={form.register({
                required: { value: true, message: t("common:required") },
                validate: (value) =>
                  policies?.some((policy) => policy.name === value)
                    ? t("createClientProfileNameHelperText").toString()
                    : true,
              })}
              type="text"
              id="kc-client-profile-name"
              name="name"
              data-testid="client-policy-name"
              validated={
                form.errors.name
                  ? ValidatedOptions.error
                  : ValidatedOptions.default
              }
            />
          </FormGroup>
          <FormGroup label={t("common:description")} fieldId="kc-description">
            <KeycloakTextArea
              name="description"
              aria-label={t("description")}
              ref={form.register()}
              type="text"
              id="kc-client-policy-description"
              data-testid="client-policy-description"
            />
          </FormGroup>
          <ActionGroup>
            <Button
              variant="primary"
              type="submit"
              data-testid="saveCreatePolicy"
              isDisabled={!form.formState.isValid}
            >
              {t("common:save")}
            </Button>
            <Button
              id="cancelCreatePolicy"
              variant="link"
              onClick={() =>
                showAddConditionsAndProfilesForm || policyName
                  ? reset()
                  : navigate(
                      toClientPolicies({
                        realm,
                        tab: "policies",
                      })
                    )
              }
              data-testid="cancelCreatePolicy"
            >
              {showAddConditionsAndProfilesForm
                ? t("common:reload")
                : t("common:cancel")}
            </Button>
          </ActionGroup>
          {(showAddConditionsAndProfilesForm || form.formState.isSubmitted) && (
            <>
              <Flex>
                <FlexItem>
                  <Text className="kc-conditions" component={TextVariants.h1}>
                    {t("conditions")}
                    <HelpItem
                      helpText="realm-settings-help:conditions"
                      fieldLabelId="realm-settings:conditions"
                    />
                  </Text>
                </FlexItem>
                <FlexItem align={{ default: "alignRight" }}>
                  <Button
                    id="addCondition"
                    component={(props) => (
                      <Link
                        {...props}
                        to={toNewClientPolicyCondition({
                          realm,
                          policyName: policyName!,
                        })}
                      ></Link>
                    )}
                    variant="link"
                    className="kc-addCondition"
                    data-testid="addCondition"
                    icon={<PlusCircleIcon />}
                  >
                    {t("realm-settings:addCondition")}
                  </Button>
                </FlexItem>
              </Flex>
              {policyConditions.length > 0 ? (
                <DataList aria-label={t("conditions")} isCompact>
                  {policyConditions.map((condition, idx) => (
                    <DataListItem
                      aria-labelledby="conditions-list-item"
                      key={`list-item-${idx}`}
                      id={condition.condition}
                      data-testid="conditions-list-item"
                    >
                      <DataListItemRow data-testid="conditions-list-row">
                        <DataListItemCells
                          dataListCells={[
                            <DataListCell
                              key={`name-${idx}`}
                              data-testid="condition-type"
                            >
                              {Object.keys(condition.configuration!).length !==
                              0 ? (
                                <Link
                                  key={condition.condition}
                                  data-testid={`${condition.condition}-condition-link`}
                                  to={toEditClientPolicyCondition({
                                    realm,
                                    conditionName: condition.condition!,
                                    policyName: policyName,
                                  })}
                                  className="kc-condition-link"
                                >
                                  {condition.condition}
                                </Link>
                              ) : (
                                condition.condition
                              )}
                              {conditionTypes?.map(
                                (type) =>
                                  type.id === condition.condition && (
                                    <>
                                      <HelpItem
                                        helpText={type.helpText}
                                        fieldLabelId={condition.condition}
                                      />
                                      <Button
                                        variant="link"
                                        isInline
                                        icon={
                                          <TrashIcon
                                            className="kc-conditionType-trash-icon"
                                            data-testid={`delete-${condition.condition}-condition`}
                                            onClick={() => {
                                              toggleDeleteConditionDialog();
                                              setConditionToDelete({
                                                idx: idx,
                                                name: type.id!,
                                              });
                                            }}
                                          />
                                        }
                                      ></Button>
                                    </>
                                  )
                              )}
                            </DataListCell>,
                          ]}
                        />
                      </DataListItemRow>
                    </DataListItem>
                  ))}
                </DataList>
              ) : (
                <>
                  <Divider />
                  <Text
                    className="kc-emptyConditions"
                    component={TextVariants.h6}
                  >
                    {t("realm-settings:emptyConditions")}
                  </Text>
                </>
              )}
            </>
          )}
          {(showAddConditionsAndProfilesForm || form.formState.isSubmitted) && (
            <>
              <Flex>
                <FlexItem>
                  <Text
                    className="kc-client-profiles"
                    component={TextVariants.h1}
                  >
                    {t("clientProfiles")}
                    <HelpItem
                      helpText="realm-settings-help:clientProfiles"
                      fieldLabelId="realm-settings:clientProfiles"
                    />
                  </Text>
                </FlexItem>
                <FlexItem align={{ default: "alignRight" }}>
                  <Button
                    id="addClientProfile"
                    variant="link"
                    className="kc-addClientProfile"
                    data-testid="addClientProfile"
                    icon={<PlusCircleIcon />}
                    onClick={toggleModal}
                  >
                    {t("realm-settings:addClientProfile")}
                  </Button>
                </FlexItem>
              </Flex>
              {policyProfiles.length > 0 ? (
                <DataList aria-label={t("profiles")} isCompact>
                  {policyProfiles.map((profile, idx) => (
                    <DataListItem
                      aria-labelledby={`${profile}-profile-list-item`}
                      key={profile}
                      id={`${profile}-profile-list-item`}
                      data-testid={"profile-list-item"}
                    >
                      <DataListItemRow data-testid="profile-list-row">
                        <DataListItemCells
                          dataListCells={[
                            <DataListCell key="name" data-testid="profile-name">
                              {profile && (
                                <Link
                                  key={profile}
                                  data-testid="profile-name-link"
                                  to={toClientProfile({
                                    realm,
                                    profileName: profile,
                                  })}
                                  className="kc-profile-link"
                                >
                                  {profile}
                                </Link>
                              )}
                              {policyProfiles
                                .filter((type) => type === profile)
                                .map((type) => (
                                  <>
                                    <HelpItem
                                      helpText={
                                        clientProfiles.find(
                                          (profile) => type === profile.name
                                        )?.description
                                      }
                                      fieldLabelId={profile}
                                    />
                                    <Button
                                      variant="link"
                                      isInline
                                      icon={
                                        <TrashIcon
                                          className="kc-conditionType-trash-icon"
                                          data-testid="deleteClientProfileDropdown"
                                          onClick={() => {
                                            toggleDeleteProfileDialog();
                                            setProfileToDelete({
                                              idx: idx,
                                              name: type!,
                                            });
                                          }}
                                        />
                                      }
                                    ></Button>
                                  </>
                                ))}
                            </DataListCell>,
                          ]}
                        />
                      </DataListItemRow>
                    </DataListItem>
                  ))}
                </DataList>
              ) : (
                <>
                  <Divider />
                  <Text
                    className="kc-emptyClientProfiles"
                    component={TextVariants.h6}
                  >
                    {t("realm-settings:emptyProfiles")}
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
