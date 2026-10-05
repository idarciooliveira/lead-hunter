import type * as React from "react";
import { cn } from "#/lib/utils";

export function Kbd({
	className,
	onAccent,
	...props
}: React.ComponentProps<"kbd"> & {
	/** Inside a primary button, where the default border would disappear. */
	onAccent?: boolean;
}) {
	return (
		<kbd
			className={cn(
				"rounded-[4px] border px-[5px] py-[3px] font-mono text-[10.5px] leading-none",
				onAccent ? "border-transparent bg-black/20 text-on-acc" : "border-line2 bg-panel text-mute",
				className,
			)}
			{...props}
		/>
	);
}
