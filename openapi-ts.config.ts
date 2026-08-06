import { defineConfig } from "@hey-api/openapi-ts";

export default defineConfig({
  input: "./api/generated/openapi.yaml",
  output: {
    path: "./build/generated/frontend-client",
  },
  plugins: [
    "@hey-api/client-fetch",
    "@hey-api/typescript",
    "@hey-api/sdk",
    "zod",
  ],
});
