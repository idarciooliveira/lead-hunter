/** The pixel fox from the CLI banner (ADR 0025), redrawn at 16×16. */
export function FoxLogo({ size = 28 }: { size?: number }) {
	return (
		<svg width={size} height={size} viewBox="0 0 16 16" shapeRendering="crispEdges" aria-hidden="true">
			<g fill="#E8590C">
				<rect x="2" y="1" width="2" height="1" />
				<rect x="12" y="1" width="2" height="1" />
				<rect x="2" y="2" width="3" height="1" />
				<rect x="11" y="2" width="3" height="1" />
				<rect x="2" y="3" width="4" height="1" />
				<rect x="10" y="3" width="4" height="1" />
				<rect x="2" y="4" width="12" height="4" />
				<rect x="3" y="8" width="10" height="1" />
			</g>
			<g fill="#FFF3E8">
				<rect x="4" y="9" width="8" height="1" />
				<rect x="5" y="10" width="6" height="1" />
				<rect x="6" y="11" width="4" height="1" />
			</g>
			<g fill="#7A2E05">
				<rect x="3" y="2" width="1" height="1" />
				<rect x="12" y="2" width="1" height="1" />
			</g>
			<g fill="#241410">
				<rect x="4" y="6" width="1" height="1" />
				<rect x="11" y="6" width="1" height="1" />
				<rect x="7" y="10" width="2" height="1" />
			</g>
		</svg>
	);
}
