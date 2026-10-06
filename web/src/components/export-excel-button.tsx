import { ExcelIcon } from "#/components/excel-icon";
import { Button, type ButtonProps } from "#/components/ui/button";
import { Kbd } from "#/components/ui/kbd";
import { Tooltip } from "#/components/ui/tooltip";

type Size = ButtonProps["size"];

/**
 * Icon-only button that exports a spreadsheet, shared by every list page. The
 * Excel logo carries the meaning; the tooltip spells it out. `shortcut` shows
 * the single-key shortcut next to the label, e.g. E.
 */
export function ExportExcelButton({
	onClick,
	shortcut,
	size = "default",
	className,
}: {
	onClick: () => void;
	shortcut?: string;
	size?: Size;
	className?: string;
}) {
	return (
		<Tooltip
			content={
				shortcut ? (
					<span className="inline-flex items-center gap-1.5">
						Exportar Excel <Kbd>{shortcut}</Kbd>
					</span>
				) : (
					"Exportar Excel"
				)
			}
		>
			<Button size={size} className={className} aria-label="Exportar Excel" onClick={onClick}>
				<ExcelIcon aria-hidden className={size === "touch" || size === "touch-icon" ? "size-[18px]" : "size-[14px]"} />
			</Button>
		</Tooltip>
	);
}
