import { Tooltip as TooltipPrimitive } from "radix-ui";
import type * as React from "react";
import { cn } from "#/lib/utils";

export const TooltipProvider = TooltipPrimitive.Provider;

/** Dark explanatory bubble; used for "why this score". */
export function Tooltip({
	content,
	children,
	className,
}: {
	content: React.ReactNode;
	children: React.ReactNode;
	className?: string;
}) {
	return (
		<TooltipPrimitive.Root delayDuration={100}>
			<TooltipPrimitive.Trigger asChild>{children}</TooltipPrimitive.Trigger>
			<TooltipPrimitive.Portal>
				<TooltipPrimitive.Content
					side="bottom"
					align="start"
					sideOffset={6}
					className={cn(
						"z-50 w-[260px] rounded-md bg-tx px-2.5 py-2 text-xs leading-snug text-bg shadow-[0_4px_16px_rgba(0,0,0,.18)] data-[state=delayed-open]:animate-in data-[state=delayed-open]:fade-in-0",
						className,
					)}
				>
					{content}
				</TooltipPrimitive.Content>
			</TooltipPrimitive.Portal>
		</TooltipPrimitive.Root>
	);
}
