import { createContext, type ReactNode, useCallback, useContext, useState } from "react";

export type Theme = "light" | "dark";

const STORAGE_KEY = "lh-theme";

/** Runs before hydration so the first paint already has the stored theme. */
export const themeScript = `try{if(localStorage.getItem("${STORAGE_KEY}")==="dark")document.documentElement.classList.add("dark")}catch(e){}`;

const ThemeContext = createContext<{ theme: Theme; toggle: () => void }>({
	theme: "light",
	toggle: () => {},
});

export function ThemeProvider({ children }: { children: ReactNode }) {
	const [theme, setTheme] = useState<Theme>(() =>
		typeof document !== "undefined" && document.documentElement.classList.contains("dark") ? "dark" : "light",
	);
	const toggle = useCallback(() => {
		setTheme((current) => {
			const next = current === "dark" ? "light" : "dark";
			document.documentElement.classList.toggle("dark", next === "dark");
			try {
				localStorage.setItem(STORAGE_KEY, next);
			} catch {
				// Storage can be blocked; the toggle still works for this page.
			}
			return next;
		});
	}, []);
	return <ThemeContext value={{ theme, toggle }}>{children}</ThemeContext>;
}

export function useTheme() {
	return useContext(ThemeContext);
}
