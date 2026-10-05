/** Main sections. `keys` is the G-chord shortcut shown in the sidebar. */
export const NAV = [
	{ to: "/hoje", label: "Hoje", keys: "G H", short: "fila" },
	{ to: "/campanhas", label: "Campanhas", keys: "G C", short: "pesquisas" },
	{ to: "/leads", label: "Leads", keys: "G L", short: "ranking" },
	{ to: "/empresa", label: "Empresa", keys: "G E", short: "perfil" },
	{ to: "/uso", label: "Uso e custos", keys: "G U", short: "custos" },
] as const;

export const STATES_NAV = { to: "/estados", label: "Estados do sistema", keys: "G S" } as const;

/** Second key of each G-chord. */
export const CHORDS: Record<string, string> = {
	h: "/hoje",
	c: "/campanhas",
	l: "/leads",
	e: "/empresa",
	u: "/uso",
	s: "/estados",
};
