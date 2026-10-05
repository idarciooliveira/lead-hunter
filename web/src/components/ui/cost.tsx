import { cn } from "#/lib/utils";

/** The price tag next to anything that spends money: `≈ $0.64`, `grátis`. */
export function Cost({
	children,
	onAccent,
	className,
}: {
	children: React.ReactNode;
	/** Inside a primary button. */
	onAccent?: boolean;
	className?: string;
}) {
	return (
		<span
			className={cn(
				"inline-flex items-center font-mono text-[11px]",
				onAccent ? "text-on-acc opacity-85" : "text-acc-tx",
				className,
			)}
		>
			{children}
		</span>
	);
}
