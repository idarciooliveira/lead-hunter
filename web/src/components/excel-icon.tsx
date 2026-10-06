/**
 * The modern Microsoft 365 Excel tile: the brand-green rounded square with the
 * white X. The green is the Office brand value (#107C41), not a theme token.
 */
export function ExcelIcon({ className, ...props }: React.ComponentProps<"svg">) {
	return (
		<svg viewBox="0 0 24 24" fill="none" className={className} {...props}>
			<title>Microsoft Excel</title>
			<path
				fill="#107C41"
				d="M5.3 3h13.4C19.97 3 21 4.03 21 5.3v13.4c0 1.27-1.03 2.3-2.3 2.3H5.3C4.03 21 3 19.97 3 18.7V5.3C3 4.03 4.03 3 5.3 3Z"
			/>
			<path stroke="#fff" strokeWidth="2.4" strokeLinecap="round" d="m9.3 8.4 5.4 7.2m0-7.2-5.4 7.2" />
		</svg>
	);
}
