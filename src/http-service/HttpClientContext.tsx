import { createContext } from "react";
import { HttpClient } from "./http-client";

export const HttpClientContext = createContext<HttpClient | undefined>(
  undefined
);
