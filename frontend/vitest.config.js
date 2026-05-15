import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  test: {
    environment: "jsdom",
    setupFiles: "./src/test/setupTests.js",
    globals: true,
    exclude: ["node_modules", "dist", "coverage", "OnlineFoodDeliveryPlatform-Frontend-main"],
    coverage: {
      provider: "v8",
      reporter: ["text", "html"],
      include: [
        "src/lib/api.js",
        "src/lib/auth.js",
        "src/lib/roleRedirect.js",
        "src/context/SessionContext.jsx",
        "src/components/AuthForm.jsx",
        "src/pages/LoginPage.jsx"
      ],
      thresholds: {
        lines: 80,
        functions: 80,
        branches: 80,
        statements: 80
      }
    }
  }
});
