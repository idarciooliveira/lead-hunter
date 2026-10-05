import { z } from "zod";

/** The company profile campaigns propose services from (ADR 0019, 0029). Optional answers are null. */
export const CompanyProfile = z.object({
	name: z.string(),
	services: z.array(z.object({ name: z.string(), price: z.string().nullable(), deliveryTime: z.string().nullable() })),
	/** Where we work: Luanda zones. */
	area: z.array(z.string()),
	/** Current clients; they never show up as leads. */
	clients: z.array(z.object({ name: z.string(), phone: z.string().nullable() })),
	cases: z.array(
		z.object({
			sector: z.string().nullable(),
			client: z.string().nullable(),
			result: z.string().nullable(),
			/** False when the pitch may not use the client's name. */
			mayName: z.boolean(),
		}),
	),
});
export type CompanyProfile = z.infer<typeof CompanyProfile>;

/** The profile needs at least this many cases before it counts as complete. */
export const MIN_CASES = 2;

/** Luanda municipalities and the neighbourhoods leads are searched in. Users can type any other zone. */
export const ZONE_OPTIONS = [
	"Talatona",
	"Belas",
	"Kilamba",
	"Nova Vida",
	"Camama",
	"Benfica",
	"Maianga",
	"Miramar",
	"Alvalade",
	"Maculusso",
	"Ingombota",
	"Kinaxixi",
	"Cidade Alta",
	"Mutamba",
	"Ilha de Luanda",
	"Sambizanga",
	"Rangel",
	"Samba",
	"Morro Bento",
	"Futungo",
	"Golf",
	"Patriota",
	"Vila Alice",
	"Prenda",
	"Bairro Azul",
	"Coqueiros",
	"Cazenga",
	"Hoji-ya-Henda",
	"Viana",
	"Zango",
	"Cacuaco",
	"Sequele",
	"Kilamba Kiaxi",
	"Cassenda",
	"Mussulo",
	"Icolo e Bengo",
	"Catete",
] as const;
