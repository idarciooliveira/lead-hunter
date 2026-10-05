import { cn } from "#/lib/utils";

export type SegmentedOption<T extends string> = { value: T; label: React.ReactNode; count?: number };

/** A row of joined toggle buttons with one active item. */
export function Segmented<T extends string>({
	options,
	value,
	onChange,
	label,
}: {
	options: SegmentedOption<T>[];
	value: T;
	onChange: (value: T) => void;
	label: string;
}) {
	return (
		<fieldset className="m-0 inline-flex overflow-hidden rounded-md border border-line2 bg-panel p-0">
			<legend className="sr-only">{label}</legend>
			{options.map((option) => (
				<button
					key={option.value}
					type="button"
					aria-pressed={option.value === value}
					onClick={() => onChange(option.value)}
					className={cn(
						"inline-flex h-[30px] cursor-pointer items-center gap-1.5 px-3 font-medium text-mute",
						option.value === value && "bg-acc-soft text-acc-tx",
					)}
				>
					{option.label}
					{option.count !== undefined && <span className="font-mono text-[11px]">{option.count}</span>}
				</button>
			))}
		</fieldset>
	);
}
