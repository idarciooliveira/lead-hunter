import { useEffect } from "react";

function typing(target: EventTarget | null): boolean {
	if (!(target instanceof HTMLElement)) return false;
	return target.isContentEditable || ["INPUT", "TEXTAREA", "SELECT"].includes(target.tagName);
}

/**
 * Single-key shortcuts (`j`, `k`, `w`...) and two-key G-chords (`g h`). Keys are
 * ignored while the user types in a field or holds a modifier.
 */
export function useHotkeys(bindings: Record<string, () => void>, enabled = true) {
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
				const chord = bindings[`g ${key}`];
				if (chord) {
					e.preventDefault();
					chord();
				}
				return;
			}
			if (key === "g" && Object.keys(bindings).some((k) => k.startsWith("g "))) {
				pendingG = true;
				timer = setTimeout(() => {
					pendingG = false;
				}, 1000);
				return;
			}
			const single = bindings[key];
			if (single) {
				e.preventDefault();
				single();
			}
		};
		window.addEventListener("keydown", onKey);
		return () => {
			window.removeEventListener("keydown", onKey);
			clearTimeout(timer);
		};
	}, [bindings, enabled]);
}
