import React from "react";
import { UseFormMethods } from "react-hook-form";
import { FormAccess } from "../../../components/form-access/FormAccess";
import { LdapMapperGeneral } from "./shared/LdapMapperGeneral";

export type LdapMapperMsadLdsUserAccountProps = {
  form: UseFormMethods;
};

export const LdapMapperMsadLdsUserAccount = ({
  form,
}: LdapMapperMsadLdsUserAccountProps) => {
  return (
    <>
      <FormAccess role="manage-realm" isHorizontal>
        <LdapMapperGeneral form={form} />
      </FormAccess>
    </>
  );
};
