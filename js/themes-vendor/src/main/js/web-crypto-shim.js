import { sha256 } from "@noble/hashes/sha2.js";
import { v4 as uuidv4 } from "uuid";

// Shim for Web Crypto API specifically for Keycloak JS, as this API can sometimes be missing, for example in an insecure context:
// https://developer.mozilla.org/en-US/docs/Web/Security/Secure_Contexts
// Since we have decided to support insecure contexts, we (sadly) need to provide a fallback for the Web Crypto API.
if (typeof crypto === "undefined") {
  globalThis.crypto = {};
}

if (typeof crypto.subtle === "undefined") {
  Object.defineProperty(crypto, "subtle", {
    value: {
      digest: async (algorithm, data) => {
        if (algorithm === "SHA-256") {
          return sha256(data);
        }

        throw new Error("Unsupported algorithm");
      },
    },
  });
}

if (typeof crypto.getRandomValues === "undefined") {
  Object.defineProperty(crypto, "getRandomValues", {
    value: (array) => {
      for (let i = 0; i < array.length; i++) {
        array[i] = Math.floor(Math.random() * 256);
      }

      return array;
    },
  });
}

if (typeof crypto.randomUUID === "undefined") {
  Object.defineProperty(crypto, "randomUUID", {
    value: () => {
      return uuidv4();
    },
  });
}
