import { Moon, Sun } from "lucide-react";
import { Button } from "#/components/ui/button";
import { useTheme } from "#/lib/theme";

export function ThemeToggle({ touch }: { touch?: boolean }) {
	const { theme, toggle } = useTheme();
	const Icon = theme === "dark" ? Sun : Moon;
	return (
		<Button size={touch ? "touch-icon" : "icon"} onClick={toggle} aria-label="Alternar tema claro e escuro">
			<Icon className="size-[15px]" aria-hidden />
		</Button>
	);
}
