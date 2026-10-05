import viteReact from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";

// Kept apart from vite.config.ts so unit tests do not load the Start and Nitro plugins.
export default defineConfig({
	resolve: { tsconfigPaths: true },
	plugins: [viteReact()],
	test: {
		environment: "jsdom",
		include: ["src/**/*.test.{ts,tsx}"],
		setupFiles: ["src/test-setup.ts"],
		env: { TZ: "Africa/Luanda" },
	},
});
