import { useEffect, useEffectEvent } from "react";

function typing(target: EventTarget | null): boolean {
	if (!(target instanceof HTMLElement)) return false;
	return target.isContentEditable || ["INPUT", "TEXTAREA", "SELECT"].includes(target.tagName);
}

/**
 * Single-key shortcuts (`j`, `k`, `w`...) and two-key G-chords (`g h`). Keys are
 * ignored while the user types in a field or holds a modifier. `bindings` always
 * reads the latest render, so callers can pass a fresh object every time.
 */
export function useHotkeys(bindings: Record<string, () => void>, enabled = true) {
	const run = useEffectEvent((key: string, e: KeyboardEvent) => {
		const action = bindings[key];
		if (!action) return;
		e.preventDefault();
		action();
	});
	const hasChords = useEffectEvent(() => Object.keys(bindings).some((k) => k.startsWith("g ")));

	useEffect(() => {
		if (!enabled) return;
		let pendingG = false;
		let timer: ReturnType<typeof setTimeout> | undefined;
		const onKey = (e: KeyboardEvent) => {
			if (e.metaKey || e.ctrlKey || e.altKey || typing(e.target)) return;
			const key = e.key.toLowerCase();
			if (pendingG) {
				pendingG = false;
				clearTimeout(timer);
				run(`g ${key}`, e);
				return;
			}
			if (key === "g" && hasChords()) {
				pendingG = true;
				timer = setTimeout(() => {
					pendingG = false;
				}, 1000);
				return;
			}
			run(key, e);
		};
		window.addEventListener("keydown", onKey);
		return () => {
			window.removeEventListener("keydown", onKey);
			clearTimeout(timer);
		};
	}, [enabled]);
}
