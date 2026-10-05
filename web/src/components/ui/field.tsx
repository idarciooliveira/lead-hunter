import type * as React from "react";
import { cn } from "#/lib/utils";

const control =
	"w-full rounded-md border border-line2 bg-panel focus:outline-2 focus:-outline-offset-1 focus:outline-acc";

export function Input({ className, ...props }: React.ComponentProps<"input">) {
	return <input className={cn(control, "h-8 px-2.5", className)} {...props} />;
}

export function Textarea({ className, ...props }: React.ComponentProps<"textarea">) {
	return <textarea className={cn(control, "min-h-[84px] resize-y px-2.5 py-2", className)} {...props} />;
}

export function NativeSelect({ className, ...props }: React.ComponentProps<"select">) {
	return <select className={cn(control, "h-8 px-2.5", className)} {...props} />;
}

export function Label({ className, ...props }: React.ComponentProps<"label">) {
	// biome-ignore lint/a11y/noLabelWithoutControl: callers nest the control or pass htmlFor.
	return <label className={cn("mb-1.5 block text-xs font-medium text-mute", className)} {...props} />;
}
