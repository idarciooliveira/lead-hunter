import { cva, type VariantProps } from "class-variance-authority";
import { Slot } from "radix-ui";
import type * as React from "react";
import { cn } from "#/lib/utils";

const buttonVariants = cva(
	"inline-flex shrink-0 cursor-pointer items-center justify-center gap-2 whitespace-nowrap rounded-md border font-medium transition-colors disabled:cursor-not-allowed disabled:opacity-45 aria-disabled:pointer-events-none aria-disabled:opacity-40 [&_svg]:shrink-0",
	{
		variants: {
			variant: {
				default: "border-line2 bg-panel hover:bg-soft",
				primary: "border-acc bg-acc text-on-acc hover:bg-acc-h",
				active: "border-acc bg-acc-soft text-acc-tx",
				ghost: "border-transparent bg-transparent hover:bg-soft",
			},
			size: {
				default: "h-8 px-3 text-sm",
				sm: "h-7 px-2.5 text-xs",
				icon: "size-8 p-0",
				"icon-sm": "size-7 p-0",
				/** 44px touch target for the mobile layouts. */
				touch: "h-11 rounded-[10px] px-3.5 text-base",
				"touch-icon": "size-11 rounded-[10px] p-0",
			},
		},
		defaultVariants: { variant: "default", size: "default" },
	},
);

export type ButtonProps = React.ComponentProps<"button"> & VariantProps<typeof buttonVariants> & { asChild?: boolean };

export function Button({ className, variant, size, asChild = false, ...props }: ButtonProps) {
	const Comp = asChild ? Slot.Root : "button";
	return <Comp data-slot="button" className={cn(buttonVariants({ variant, size }), className)} {...props} />;
}

export { buttonVariants };
