import type * as React from "react";
import { cn } from "#/lib/utils";

/** A full-width selectable row, with an optional check box mark for multi-select. */
export function OptionButton({
	selected,
	check,
	className,
	children,
	...props
}: React.ComponentProps<"button"> & { selected: boolean; check?: boolean }) {
	return (
		<button
			type="button"
			aria-pressed={selected}
			className={cn(
				"flex w-full cursor-pointer items-center gap-2.5 rounded-md border border-line2 bg-panel px-3 py-2.5 text-left hover:border-mute",
				selected && "border-acc bg-acc-soft text-acc-tx hover:border-acc",
				className,
			)}
			{...props}
		>
			{check && (
				<span className="inline-flex size-4 items-center justify-center rounded-[4px] border-[1.5px] border-current text-[11px]">
					{selected ? "✓" : ""}
				</span>
			)}
			{children}
		</button>
	);
}
