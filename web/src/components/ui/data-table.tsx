import { type ColumnDef, createColumnHelper, type RowData, tableFeatures, useTable } from "@tanstack/react-table";
import { cn } from "#/lib/utils";

/** Lists only need the core row model; sorting and filtering happen in the API. */
export const listFeatures = tableFeatures({});
// biome-ignore lint/suspicious/noExplicitAny: each column has its own value type.
export type ListColumn<T extends RowData> = ColumnDef<typeof listFeatures, T, any>;
export const listColumns = <T extends RowData>() => createColumnHelper<typeof listFeatures, T>();

/**
 * A real table laid out as CSS grid rows, the way every list in the prototype looks.
 * `template` is the `grid-template-columns` value shared by header and rows.
 */
export function DataTable<T extends RowData>({
	columns,
	data,
	template,
	minWidth,
	getRowId,
	onRowClick,
	rowClassName,
	label,
	empty,
}: {
	columns: ListColumn<T>[];
	data: T[];
	template: string;
	minWidth: number;
	getRowId: (row: T) => string;
	/** Mouse shortcut only; a cell must also hold a link so keyboard users can open the row. */
	onRowClick?: (row: T) => void;
	rowClassName?: string;
	label: string;
	empty?: React.ReactNode;
}) {
	const table = useTable({ features: listFeatures, columns, data, getRowId: (row) => getRowId(row) });
	const rowBase = "grid items-center gap-3 border-b border-line px-4";
	return (
		<div className="overflow-x-auto">
			<table aria-label={label} className="block border-collapse" style={{ minWidth }}>
				<thead className="block">
					{table.getHeaderGroups().map((group) => (
						<tr
							key={group.id}
							className={cn(rowBase, "eyebrow min-h-9 bg-bg")}
							style={{ gridTemplateColumns: template }}
						>
							{group.headers.map((header) => (
								<th key={header.id} className="p-0 text-left font-medium">
									{header.isPlaceholder ? null : <table.FlexRender header={header} />}
								</th>
							))}
						</tr>
					))}
				</thead>
				<tbody className="block">
					{table.getRowModel().rows.map((row) => (
						<tr
							key={row.id}
							data-row-id={row.id}
							className={cn(rowBase, onRowClick && "cursor-pointer hover:bg-soft", rowClassName)}
							style={{ gridTemplateColumns: template }}
							onClick={onRowClick ? () => onRowClick(row.original) : undefined}
						>
							{row.getAllCells().map((cell) => (
								<td key={cell.id} className="min-w-0 p-0">
									<table.FlexRender cell={cell} />
								</td>
							))}
						</tr>
					))}
				</tbody>
			</table>
			{data.length === 0 && empty}
		</div>
	);
}
