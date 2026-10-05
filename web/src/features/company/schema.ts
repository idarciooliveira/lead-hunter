import { z } from "zod";

/** The company profile campaigns propose services from (ADR 0019, 0029). */
export const CompanyProfile = z.object({
	name: z.string(),
	services: z.array(
		z.object({ name: z.string(), description: z.string(), priceRange: z.string(), timeline: z.string() }),
	),
	target: z.object({ sectors: z.array(z.string()), size: z.string(), zones: z.array(z.string()), decider: z.string() }),
	pastClients: z.array(z.string()),
	cases: z.array(z.object({ client: z.string(), summary: z.string() })),
});
export type CompanyProfile = z.infer<typeof CompanyProfile>;

/** The profile needs at least this many cases before it counts as complete. */
export const MIN_CASES = 2;

/** Markets a campaign can target: PME segments in Luanda that buy software. The API will serve this list; until then it lives here. */
export const SECTOR_OPTIONS = [
	// Saúde e bem-estar
	"Clínicas privadas",
	"Consultórios médicos",
	"Clínicas dentárias",
	"Laboratórios de análises",
	"Farmácias",
	"Ópticas",
	"Clínicas veterinárias",
	"Salões de beleza",
	"Barbearias",
	"Spas e estética",
	"Ginásios",
	// Educação
	"Escolas privadas",
	"Creches e infantários",
	"Centros de explicações",
	"Escolas de condução",
	"Centros de formação",
	"Escolas de idiomas",
	// Alimentação e hotelaria
	"Restaurantes",
	"Cafés e pastelarias",
	"Padarias",
	"Bares e discotecas",
	"Catering e eventos",
	"Hotéis e alojamento",
	"Agências de viagens",
	// Comércio
	"Supermercados e minimercados",
	"Lojas de roupa e calçado",
	"Lojas de electrónica",
	"Lojas de materiais de construção",
	"Distribuidores e grossistas",
	"Floristas e presentes",
	// Automóvel e transporte
	"Oficinas auto",
	"Stands de automóveis",
	"Lavagens auto",
	"Empresas de transporte e logística",
	"Aluguer de viaturas",
	"Táxis e transporte de passageiros",
	// Serviços profissionais
	"Escritórios de advogados",
	"Contabilidade e auditoria",
	"Imobiliárias",
	"Seguradoras e corretores",
	"Agências de marketing e publicidade",
	"Gráficas",
	"Empresas de segurança",
	"Empresas de limpeza",
	// Construção e indústria
	"Construção civil",
	"Arquitectura e engenharia",
	"Serralharias e carpintarias",
	"Pequena indústria e fabrico",
	// Outros
	"ONGs e associações",
	"Igrejas e organizações religiosas",
	"Agricultura e agro-negócio",
	"Energia solar e instalações",
	"Lavandarias",
] as const;

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
