import React, { useEffect, useState } from "react";
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
import { Controller, UseFormMethods } from "react-hook-form";
import { useHistory, useParams } from "react-router-dom";
import { FormAccess } from "../components/form-access/FormAccess";
import UserRepresentation from "keycloak-admin/lib/defs/userRepresentation";
import { HelpItem } from "../components/help-enabler/HelpItem";
import { useRealm } from "../context/realm-context/RealmContext";
import { asyncStateFetch, useAdminClient } from "../context/auth/AdminClient";
import { useErrorHandler } from "react-error-boundary";
import moment from "moment";
import { JoinGroupDialog } from "./JoinGroupDialog";
import GroupRepresentation from "keycloak-admin/lib/defs/groupRepresentation";
import { useAlerts } from "../components/alert/Alerts";
import { emailRegexPattern } from "../util";

export type UserFormProps = {
  form: UseFormMethods<UserRepresentation>;
  save: (user: UserRepresentation) => void;
  editMode: boolean;
  timestamp?: number;
  onGroupsUpdate: (groups: GroupRepresentation[]) => void;
};

export const UserForm = ({
  form: { handleSubmit, register, errors, watch, control, setValue, reset },
  save,
  editMode,
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
  const { id } = useParams<{ id: string }>();
  const handleError = useErrorHandler();

  const watchUsernameInput = watch("username");
  const [timestamp, setTimestamp] = useState(null);
  const [chips, setChips] = useState<(string | undefined)[]>([]);
  const [selectedGroups, setSelectedGroups] = useState<GroupRepresentation[]>(
    []
  );

  const { addAlert } = useAlerts();

  const [open, setOpen] = useState(false);

  useEffect(() => {
    if (editMode) {
      return asyncStateFetch(
        () => adminClient.users.findOne({ id: id }),
        (user) => {
          setupForm(user);
        },
        handleError
      );
    }
  }, [chips]);

  const setupForm = (user: UserRepresentation) => {
    reset();
    Object.entries(user).map((entry) => {
      if (entry[0] == "createdTimestamp") {
        setTimestamp(entry[1]);
      } else {
        setValue(entry[0], entry[1]);
      }
    });
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
    const copyOfChips = chips;
    const copyOfGroups = selectedGroups;

    setChips(copyOfChips.filter((item) => item !== id));
    setSelectedGroups(copyOfGroups.filter((item) => item.name !== id));
    onGroupsUpdate(selectedGroups);
  };

  const addChips = async (groups: GroupRepresentation[]): Promise<void> => {
    const newSelectedGroups = groups;

    const newGroupNames: (string | undefined)[] = newSelectedGroups!.map(
      (item) => item.name
    );
    setChips([...chips!, ...newGroupNames]);
    setSelectedGroups([...selectedGroups!, ...newSelectedGroups]);
  };

  onGroupsUpdate(selectedGroups);

  const addGroups = async (groups: GroupRepresentation[]): Promise<void> => {
    const newGroups = groups;

    newGroups.forEach(async (group) => {
      try {
        await adminClient.users.addToGroup({
          id: id,
          groupId: group.id!,
        });
        addAlert(t("users:addedGroupMembership"), AlertVariant.success);
      } catch (error) {
        addAlert(
          t("users:addedGroupMembershipError", { error }),
          AlertVariant.danger
        );
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
        <JoinGroupDialog
          open={open}
          onClose={() => setOpen(!open)}
          onConfirm={editMode ? addGroups : addChips}
          toggleDialog={() => toggleModal()}
          chips={chips}
        />
      )}
      {editMode ? (
        <>
          <FormGroup
            label={t("common:id")}
            fieldId="kc-id"
            isRequired
            validated={errors.id ? "error" : "default"}
            helperTextInvalid={t("common:required")}
          >
            <TextInput
              ref={register({ required: !editMode })}
              type="text"
              id="kc-id"
              name="id"
              isReadOnly={editMode}
            />
          </FormGroup>
          <FormGroup
            label={t("createdAt")}
            fieldId="kc-created-at"
            isRequired
            validated={errors.createdTimestamp ? "error" : "default"}
            helperTextInvalid={t("common:required")}
          >
            <TextInput
              value={moment(timestamp).format("MM/DD/YY hh:MM:ss A")}
              type="text"
              id="kc-created-at"
              name="createdTimestamp"
              isReadOnly={editMode}
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
            isReadOnly={editMode}
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
            helpText={t("emailVerifiedHelpText")}
            forLabel={t("emailVerified")}
            forID="email-verified"
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
        ></Controller>
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
      <FormGroup
        label={t("common:enabled")}
        fieldId="kc-enabled"
        labelIcon={
          <HelpItem
            helpText={t("disabledHelpText")}
            forLabel={t("enabled")}
            forID="enabled-label"
          />
        }
      >
        <Controller
          name="enabled"
          defaultValue={false}
          control={control}
          render={({ onChange, value }) => (
            <Switch
              data-testid="user-enabled-switch"
              id={"kc-user-enabled"}
              isDisabled={false}
              onChange={(value) => onChange(value)}
              isChecked={value}
              label={t("common:on")}
              labelOff={t("common:off")}
            />
          )}
        ></Controller>
      </FormGroup>
      <FormGroup
        label={t("requiredUserActions")}
        fieldId="kc-required-user-actions"
        validated={errors.requiredActions ? "error" : "default"}
        helperTextInvalid={t("common:required")}
        labelIcon={
          <HelpItem
            helpText={t("requiredUserActionsHelpText")}
            forLabel={t("requiredUserActions")}
            forID="required-user-actions-label"
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
      {!editMode && (
        <FormGroup
          label={t("common:groups")}
          fieldId="kc-groups"
          validated={errors.requiredActions ? "error" : "default"}
          helperTextInvalid={t("common:required")}
          labelIcon={
            <HelpItem
              helpText={t("requiredUserActionsHelpText")}
              forLabel={t("requiredUserActions")}
              forID="required-user-actions-label"
            />
          }
        >
          <Controller
            name="groups"
            defaultValue={[]}
            typeAheadAriaLabel="Select an action"
            control={control}
            render={() => (
              <>
                <InputGroup>
                  <ChipGroup categoryName={" "}>
                    {chips.map((currentChip) => (
                      <Chip
                        key={currentChip}
                        onClick={() => deleteItem(currentChip!)}
                      >
                        {currentChip}
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
              </>
            )}
          />
        </FormGroup>
      )}

      <ActionGroup>
        <Button
          data-testid={!editMode ? "create-user" : "save-user"}
          isDisabled={!editMode && !watchUsernameInput}
          variant="primary"
          type="submit"
        >
          {!editMode ? t("common:Create") : t("common:Save")}
        </Button>
        <Button
          data-testid="cancel-create-user"
          onClick={() => history.push(`/${realm}/users`)}
          variant="link"
        >
          {t("common:cancel")}
        </Button>
      </ActionGroup>
    </FormAccess>
  );
};
