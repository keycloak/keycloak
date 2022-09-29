import { useState } from "react";
import {
  ActionGroup,
  AlertVariant,
  Button,
  Chip,
  ChipGroup,
  FormGroup,
  InputGroup,
  Select,
  SelectOption,
  Switch,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { Controller, useFormContext } from "react-hook-form";
import { useNavigate } from "react-router-dom-v5-compat";

import { FormAccess } from "../components/form-access/FormAccess";
import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import { HelpItem } from "../components/help-enabler/HelpItem";
import { useRealm } from "../context/realm-context/RealmContext";
import { useAdminClient, useFetch } from "../context/auth/AdminClient";
import type GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";
import { useAlerts } from "../components/alert/Alerts";
import { KeycloakTextInput } from "../components/keycloak-text-input/KeycloakTextInput";
import { emailRegexPattern } from "../util";
import useFormatDate from "../utils/useFormatDate";
import { GroupPickerDialog } from "../components/group/GroupPickerDialog";
import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import type RequiredActionProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/requiredActionProviderRepresentation";
import { useAccess } from "../context/access/Access";

export type BruteForced = {
  isBruteForceProtected?: boolean;
  isLocked?: boolean;
};

export type UserFormProps = {
  user?: UserRepresentation;
  bruteForce?: BruteForced;
  save: (user: UserRepresentation) => void;
  onGroupsUpdate: (groups: GroupRepresentation[]) => void;
};

export const UserForm = ({
  user,
  bruteForce: { isBruteForceProtected, isLocked } = {
    isBruteForceProtected: false,
    isLocked: false,
  },
  save,
  onGroupsUpdate,
}: UserFormProps) => {
  const { t } = useTranslation("users");
  const { realm: realmName } = useRealm();
  const formatDate = useFormatDate();

  const [
    isRequiredUserActionsDropdownOpen,
    setRequiredUserActionsDropdownOpen,
  ] = useState(false);
  const navigate = useNavigate();
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const { hasAccess } = useAccess();
  const isManager = hasAccess("manage-users");

  const {
    handleSubmit,
    register,
    watch,
    control,
    reset,
    formState: { errors },
  } = useFormContext();
  const watchUsernameInput = watch("username");
  const [selectedGroups, setSelectedGroups] = useState<GroupRepresentation[]>(
    []
  );
  const [open, setOpen] = useState(false);
  const [locked, setLocked] = useState(isLocked);
  const [realm, setRealm] = useState<RealmRepresentation>();
  const [requiredActions, setRequiredActions] = useState<
    RequiredActionProviderRepresentation[]
  >([]);

  useFetch(
    () =>
      Promise.all([
        adminClient.realms.findOne({ realm: realmName }),
        adminClient.authenticationManagement.getRequiredActions(),
      ]),
    ([realm, actions]) => {
      if (!realm) {
        throw new Error(t("common:notFound"));
      }
      setRealm(realm);
      setRequiredActions(actions);
    },
    []
  );

  const unLockUser = async () => {
    try {
      await adminClient.attackDetection.del({ id: user!.id! });
      addAlert(t("unlockSuccess"), AlertVariant.success);
    } catch (error) {
      addError("users:unlockError", error);
    }
  };

  const clearSelection = () => {
    setRequiredUserActionsDropdownOpen(false);
  };

  const deleteItem = (id: string) => {
    setSelectedGroups(selectedGroups.filter((item) => item.name !== id));
    onGroupsUpdate(selectedGroups);
  };

  const addChips = async (groups: GroupRepresentation[]): Promise<void> => {
    setSelectedGroups([...selectedGroups!, ...groups]);
    onGroupsUpdate([...selectedGroups!, ...groups]);
  };

  const addGroups = async (groups: GroupRepresentation[]): Promise<void> => {
    const newGroups = groups;

    newGroups.forEach(async (group) => {
      try {
        await adminClient.users.addToGroup({
          id: user!.id!,
          groupId: group.id!,
        });
        addAlert(t("users:addedGroupMembership"), AlertVariant.success);
      } catch (error) {
        addError("users:addedGroupMembershipError", error);
      }
    });
  };

  const toggleModal = () => {
    setOpen(!open);
  };

  return (
    <FormAccess
      isHorizontal
      onSubmit={handleSubmit(save)}
      role="query-users"
      fineGrainedAccess={user?.access?.manage}
      className="pf-u-mt-lg"
    >
      {open && (
        <GroupPickerDialog
          type="selectMany"
          text={{
            title: "users:selectGroups",
            ok: "users:join",
          }}
          canBrowse={isManager}
          onConfirm={(groups) => {
            user?.id ? addGroups(groups || []) : addChips(groups || []);
            setOpen(false);
          }}
          onClose={() => setOpen(false)}
          filterGroups={selectedGroups.map((group) => group.name!)}
        />
      )}
      {user?.id && (
        <>
          <FormGroup label={t("common:id")} fieldId="kc-id" isRequired>
            <KeycloakTextInput
              id={user.id}
              aria-label={t("userID")}
              value={user.id}
              type="text"
              isReadOnly
            />
          </FormGroup>
          <FormGroup label={t("createdAt")} fieldId="kc-created-at" isRequired>
            <KeycloakTextInput
              value={formatDate(new Date(user.createdTimestamp!))}
              type="text"
              id="kc-created-at"
              aria-label={t("createdAt")}
              name="createdTimestamp"
              isReadOnly
            />
          </FormGroup>
        </>
      )}
      {!realm?.registrationEmailAsUsername && (
        <FormGroup
          label={t("username")}
          fieldId="kc-username"
          isRequired
          validated={errors.username ? "error" : "default"}
          helperTextInvalid={t("common:required")}
        >
          <KeycloakTextInput
            ref={register()}
            type="text"
            id="kc-username"
            aria-label={t("username")}
            name="username"
            isReadOnly={
              !!user?.id &&
              !realm?.editUsernameAllowed &&
              realm?.editUsernameAllowed !== undefined
            }
          />
        </FormGroup>
      )}
      <FormGroup
        label={t("email")}
        fieldId="kc-description"
        validated={errors.email ? "error" : "default"}
        helperTextInvalid={t("users:emailInvalid")}
      >
        <KeycloakTextInput
          ref={register({
            pattern: emailRegexPattern,
          })}
          type="email"
          id="kc-email"
          name="email"
          data-testid="email-input"
          aria-label={t("emailInput")}
        />
      </FormGroup>
      <FormGroup
        label={t("emailVerified")}
        fieldId="kc-email-verified"
        helperTextInvalid={t("common:required")}
        labelIcon={
          <HelpItem
            helpText="users-help:emailVerified"
            fieldLabelId="users:emailVerified"
          />
        }
      >
        <Controller
          name="emailVerified"
          defaultValue={false}
          control={control}
          render={({ onChange, value }) => (
            <Switch
              data-testid="email-verified-switch"
              id={"kc-user-email-verified"}
              isDisabled={false}
              onChange={(value) => onChange(value)}
              isChecked={value}
              label={t("common:on")}
              labelOff={t("common:off")}
              aria-label={t("emailVerified")}
            />
          )}
        />
      </FormGroup>
      <FormGroup
        label={t("firstName")}
        fieldId="kc-firstname"
        validated={errors.firstName ? "error" : "default"}
        helperTextInvalid={t("common:required")}
      >
        <KeycloakTextInput
          ref={register()}
          data-testid="firstName-input"
          type="text"
          id="kc-firstname"
          aria-label={t("firstName")}
          name="firstName"
        />
      </FormGroup>
      <FormGroup
        label={t("lastName")}
        fieldId="kc-name"
        validated={errors.lastName ? "error" : "default"}
      >
        <KeycloakTextInput
          ref={register()}
          data-testid="lastName-input"
          type="text"
          id="kc-lastname"
          name="lastName"
          aria-label={t("lastName")}
        />
      </FormGroup>
      {isBruteForceProtected && (
        <FormGroup
          label={t("temporaryLocked")}
          fieldId="temporaryLocked"
          labelIcon={
            <HelpItem
              helpText="users-help:temporaryLocked"
              fieldLabelId="users:temporaryLocked"
            />
          }
        >
          <Switch
            data-testid="user-locked-switch"
            id={"temporaryLocked"}
            onChange={(value) => {
              unLockUser();
              setLocked(value);
            }}
            isChecked={locked}
            isDisabled={!locked}
            label={t("common:on")}
            labelOff={t("common:off")}
            aria-label={t("temporaryLocked")}
          />
        </FormGroup>
      )}
      <FormGroup
        label={t("requiredUserActions")}
        fieldId="kc-required-user-actions"
        validated={errors.requiredActions ? "error" : "default"}
        helperTextInvalid={t("common:required")}
        labelIcon={
          <HelpItem
            helpText="users-help:requiredUserActions"
            fieldLabelId="users:requiredUserActions"
          />
        }
      >
        <Controller
          name="requiredActions"
          defaultValue={[]}
          typeAheadAriaLabel="Select an action"
          control={control}
          render={({ onChange, value }) => (
            <Select
              data-testid="required-actions-select"
              placeholderText="Select action"
              toggleId="kc-required-user-actions"
              onToggle={() =>
                setRequiredUserActionsDropdownOpen(
                  !isRequiredUserActionsDropdownOpen
                )
              }
              isOpen={isRequiredUserActionsDropdownOpen}
              selections={value}
              onSelect={(_, v) => {
                const option = v as string;
                if (value.includes(option)) {
                  onChange(value.filter((item: string) => item !== option));
                } else {
                  onChange([...value, option]);
                }
              }}
              onClear={clearSelection}
              variant="typeaheadmulti"
            >
              {requiredActions.map(({ alias, name }) => (
                <SelectOption key={alias} value={alias}>
                  {name}
                </SelectOption>
              ))}
            </Select>
          )}
        />
      </FormGroup>
      {!user?.id && (
        <FormGroup
          label={t("common:groups")}
          fieldId="kc-groups"
          validated={errors.requiredActions ? "error" : "default"}
          helperTextInvalid={t("common:required")}
          labelIcon={
            <HelpItem helpText="users-help:groups" fieldLabelId="groups" />
          }
        >
          <Controller
            name="groups"
            defaultValue={[]}
            typeAheadAriaLabel="Select an action"
            control={control}
            render={() => (
              <InputGroup>
                <ChipGroup categoryName={" "}>
                  {selectedGroups.map((currentChip) => (
                    <Chip
                      key={currentChip.id}
                      onClick={() => deleteItem(currentChip.name!)}
                    >
                      {currentChip.path}
                    </Chip>
                  ))}
                </ChipGroup>
                <Button
                  id="kc-join-groups-button"
                  onClick={toggleModal}
                  variant="secondary"
                  data-testid="join-groups-button"
                >
                  {t("users:joinGroups")}
                </Button>
              </InputGroup>
            )}
          />
        </FormGroup>
      )}

      <ActionGroup>
        <Button
          data-testid={!user?.id ? "create-user" : "save-user"}
          isDisabled={
            !user?.id &&
            !watchUsernameInput &&
            !realm?.registrationEmailAsUsername
          }
          variant="primary"
          type="submit"
        >
          {user?.id ? t("common:save") : t("common:create")}
        </Button>
        <Button
          data-testid="cancel-create-user"
          onClick={() =>
            user?.id ? reset(user) : navigate(`/${realmName}/users`)
          }
          variant="link"
        >
          {user?.id ? t("common:revert") : t("common:cancel")}
        </Button>
      </ActionGroup>
    </FormAccess>
  );
};
