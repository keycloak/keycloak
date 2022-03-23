import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import {
  ActionGroup,
  Button,
  FormGroup,
  PageSection,
  InputGroup,
  TextInput,
} from "@patternfly/react-core";
import { useEffect, useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { FormAccess } from "../components/form/FormAccess";
import { HelpItem } from "ui-shared";
import { MinusIcon, PlusIcon } from "@patternfly/react-icons";

type RealmSettingsThemesTabProps = {
  realm: RealmRepresentation;
  save: (realm: RealmRepresentation) => void;
};

export const OpenIdEndpointConfigurationTab = ({
  realm,
  save,
}: RealmSettingsThemesTabProps) => {
  const { t } = useTranslation();

  const [newClaim, setNewClaim] = useState("");
  const { control, handleSubmit, setValue } = useForm<RealmRepresentation>();

  const setupForm = () => {
    const attributes = realm.attributes || {};
    setValue("attributes", attributes);
  };

  useEffect(setupForm, []);

  return (
    <PageSection variant="light">
      <FormAccess
        isHorizontal
        role="manage-realm"
        className="pf-u-mt-lg"
        onSubmit={handleSubmit(save)}
      >
        <FormGroup
          label={t("claimsSupported")}
          fieldId="kc-claims-supported"
          labelIcon={
            <HelpItem
              helpText={t("claimsSupportedHelp")}
              fieldLabelId="claimsSupported"
            />
          }
        >
          <Controller
            name="attributes"
            control={control}
            render={({ field }) => {
              const claimsSupported = field.value?.claimsSupported?.trim()
                ? field.value.claimsSupported.split(",")
                : [];
              return (
                <>
                  {claimsSupported
                    .filter(Boolean)
                    .map((claim: string, index: number) => (
                      <InputGroup key={index}>
                        <TextInput
                          value={claim}
                          onChange={(value) => {
                            const attributes = field.value || {};
                            claimsSupported[index] = value;
                            attributes.claimsSupported =
                              claimsSupported.join(",");
                            field.onChange(attributes);
                          }}
                          aria-label={t("editClaimInput")}
                        />
                        <Button
                          icon={<MinusIcon />}
                          aria-label={t("removeClaimButton")}
                          onClick={() => {
                            const attributes = field.value || {};
                            claimsSupported.splice(index, 1);
                            attributes.claimsSupported =
                              claimsSupported.join(",");
                            field.onChange(attributes);
                          }}
                          variant="control"
                        />
                      </InputGroup>
                    ))}
                  <InputGroup>
                    <TextInput
                      value={newClaim}
                      onChange={(value) => {
                        setNewClaim(value);
                      }}
                      aria-label={t("newClaimInput")}
                    />
                    <Button
                      icon={<PlusIcon />}
                      aria-label={t("addClaimButton")}
                      onClick={() => {
                        if (newClaim) {
                          const attributes = field.value || {};
                          claimsSupported.push(newClaim);
                          attributes.claimsSupported =
                            claimsSupported.join(",");
                          setNewClaim("");
                          field.onChange(attributes);
                        }
                      }}
                      variant="control"
                    />
                  </InputGroup>
                </>
              );
            }}
          />
        </FormGroup>

        <ActionGroup>
          <Button
            variant="primary"
            type="submit"
            data-testid="openid-configuration-tab-save"
            aria-label={t("submit")}
          >
            {t("save")}
          </Button>
          <Button variant="link" onClick={setupForm}>
            {t("revert")}
          </Button>
        </ActionGroup>
      </FormAccess>
    </PageSection>
  );
};
