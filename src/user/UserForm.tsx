import React, { useState } from "react";
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
  TextInput,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { Controller, useFormContext } from "react-hook-form";
import { useHistory } from "react-router-dom";

import { FormAccess } from "../components/form-access/FormAccess";
import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import { HelpItem } from "../components/help-enabler/HelpItem";
import { useRealm } from "../context/realm-context/RealmContext";
import { useAdminClient } from "../context/auth/AdminClient";
import type GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";
import { useAlerts } from "../components/alert/Alerts";
import { emailRegexPattern } from "../util";
import { GroupPickerDialog } from "../components/group/GroupPickerDialog";
import moment from "moment";

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
  const { realm } = useRealm();

  const [
    isRequiredUserActionsDropdownOpen,
    setRequiredUserActionsDropdownOpen,
  ] = useState(false);
  const history = useHistory();
  const adminClient = useAdminClient();
  const { addAlert, addError } = useAlerts();

  const { handleSubmit, register, errors, watch, control, reset } =
    useFormContext();
  const watchUsernameInput = watch("username");
  const [selectedGroups, setSelectedGroups] = useState<GroupRepresentation[]>(
    []
  );
  const [open, setOpen] = useState(false);
  const [locked, setLocked] = useState(isLocked);

  const unLockUser = async () => {
    try {
      await adminClient.attackDetection.del({ id: user!.id! });
      addAlert(t("unlockSuccess"), AlertVariant.success);
    } catch (error) {
      addError("users:unlockError", error);
    }
  };

  const requiredUserActionsOptions = [
    <SelectOption key={0} value="CONFIGURE_TOTP">
      {t("configureOTP")}
    </SelectOption>,
    <SelectOption key={1} value="UPDATE_PASSWORD">
      {t("updatePassword")}
    </SelectOption>,
    <SelectOption key={2} value="UPDATE_PROFILE">
      {t("updateProfile")}
    </SelectOption>,
    <SelectOption key={3} value="VERIFY_EMAIL">
      {t("verifyEmail")}
    </SelectOption>,
  ];

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
      role="manage-users"
      className="pf-u-mt-lg"
    >
      {open && (
        <GroupPickerDialog
          type="selectMany"
          text={{
            title: "users:selectGroups",
            ok: "users:join",
          }}
          onConfirm={(groups) => {
            user?.id ? addGroups(groups) : addChips(groups);
            setOpen(false);
          }}
          onClose={() => setOpen(false)}
          filterGroups={selectedGroups.map((group) => group.name!)}
        />
      )}
      {user?.id ? (
        <>
          <FormGroup label={t("common:id")} fieldId="kc-id" isRequired>
            <TextInput id={user.id} value={user.id} type="text" isReadOnly />
          </FormGroup>
          <FormGroup label={t("createdAt")} fieldId="kc-created-at" isRequired>
            <TextInput
              value={moment(user.createdTimestamp).format(
                "MM/DD/YY hh:MM:ss A"
              )}
              type="text"
              id="kc-created-at"
              name="createdTimestamp"
              isReadOnly
            />
          </FormGroup>
        </>
      ) : (
        <FormGroup
          label={t("username")}
          fieldId="kc-username"
          isRequired
          validated={errors.username ? "error" : "default"}
          helperTextInvalid={t("common:required")}
        >
          <TextInput
            ref={register()}
            type="text"
            id="kc-username"
            name="username"
            isReadOnly={!!user?.id}
          />
        </FormGroup>
      )}
      <FormGroup
        label={t("email")}
        fieldId="kc-description"
        validated={errors.email ? "error" : "default"}
        helperTextInvalid={t("users:emailInvalid")}
      >
        <TextInput
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
        <TextInput
          ref={register()}
          data-testid="firstName-input"
          type="text"
          id="kc-firstname"
          name="firstName"
        />
      </FormGroup>
      <FormGroup
        label={t("lastName")}
        fieldId="kc-name"
        validated={errors.lastName ? "error" : "default"}
      >
        <TextInput
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
          />
        </FormGroup>
      )}
      <FormGroup
        label={t("common:enabled")}
        fieldId="kc-enabled"
        labelIcon={
          <HelpItem helpText="users-help:disabled" fieldLabelId="enabled" />
        }
      >
        <Controller
          name="enabled"
          defaultValue={true}
          control={control}
          render={({ onChange, value }) => (
            <Switch
              data-testid="user-enabled-switch"
              id={"kc-user-enabled"}
              onChange={(value) => onChange(value)}
              isChecked={value}
              label={t("common:on")}
              labelOff={t("common:off")}
            />
          )}
        />
      </FormGroup>
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
              {requiredUserActionsOptions}
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
          isDisabled={!user?.id && !watchUsernameInput}
          variant="primary"
          type="submit"
        >
          {user?.id ? t("common:save") : t("common:create")}
        </Button>
        <Button
          data-testid="cancel-create-user"
          onClick={() =>
            user?.id ? reset(user) : history.push(`/${realm}/users`)
          }
          variant="link"
        >
          {user?.id ? t("common:revert") : t("common:cancel")}
        </Button>
      </ActionGroup>
    </FormAccess>
  );
};
