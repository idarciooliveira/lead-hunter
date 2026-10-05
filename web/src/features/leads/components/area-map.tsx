/** A schematic street map with a pin. Placeholder until we embed real map tiles. */
export function AreaMap() {
	return (
		<div className="relative hidden min-h-[180px] min-w-[240px] flex-[0_1_320px] overflow-hidden rounded-lg border border-line bg-soft md:block">
			<svg
				viewBox="0 0 320 200"
				width="100%"
				height="100%"
				preserveAspectRatio="xMidYMid slice"
				role="img"
				aria-label="Mapa esquemático da zona"
				className="absolute inset-0"
			>
				<g fill="none" className="stroke-line2" strokeWidth="10" strokeLinecap="round">
					<path d="M-10 150 L120 120 L200 130 L340 70" />
					<path d="M90 -10 L110 120 L100 210" />
					<path d="M200 130 L230 210" />
				</g>
				<g fill="none" className="stroke-panel" strokeWidth="6" strokeLinecap="round">
					<path d="M-10 150 L120 120 L200 130 L340 70" />
					<path d="M90 -10 L110 120 L100 210" />
					<path d="M200 130 L230 210" />
				</g>
				<rect x="230" y="20" width="60" height="40" rx="6" className="fill-line" />
				<rect x="20" y="20" width="50" height="60" rx="6" className="fill-line" />
				<path d="M160 70c-12 0-20 9-20 20 0 14 20 36 20 36s20-22 20-36c0-11-8-20-20-20Z" fill="#C2410C" />
				<circle cx="160" cy="90" r="7" fill="#fff" />
			</svg>
		</div>
	);
}
