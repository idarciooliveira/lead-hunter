import { Dialog as DialogPrimitive } from "radix-ui";
import type * as React from "react";
import { cn } from "#/lib/utils";

export const Dialog = DialogPrimitive.Root;
export const DialogClose = DialogPrimitive.Close;

/** A top-anchored modal card over a dimmed page, as in the prototype's run dialogs. */
export function DialogContent({
	className,
	children,
	top = 140,
	...props
}: React.ComponentProps<typeof DialogPrimitive.Content> & { top?: number }) {
	return (
		<DialogPrimitive.Portal>
			<DialogPrimitive.Overlay className="fixed inset-0 z-50 bg-ovl data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:animate-in data-[state=open]:fade-in-0" />
			<DialogPrimitive.Content
				style={{ top }}
				className={cn(
					"fixed left-1/2 z-50 flex w-[520px] max-w-[calc(100%-32px)] -translate-x-1/2 flex-col gap-3.5 rounded-lg border border-line bg-panel p-5 text-tx outline-none data-[state=open]:animate-in data-[state=open]:fade-in-0 data-[state=open]:zoom-in-95",
					className,
				)}
				{...props}
			>
				{children}
			</DialogPrimitive.Content>
		</DialogPrimitive.Portal>
	);
}

export function DialogTitle({ className, ...props }: React.ComponentProps<typeof DialogPrimitive.Title>) {
	return <DialogPrimitive.Title className={cn("m-0 text-base font-semibold", className)} {...props} />;
}

export function DialogDescription({ className, ...props }: React.ComponentProps<typeof DialogPrimitive.Description>) {
	return <DialogPrimitive.Description className={cn("text-mute", className)} {...props} />;
}
