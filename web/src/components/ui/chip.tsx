import { cva, type VariantProps } from "class-variance-authority";
import type * as React from "react";
import { cn } from "#/lib/utils";

const chipVariants = cva("inline-flex items-center gap-1 whitespace-nowrap text-xs font-medium", {
	variants: {
		tone: {
			neutral: "text-mute",
			ok: "text-ok",
			warn: "text-warn",
			bad: "text-bad",
			info: "text-info",
			acc: "text-acc-tx",
		},
	},
	defaultVariants: { tone: "neutral" },
});

export type Tone = NonNullable<VariantProps<typeof chipVariants>["tone"]>;

/** A coloured status label. The prototype keeps these as text, without a background. */
export function Chip({ className, tone, ...props }: React.ComponentProps<"span"> & { tone?: Tone }) {
	return <span data-slot="chip" className={cn(chipVariants({ tone }), className)} {...props} />;
}

/** Marks a screen or control that the prototype shows but the product does not have yet. */
export function PlannedChip({ step }: { step?: number }) {
	return <Chip tone="warn">{step ? `Planeado, passo ${step}` : "Planeado"}</Chip>;
}
