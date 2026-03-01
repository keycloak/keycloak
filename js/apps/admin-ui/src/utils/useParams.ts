import { useParams as useParamsRR } from "react-router-dom";

export const useParams = <T extends Record<string, string>>() =>
  useParamsRR<T>() as T;
