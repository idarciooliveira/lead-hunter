import type { CompanyProfile } from "./schema";

export const COMPANY: CompanyProfile = {
	name: "Raposa Software, Lda.",
	intro: "Fazemos sites, sistemas de marcações e apps para PME de Luanda.",
	entryOffer: "Website institucional",
	services: [
		{ name: "Website institucional", price: "350 000 a 900 000 Kz", deliveryTime: "2 a 4 semanas" },
		{ name: "Sistema de marcações", price: "1 800 000 a 6 000 000 Kz", deliveryTime: "6 a 10 semanas" },
		{ name: "App móvel", price: "2 500 000 a 8 000 000 Kz", deliveryTime: "10 a 16 semanas" },
		{ name: "Loja online", price: "600 000 a 1 800 000 Kz", deliveryTime: "3 a 6 semanas" },
	],
	area: ["Talatona", "Kilamba", "Maianga", "Viana", "Miramar"],
	clients: [
		{ name: "Clínica Esperança", phone: "+244923111222" },
		{ name: "Restaurante Sabor do Kwanza", phone: null },
		{ name: "Colégio Nova Geração", phone: null },
		{ name: "Auto Center Kilamba", phone: "+244923333444" },
		{ name: "Farmácia Central Maianga", phone: null },
		{ name: "Escola Luz do Saber", phone: null },
		{ name: "Oficina Rápida Cazenga", phone: null },
		{ name: "Padaria Pão Quente", phone: null },
		{ name: "Laboratório Vida", phone: null },
	],
	cases: [
		{
			sector: "Clínicas privadas",
			client: "Clínica Esperança, Alvalade",
			problem: "Marcações por telefone e muitas faltas.",
			built: "Sistema de marcações com lembretes por WhatsApp.",
			result: "Sistema de marcações com lembretes por WhatsApp, em produção desde 2025.",
			mayName: true,
		},
	],
};
