import { useParams as useParamsRR } from "react-router-dom-v5-compat";

export const useParams = <T extends Record<string, string>>() =>
  useParamsRR<T>() as T;
