import { cn } from "#/lib/utils";

export type Band = "hi" | "mid" | "lo";

const fill: Record<Band | "acc", string> = {
	hi: "bg-ok",
	mid: "bg-acc",
	lo: "bg-mute",
	acc: "bg-acc",
};

/**
 * A thin rounded bar. `size="sm"` is the 64px score bar in lists; `lg` fills its
 * container and is used for budgets and progress.
 */
export function ProgressBar({
	value,
	tone = "acc",
	size = "lg",
	className,
	label,
}: {
	value: number;
	tone?: Band | "acc";
	size?: "sm" | "lg";
	className?: string;
	label?: string;
}) {
	const clamped = Math.min(100, Math.max(0, value));
	return (
		<div
			role="progressbar"
			aria-label={label}
			aria-valuenow={Math.round(clamped)}
			aria-valuemin={0}
			aria-valuemax={100}
			className={cn(
				"flex-none overflow-hidden rounded-full bg-soft",
				size === "sm" ? "h-1.5 w-16" : "h-2 w-full",
				className,
			)}
		>
			<i className={cn("block h-full rounded-full", fill[tone])} style={{ width: `${clamped}%` }} />
		</div>
	);
}
