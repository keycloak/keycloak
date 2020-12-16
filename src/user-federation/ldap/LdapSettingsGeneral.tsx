import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
  TextInput,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import React, { useEffect, useState } from "react";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { useForm, Controller } from "react-hook-form";
import { convertToFormValues } from "../../util";
import ComponentRepresentation from "keycloak-admin/lib/defs/componentRepresentation";
import { FormAccess } from "../../components/form-access/FormAccess";
import { useAdminClient } from "../../context/auth/AdminClient";
import { useParams } from "react-router-dom";

export const LdapSettingsGeneral = () => {
  const { t } = useTranslation("user-federation");
  const helpText = useTranslation("user-federation-help").t;
  const adminClient = useAdminClient();
  const { register, control, setValue } = useForm<ComponentRepresentation>();
  const { id } = useParams<{ id: string }>();
  const [isVendorDropdownOpen, setIsVendorDropdownOpen] = useState(false);

  const setupForm = (component: ComponentRepresentation) => {
    Object.entries(component).map((entry) => {
      if (entry[0] === "config") {
        convertToFormValues(entry[1], "config", setValue);
        if (entry[1].vendor) {
          setValue("config.vendor", convertVendorNames(entry[1].vendor[0]));
        }
      } else {
        setValue(entry[0], entry[1]);
      }
    });
  };

  useEffect(() => {
    (async () => {
      const fetchedComponent = await adminClient.components.findOne({ id });
      if (fetchedComponent) {
        setupForm(fetchedComponent);
      }
    })();
  }, []);

  const convertVendorNames = (vendorName: string) => {
    switch (vendorName) {
      case "ad":
        return "Active Directory";
      case "rhds":
        return "Red Hat Directory Server";
      case "tivoli":
        return "Tivoli";
      case "edirectory":
        return "Novell eDirectory";
      case "other":
        return "Other";
      default:
        return t("common:choose");
    }
  };

  return (
    <>
      <FormAccess role="manage-realm" isHorizontal>
        <FormGroup
          label={t("consoleDisplayName")}
          labelIcon={
            <HelpItem
              helpText={helpText("consoleDisplayNameHelp")}
              forLabel={t("consoleDisplayName")}
              forID="kc-console-display-name"
            />
          }
          fieldId="kc-console-display-name"
          isRequired
        >
          <TextInput
            isRequired
            type="text"
            id="kc-console-display-name"
            name="name"
            ref={register}
          />
        </FormGroup>
        <FormGroup
          label={t("vendor")}
          labelIcon={
            <HelpItem
              helpText={helpText("vendorHelp")}
              forLabel={t("vendor")}
              forID="kc-vendor"
            />
          }
          fieldId="kc-vendor"
          isRequired
        >
          <Controller
            name="config.vendor"
            defaultValue=""
            control={control}
            render={({ onChange, value }) => (
              <Select
                toggleId="kc-vendor"
                required
                onToggle={() => setIsVendorDropdownOpen(!isVendorDropdownOpen)}
                isOpen={isVendorDropdownOpen}
                onSelect={(_, value) => {
                  onChange(value as string);
                  setIsVendorDropdownOpen(false);
                }}
                selections={value}
                variant={SelectVariant.single}
              >
                <SelectOption
                  key={0}
                  value={t("common:choose")}
                  isPlaceholder
                />
                <SelectOption key={1} value="Active Directory" />
                <SelectOption key={2} value="Red Hat Directory Server" />
                <SelectOption key={3} value="Tivoli" />
                <SelectOption key={4} value="Novell eDirectory" />
                <SelectOption key={5} value="Other" />
              </Select>
            )}
          ></Controller>
        </FormGroup>
      </FormAccess>
    </>
  );
};
