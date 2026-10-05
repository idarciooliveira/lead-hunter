import type { CompanyProfile } from "./schema";

export const COMPANY: CompanyProfile = {
	name: "Raposa Software, Lda.",
	services: [
		{
			name: "Website institucional",
			description: "Site rápido, pensado para telemóvel, com mapa e botão de WhatsApp.",
			priceRange: "350 000 a 900 000 Kz",
			timeline: "2 a 4 semanas",
		},
		{
			name: "Sistema de marcações",
			description: "Marcações online com lembretes por SMS e WhatsApp.",
			priceRange: "1 800 000 a 6 000 000 Kz",
			timeline: "6 a 10 semanas",
		},
		{
			name: "App móvel",
			description: "App iOS e Android para clientes ou para a equipa.",
			priceRange: "2 500 000 a 8 000 000 Kz",
			timeline: "10 a 16 semanas",
		},
		{
			name: "Loja online",
			description: "Catálogo, pagamento Multicaixa Express e entregas.",
			priceRange: "600 000 a 1 800 000 Kz",
			timeline: "3 a 6 semanas",
		},
	],
	target: {
		sectors: ["Clínicas privadas", "Restaurantes", "Escolas privadas", "Oficinas auto"],
		size: "1 a 20 funcionários, um ou dois locais",
		zones: ["Talatona", "Kilamba", "Maianga", "Viana", "Miramar"],
		decider: "Dono ou director clínico, contacto directo por WhatsApp",
	},
	pastClients: [
		"Clínica Esperança",
		"Restaurante Sabor do Kwanza",
		"Colégio Nova Geração",
		"Auto Center Kilamba",
		"Farmácia Central Maianga",
		"Escola Luz do Saber",
		"Oficina Rápida Cazenga",
		"Padaria Pão Quente",
		"Laboratório Vida",
	],
	cases: [
		{
			client: "Clínica Esperança, Alvalade",
			summary: "Sistema de marcações com lembretes por WhatsApp, em produção desde 2025.",
		},
	],
};
