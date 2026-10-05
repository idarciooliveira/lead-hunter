import type { Lead } from "./schema";

// Sample leads shaped like the future GET /api/leads response. Scores and reasons are data,
// as the API will return them; the UI never computes them (ADR 0007, 0030).
export const LEADS: Lead[] = [
	{
		id: "l1",
		campaignSlug: "clinicas-talatona",
		rank: 1,
		name: "Clínica Santa Luzia",
		category: "Clínica médica",
		area: "Talatona",
		address: "Rua das Acácias 14, Talatona, Luanda",
		rating: 4.5,
		reviewCount: 187,
		phone: "+244 923 410 872",
		website: null,
		stage: "QUALIFIED",
		stageReason: null,
		status: "NEW",
		lostReason: null,
		score: 80,
		breakdown: {
			stage1: [
				{
					points: 25,
					reason: "Sem website no Google Maps",
				},
				{
					points: 8,
					reason: "Avaliação média de 4.0 ou mais",
				},
				{
					points: 10,
					reason: "Mais de 100 avaliações, clientela activa",
				},
				{
					points: 5,
					reason: "Telefone público para contacto",
				},
				{
					points: 5,
					reason: "Categoria alinhada com o serviço da campanha",
				},
			],
			stage2: [
				{
					points: 9,
					reason: "Sem marcação online e sem website",
				},
				{
					points: 9,
					reason: "Queixa recorrente nas reviews: demora no atendimento",
				},
				{
					points: 9,
					reason: "Queixa recorrente nas reviews: marcação só por telefone",
				},
			],
		},
		audit: [
			{
				check: "Website",
				result: "FAIL",
				detail: "Não existe. O Google Maps não tem ligação para nenhum site.",
			},
		],
		complaints: [
			{
				theme: "Demora no atendimento",
				mentions: 14,
				quotes: [
					'"Marquei para as 9h e só fui atendido às 11h30. Médicos bons, recepção lenta."',
					'"Esperei duas horas mesmo com consulta marcada."',
				],
			},
			{
				theme: "Marcação só por telefone",
				mentions: 8,
				quotes: ['"Liguei cinco vezes e ninguém atendeu. Não há outra forma de marcar?"'],
			},
		],
		pitch:
			"Olá, boa tarde! Vi a Clínica Santa Luzia no Google Maps, com 4.5 estrelas e 187 avaliações, e reparei que ainda não têm website. Na Raposa Software fazemos sistemas de marcação online para negócios em Talatona. Posso mostrar-lhe um exemplo em 2 minutos?",
		note: "Falei com a recepção. Quem decide é a directora clínica, ligar depois das 14h.",
	},
	{
		id: "l2",
		campaignSlug: "restaurantes-maianga",
		rank: 2,
		name: "Restaurante Mussulo Grill",
		category: "Restaurante",
		area: "Ilha de Luanda",
		address: "Av. da Ilha 220, Ilha de Luanda",
		rating: 4.3,
		reviewCount: 326,
		phone: "+244 924 118 305",
		website: {
			host: "mussulogrill.net",
			https: false,
		},
		stage: "QUALIFIED",
		stageReason: null,
		status: "CONTACTED",
		lostReason: null,
		score: 79,
		breakdown: {
			stage1: [
				{
					points: 10,
					reason: "Website sem HTTPS",
				},
				{
					points: 8,
					reason: "Avaliação média de 4.0 ou mais",
				},
				{
					points: 10,
					reason: "Mais de 100 avaliações, clientela activa",
				},
				{
					points: 5,
					reason: "Telefone público para contacto",
				},
				{
					points: 5,
					reason: "Categoria alinhada com o serviço da campanha",
				},
			],
			stage2: [
				{
					points: 10,
					reason: "Website não se adapta ao telemóvel",
				},
				{
					points: 8,
					reason: "Website desactualizado, rodapé de 2017",
				},
				{
					points: 5,
					reason: "Carrega em mais de 5 segundos",
				},
				{
					points: 9,
					reason: "Sem marcação ou reserva online",
				},
				{
					points: 9,
					reason: "Queixa recorrente nas reviews: reservas difíceis",
				},
			],
		},
		audit: [
			{
				check: "Carrega",
				result: "WARN",
				detail: "Carrega em 6,2 s em 4G",
			},
			{
				check: "Telemóvel",
				result: "FAIL",
				detail: "Não se adapta ao ecrã pequeno",
			},
			{
				check: "HTTPS",
				result: "FAIL",
				detail: "Sem HTTPS, o browser mostra aviso",
			},
			{
				check: "Actualizado",
				result: "WARN",
				detail: "Rodapé de 2017 e design antigo",
			},
			{
				check: "Marcação online",
				result: "FAIL",
				detail: "Sem formulário de marcação ou reserva",
			},
		],
		complaints: [
			{
				theme: "Reservas difíceis",
				mentions: 14,
				quotes: ['"Tentei reservar mesa para sábado, o número dá sempre ocupado."'],
			},
		],
		pitch:
			"Olá, boa tarde! Vi a Restaurante Mussulo Grill no Google Maps, com 4.3 estrelas e 326 avaliações, e reparei que o vosso website tem pontos a melhorar. Na Raposa Software fazemos websites com reserva de mesa para negócios em Ilha de Luanda. Posso mostrar-lhe um exemplo em 2 minutos?",
		note: null,
	},
	{
		id: "l3",
		campaignSlug: "escolas-viana",
		rank: 4,
		name: "Colégio Horizonte Azul",
		category: "Escola",
		area: "Viana",
		address: "Estrada de Catete km 12, Viana",
		rating: 4.2,
		reviewCount: 94,
		phone: "+244 935 204 771",
		website: null,
		stage: "QUALIFIED",
		stageReason: null,
		status: "INTERESTED",
		lostReason: null,
		score: 67,
		breakdown: {
			stage1: [
				{
					points: 25,
					reason: "Sem website no Google Maps",
				},
				{
					points: 8,
					reason: "Avaliação média de 4.0 ou mais",
				},
				{
					points: 6,
					reason: "Mais de 50 avaliações",
				},
				{
					points: 5,
					reason: "Telefone público para contacto",
				},
				{
					points: 5,
					reason: "Categoria alinhada com o serviço da campanha",
				},
			],
			stage2: [
				{
					points: 9,
					reason: "Sem marcação online e sem website",
				},
				{
					points: 9,
					reason: "Queixa recorrente nas reviews: comunicação com os pais",
				},
			],
		},
		audit: [
			{
				check: "Website",
				result: "FAIL",
				detail: "Não existe. O Google Maps não tem ligação para nenhum site.",
			},
		],
		complaints: [
			{
				theme: "Comunicação com os pais",
				mentions: 14,
				quotes: ['"Só soubemos da reunião no próprio dia. Falta um canal para avisos."'],
			},
		],
		pitch:
			"Olá, boa tarde! Vi a Colégio Horizonte Azul no Google Maps, com 4.2 estrelas e 94 avaliações, e reparei que ainda não têm website. Na Raposa Software fazemos portais para comunicar com os pais para negócios em Viana. Posso mostrar-lhe um exemplo em 2 minutos?",
		note: null,
	},
	{
		id: "l4",
		campaignSlug: "oficinas-zango",
		rank: 3,
		name: "Oficina AutoMax Viana",
		category: "Oficina auto",
		area: "Viana",
		address: "Zona Industrial de Viana, Rua 3",
		rating: 4.4,
		reviewCount: 128,
		phone: "+244 927 660 143",
		website: null,
		stage: "QUALIFIED",
		stageReason: null,
		status: "NEW",
		lostReason: null,
		score: 71,
		breakdown: {
			stage1: [
				{
					points: 25,
					reason: "Sem website no Google Maps",
				},
				{
					points: 8,
					reason: "Avaliação média de 4.0 ou mais",
				},
				{
					points: 10,
					reason: "Mais de 100 avaliações, clientela activa",
				},
				{
					points: 5,
					reason: "Telefone público para contacto",
				},
				{
					points: 5,
					reason: "Categoria alinhada com o serviço da campanha",
				},
			],
			stage2: [
				{
					points: 9,
					reason: "Sem marcação online e sem website",
				},
				{
					points: 9,
					reason: "Queixa recorrente nas reviews: orçamento demorado",
				},
			],
		},
		audit: [
			{
				check: "Website",
				result: "FAIL",
				detail: "Não existe. O Google Maps não tem ligação para nenhum site.",
			},
		],
		complaints: [
			{
				theme: "Orçamento demorado",
				mentions: 14,
				quotes: ['"Pedi orçamento e esperei uma semana por resposta."'],
			},
		],
		pitch:
			"Olá, boa tarde! Vi a Oficina AutoMax Viana no Google Maps, com 4.4 estrelas e 128 avaliações, e reparei que ainda não têm website. Na Raposa Software fazemos websites com pedido de orçamento por WhatsApp para negócios em Viana. Posso mostrar-lhe um exemplo em 2 minutos?",
		note: null,
	},
	{
		id: "l5",
		campaignSlug: "clinicas-talatona",
		rank: 6,
		name: "Clínica Dentária Sorriso",
		category: "Clínica dentária",
		area: "Maianga",
		address: "Rua Comandante Gika 55, Maianga",
		rating: 4.7,
		reviewCount: 241,
		phone: "+244 922 580 019",
		website: {
			host: "sorrisoclinica.com",
			https: true,
		},
		stage: "QUALIFIED",
		stageReason: null,
		status: "MEETING",
		lostReason: null,
		score: 61,
		breakdown: {
			stage1: [
				{
					points: 8,
					reason: "Avaliação média de 4.0 ou mais",
				},
				{
					points: 10,
					reason: "Mais de 100 avaliações, clientela activa",
				},
				{
					points: 5,
					reason: "Telefone público para contacto",
				},
				{
					points: 5,
					reason: "Categoria alinhada com o serviço da campanha",
				},
			],
			stage2: [
				{
					points: 10,
					reason: "Website não se adapta ao telemóvel",
				},
				{
					points: 5,
					reason: "Carrega em mais de 5 segundos",
				},
				{
					points: 9,
					reason: "Sem marcação ou reserva online",
				},
				{
					points: 9,
					reason: "Queixa recorrente nas reviews: tempo de espera",
				},
			],
		},
		audit: [
			{
				check: "Carrega",
				result: "WARN",
				detail: "Carrega em 6,2 s em 4G",
			},
			{
				check: "Telemóvel",
				result: "FAIL",
				detail: "Não se adapta ao ecrã pequeno",
			},
			{
				check: "HTTPS",
				result: "OK",
				detail: "Certificado válido",
			},
			{
				check: "Actualizado",
				result: "OK",
				detail: "Conteúdo recente",
			},
			{
				check: "Marcação online",
				result: "FAIL",
				detail: "Sem formulário de marcação ou reserva",
			},
		],
		complaints: [
			{
				theme: "Tempo de espera",
				mentions: 14,
				quotes: ['"Chegámos à hora e esperámos quase uma hora na sala."'],
			},
		],
		pitch:
			"Olá, boa tarde! Vi a Clínica Dentária Sorriso no Google Maps, com 4.7 estrelas e 241 avaliações, e reparei que o vosso website tem pontos a melhorar. Na Raposa Software fazemos sistemas de marcação online para negócios em Maianga. Posso mostrar-lhe um exemplo em 2 minutos?",
		note: null,
	},
	{
		id: "l6",
		campaignSlug: "restaurantes-maianga",
		rank: 8,
		name: "Pizzaria Napoli Kilamba",
		category: "Restaurante",
		area: "Kilamba",
		address: "Quarteirão M, Kilamba",
		rating: 4.1,
		reviewCount: 77,
		phone: "+244 931 447 902",
		website: null,
		stage: "QUALIFIED",
		stageReason: null,
		status: "NEW",
		lostReason: null,
		score: 49,
		breakdown: {
			stage1: [
				{
					points: 25,
					reason: "Sem website no Google Maps",
				},
				{
					points: 8,
					reason: "Avaliação média de 4.0 ou mais",
				},
				{
					points: 6,
					reason: "Mais de 50 avaliações",
				},
				{
					points: 5,
					reason: "Telefone público para contacto",
				},
				{
					points: 5,
					reason: "Categoria alinhada com o serviço da campanha",
				},
			],
			stage2: null,
		},
		audit: [
			{
				check: "Website",
				result: "FAIL",
				detail: "Não existe. O Google Maps não tem ligação para nenhum site.",
			},
		],
		complaints: null,
		pitch:
			"Olá, boa tarde! Vi a Pizzaria Napoli Kilamba no Google Maps, com 4.1 estrelas e 77 avaliações, e reparei que ainda não têm website. Na Raposa Software fazemos websites com reserva de mesa para negócios em Kilamba. Posso mostrar-lhe um exemplo em 2 minutos?",
		note: null,
	},
	{
		id: "l7",
		campaignSlug: "escolas-viana",
		rank: 13,
		name: "Escola Os Pequenos Sábios",
		category: "Escola",
		area: "Cazenga",
		address: "Rua do Cazenga 41, Cazenga",
		rating: 3.9,
		reviewCount: 58,
		phone: "+244 926 305 618",
		website: null,
		stage: "QUALIFIED",
		stageReason: null,
		status: "NEW",
		lostReason: null,
		score: 41,
		breakdown: {
			stage1: [
				{
					points: 25,
					reason: "Sem website no Google Maps",
				},
				{
					points: 6,
					reason: "Mais de 50 avaliações",
				},
				{
					points: 5,
					reason: "Telefone público para contacto",
				},
				{
					points: 5,
					reason: "Categoria alinhada com o serviço da campanha",
				},
			],
			stage2: null,
		},
		audit: [
			{
				check: "Website",
				result: "FAIL",
				detail: "Não existe. O Google Maps não tem ligação para nenhum site.",
			},
		],
		complaints: null,
		pitch:
			"Olá, boa tarde! Vi a Escola Os Pequenos Sábios no Google Maps, com 3.9 estrelas e 58 avaliações, e reparei que ainda não têm website. Na Raposa Software fazemos portais para comunicar com os pais para negócios em Cazenga. Posso mostrar-lhe um exemplo em 2 minutos?",
		note: null,
	},
	{
		id: "l8",
		campaignSlug: "oficinas-zango",
		rank: 12,
		name: "Mecânica Rápida do Zango",
		category: "Oficina auto",
		area: "Zango",
		address: "Zango 3, Via Expressa",
		rating: 4,
		reviewCount: 63,
		phone: null,
		website: null,
		stage: "QUALIFIED",
		stageReason: null,
		status: "NEW",
		lostReason: null,
		score: 44,
		breakdown: {
			stage1: [
				{
					points: 25,
					reason: "Sem website no Google Maps",
				},
				{
					points: 8,
					reason: "Avaliação média de 4.0 ou mais",
				},
				{
					points: 6,
					reason: "Mais de 50 avaliações",
				},
				{
					points: 5,
					reason: "Categoria alinhada com o serviço da campanha",
				},
			],
			stage2: null,
		},
		audit: [
			{
				check: "Website",
				result: "FAIL",
				detail: "Não existe. O Google Maps não tem ligação para nenhum site.",
			},
		],
		complaints: null,
		pitch:
			"Olá, boa tarde! Vi a Mecânica Rápida do Zango no Google Maps, com 4.0 estrelas e 63 avaliações, e reparei que ainda não têm website. Na Raposa Software fazemos websites com pedido de orçamento por WhatsApp para negócios em Zango. Posso mostrar-lhe um exemplo em 2 minutos?",
		note: null,
	},
	{
		id: "l9",
		campaignSlug: "clinicas-talatona",
		rank: 7,
		name: "Laboratório BioAnálises",
		category: "Laboratório",
		area: "Maianga",
		address: "Rua Dr. António Agostinho Neto 90, Maianga",
		rating: 4.4,
		reviewCount: 109,
		phone: "+244 923 771 560",
		website: {
			host: "bioanalises-ao.com",
			https: true,
		},
		stage: "QUALIFIED",
		stageReason: null,
		status: "LOST",
		lostReason: "NOT_NOW",
		score: 54,
		breakdown: {
			stage1: [
				{
					points: 8,
					reason: "Avaliação média de 4.0 ou mais",
				},
				{
					points: 10,
					reason: "Mais de 100 avaliações, clientela activa",
				},
				{
					points: 5,
					reason: "Telefone público para contacto",
				},
				{
					points: 5,
					reason: "Categoria alinhada com o serviço da campanha",
				},
			],
			stage2: [
				{
					points: 8,
					reason: "Website desactualizado, rodapé de 2017",
				},
				{
					points: 9,
					reason: "Sem marcação ou reserva online",
				},
				{
					points: 9,
					reason: "Queixa recorrente nas reviews: resultados atrasados",
				},
			],
		},
		audit: [
			{
				check: "Carrega",
				result: "OK",
				detail: "Carrega em 1,8 s em 4G",
			},
			{
				check: "Telemóvel",
				result: "OK",
				detail: "Adaptado ao ecrã pequeno",
			},
			{
				check: "HTTPS",
				result: "OK",
				detail: "Certificado válido",
			},
			{
				check: "Actualizado",
				result: "WARN",
				detail: "Rodapé de 2017 e design antigo",
			},
			{
				check: "Marcação online",
				result: "FAIL",
				detail: "Sem formulário de marcação ou reserva",
			},
		],
		complaints: [
			{
				theme: "Resultados atrasados",
				mentions: 14,
				quotes: ['"Os resultados prometidos para 24h demoraram quatro dias."'],
			},
		],
		pitch:
			"Olá, boa tarde! Vi a Laboratório BioAnálises no Google Maps, com 4.4 estrelas e 109 avaliações, e reparei que o vosso website tem pontos a melhorar. Na Raposa Software fazemos sistemas de marcação online para negócios em Maianga. Posso mostrar-lhe um exemplo em 2 minutos?",
		note: null,
	},
	{
		id: "l10",
		campaignSlug: "restaurantes-maianga",
		rank: 11,
		name: "Restaurante Cantinho do Mar",
		category: "Restaurante",
		area: "Miramar",
		address: "Rua Rainha Ginga 7, Miramar",
		rating: 4.5,
		reviewCount: 402,
		phone: "+244 929 331 884",
		website: {
			host: "cantinhodomar.ao",
			https: true,
		},
		stage: "QUALIFIED",
		stageReason: null,
		status: "NEW",
		lostReason: null,
		score: 45,
		breakdown: {
			stage1: [
				{
					points: 8,
					reason: "Avaliação média de 4.0 ou mais",
				},
				{
					points: 10,
					reason: "Mais de 100 avaliações, clientela activa",
				},
				{
					points: 5,
					reason: "Telefone público para contacto",
				},
				{
					points: 5,
					reason: "Categoria alinhada com o serviço da campanha",
				},
			],
			stage2: [
				{
					points: 8,
					reason: "Website desactualizado, rodapé de 2017",
				},
				{
					points: 9,
					reason: "Sem marcação ou reserva online",
				},
			],
		},
		audit: [
			{
				check: "Carrega",
				result: "OK",
				detail: "Carrega em 1,8 s em 4G",
			},
			{
				check: "Telemóvel",
				result: "OK",
				detail: "Adaptado ao ecrã pequeno",
			},
			{
				check: "HTTPS",
				result: "OK",
				detail: "Certificado válido",
			},
			{
				check: "Actualizado",
				result: "WARN",
				detail: "Rodapé de 2017 e design antigo",
			},
			{
				check: "Marcação online",
				result: "FAIL",
				detail: "Sem formulário de marcação ou reserva",
			},
		],
		complaints: [],
		pitch:
			"Olá, boa tarde! Vi a Restaurante Cantinho do Mar no Google Maps, com 4.5 estrelas e 402 avaliações, e reparei que o vosso website tem pontos a melhorar. Na Raposa Software fazemos websites com reserva de mesa para negócios em Miramar. Posso mostrar-lhe um exemplo em 2 minutos?",
		note: null,
	},
	{
		id: "l11",
		campaignSlug: "clinicas-talatona",
		rank: 16,
		name: "Centro Médico Vitória",
		category: "Clínica médica",
		area: "Alvalade",
		address: "Rua Che Guevara 33, Alvalade",
		rating: 4.3,
		reviewCount: 156,
		phone: "+244 924 902 237",
		website: {
			host: "centromedicovitoria.co.ao",
			https: true,
		},
		stage: "BELOW_CUT",
		stageReason: null,
		status: "NEW",
		lostReason: null,
		score: 28,
		breakdown: {
			stage1: [
				{
					points: 8,
					reason: "Avaliação média de 4.0 ou mais",
				},
				{
					points: 10,
					reason: "Mais de 100 avaliações, clientela activa",
				},
				{
					points: 5,
					reason: "Telefone público para contacto",
				},
				{
					points: 5,
					reason: "Categoria alinhada com o serviço da campanha",
				},
			],
			stage2: null,
		},
		audit: [
			{
				check: "HTTPS",
				result: "OK",
				detail: "Certificado válido",
			},
			{
				check: "Restante auditoria",
				result: "PENDING",
				detail: "Corre no Stage 2 (enriquecimento)",
			},
		],
		complaints: null,
		pitch:
			"Olá, boa tarde! Vi a Centro Médico Vitória no Google Maps, com 4.3 estrelas e 156 avaliações, e reparei que o vosso website tem pontos a melhorar. Na Raposa Software fazemos sistemas de marcação online para negócios em Alvalade. Posso mostrar-lhe um exemplo em 2 minutos?",
		note: null,
	},
	{
		id: "l12",
		campaignSlug: "oficinas-zango",
		rank: null,
		name: "Auto Peças Benfica",
		category: "Oficina auto",
		area: "Benfica",
		address: "Rua da Samba 5, Benfica",
		rating: 4.2,
		reviewCount: 45,
		phone: "+244 928 115 640",
		website: null,
		stage: "EXCLUDED",
		stageReason: "Menos de 50 avaliações (45)",
		status: "NEW",
		lostReason: null,
		score: null,
		breakdown: {
			stage1: [],
			stage2: null,
		},
		audit: [
			{
				check: "Website",
				result: "FAIL",
				detail: "Não existe. O Google Maps não tem ligação para nenhum site.",
			},
		],
		complaints: null,
		pitch:
			"Olá, boa tarde! Vi a Auto Peças Benfica no Google Maps, com 4.2 estrelas e 45 avaliações, e reparei que ainda não têm website. Na Raposa Software fazemos websites com pedido de orçamento por WhatsApp para negócios em Benfica. Posso mostrar-lhe um exemplo em 2 minutos?",
		note: null,
	},
	{
		id: "l13",
		campaignSlug: "clinicas-talatona",
		rank: null,
		name: "Clínica Esperança",
		category: "Clínica médica",
		area: "Alvalade",
		address: "Rua Che Guevara 80, Alvalade",
		rating: 4.6,
		reviewCount: 210,
		phone: "+244 923 600 481",
		website: null,
		stage: "EXCLUDED",
		stageReason: "Cliente existente (lista de clientes anteriores)",
		status: "WON",
		lostReason: null,
		score: null,
		breakdown: {
			stage1: [],
			stage2: null,
		},
		audit: [
			{
				check: "Website",
				result: "FAIL",
				detail: "Não existe. O Google Maps não tem ligação para nenhum site.",
			},
		],
		complaints: null,
		pitch:
			"Olá, boa tarde! Vi a Clínica Esperança no Google Maps, com 4.6 estrelas e 210 avaliações, e reparei que ainda não têm website. Na Raposa Software fazemos sistemas de marcação online para negócios em Alvalade. Posso mostrar-lhe um exemplo em 2 minutos?",
		note: null,
	},
	{
		id: "l14",
		campaignSlug: "escolas-viana",
		rank: 14,
		name: "Colégio São Gabriel",
		category: "Escola",
		area: "Talatona",
		address: "Condomínio Cidade Financeira, Talatona",
		rating: 4,
		reviewCount: 71,
		phone: "+244 932 840 126",
		website: {
			host: "colegiosaogabriel.org",
			https: false,
		},
		stage: "BELOW_CUT",
		stageReason: null,
		status: "NEW",
		lostReason: null,
		score: 34,
		breakdown: {
			stage1: [
				{
					points: 10,
					reason: "Website sem HTTPS",
				},
				{
					points: 8,
					reason: "Avaliação média de 4.0 ou mais",
				},
				{
					points: 6,
					reason: "Mais de 50 avaliações",
				},
				{
					points: 5,
					reason: "Telefone público para contacto",
				},
				{
					points: 5,
					reason: "Categoria alinhada com o serviço da campanha",
				},
			],
			stage2: null,
		},
		audit: [
			{
				check: "HTTPS",
				result: "FAIL",
				detail: "Sem HTTPS, o browser mostra aviso",
			},
			{
				check: "Restante auditoria",
				result: "PENDING",
				detail: "Corre no Stage 2 (enriquecimento)",
			},
		],
		complaints: null,
		pitch:
			"Olá, boa tarde! Vi a Colégio São Gabriel no Google Maps, com 4.0 estrelas e 71 avaliações, e reparei que o vosso website tem pontos a melhorar. Na Raposa Software fazemos portais para comunicar com os pais para negócios em Talatona. Posso mostrar-lhe um exemplo em 2 minutos?",
		note: null,
	},
	{
		id: "l15",
		campaignSlug: "restaurantes-maianga",
		rank: 17,
		name: "Pastelaria Doce Lar",
		category: "Restaurante",
		area: "Maianga",
		address: "Rua Rei Katyavala 12, Maianga",
		rating: 3.8,
		reviewCount: 52,
		phone: "+244 925 417 090",
		website: {
			host: "docelar-pastelaria.com",
			https: true,
		},
		stage: "BELOW_CUT",
		stageReason: null,
		status: "NEW",
		lostReason: null,
		score: 16,
		breakdown: {
			stage1: [
				{
					points: 6,
					reason: "Mais de 50 avaliações",
				},
				{
					points: 5,
					reason: "Telefone público para contacto",
				},
				{
					points: 5,
					reason: "Categoria alinhada com o serviço da campanha",
				},
			],
			stage2: null,
		},
		audit: [
			{
				check: "HTTPS",
				result: "OK",
				detail: "Certificado válido",
			},
			{
				check: "Restante auditoria",
				result: "PENDING",
				detail: "Corre no Stage 2 (enriquecimento)",
			},
		],
		complaints: null,
		pitch:
			"Olá, boa tarde! Vi a Pastelaria Doce Lar no Google Maps, com 3.8 estrelas e 52 avaliações, e reparei que o vosso website tem pontos a melhorar. Na Raposa Software fazemos websites com reserva de mesa para negócios em Maianga. Posso mostrar-lhe um exemplo em 2 minutos?",
		note: null,
	},
	{
		id: "l16",
		campaignSlug: "restaurantes-maianga",
		rank: 9,
		name: "Restaurante O Panorama",
		category: "Restaurante",
		area: "Miramar",
		address: "Av. 4 de Fevereiro 300, Miramar",
		rating: 4.2,
		reviewCount: 88,
		phone: "+244 937 556 120",
		website: null,
		stage: "QUALIFIED",
		stageReason: null,
		status: "NEW",
		lostReason: null,
		score: 49,
		breakdown: {
			stage1: [
				{
					points: 25,
					reason: "Sem website no Google Maps",
				},
				{
					points: 8,
					reason: "Avaliação média de 4.0 ou mais",
				},
				{
					points: 6,
					reason: "Mais de 50 avaliações",
				},
				{
					points: 5,
					reason: "Telefone público para contacto",
				},
				{
					points: 5,
					reason: "Categoria alinhada com o serviço da campanha",
				},
			],
			stage2: null,
		},
		audit: [
			{
				check: "Website",
				result: "FAIL",
				detail: "Não existe. O Google Maps não tem ligação para nenhum site.",
			},
		],
		complaints: null,
		pitch:
			"Olá, boa tarde! Vi a Restaurante O Panorama no Google Maps, com 4.2 estrelas e 88 avaliações, e reparei que ainda não têm website. Na Raposa Software fazemos websites com reserva de mesa para negócios em Miramar. Posso mostrar-lhe um exemplo em 2 minutos?",
		note: null,
	},
	{
		id: "l17",
		campaignSlug: "escolas-viana",
		rank: 15,
		name: "Escola Criativa Luanda Sul",
		category: "Escola",
		area: "Belas",
		address: "Urbanização Nova Vida, Belas",
		rating: 4.1,
		reviewCount: 66,
		phone: "+244 930 228 714",
		website: {
			host: "criativaluandasul.com",
			https: false,
		},
		stage: "QUALIFIED",
		stageReason: null,
		status: "NEW",
		lostReason: null,
		score: 34,
		breakdown: {
			stage1: [
				{
					points: 10,
					reason: "Website sem HTTPS",
				},
				{
					points: 8,
					reason: "Avaliação média de 4.0 ou mais",
				},
				{
					points: 6,
					reason: "Mais de 50 avaliações",
				},
				{
					points: 5,
					reason: "Telefone público para contacto",
				},
				{
					points: 5,
					reason: "Categoria alinhada com o serviço da campanha",
				},
			],
			stage2: null,
		},
		audit: [
			{
				check: "HTTPS",
				result: "FAIL",
				detail: "Sem HTTPS, o browser mostra aviso",
			},
			{
				check: "Restante auditoria",
				result: "PENDING",
				detail: "Corre no Stage 2 (enriquecimento)",
			},
		],
		complaints: null,
		pitch:
			"Olá, boa tarde! Vi a Escola Criativa Luanda Sul no Google Maps, com 4.1 estrelas e 66 avaliações, e reparei que o vosso website tem pontos a melhorar. Na Raposa Software fazemos portais para comunicar com os pais para negócios em Belas. Posso mostrar-lhe um exemplo em 2 minutos?",
		note: null,
	},
	{
		id: "l18",
		campaignSlug: "clinicas-talatona",
		rank: 5,
		name: "Clínica Veterinária Pet Vida",
		category: "Clínica veterinária",
		area: "Talatona",
		address: "Rua dos Coqueiros 9, Talatona",
		rating: 4.4,
		reviewCount: 74,
		phone: "+244 922 905 363",
		website: null,
		stage: "QUALIFIED",
		stageReason: null,
		status: "NEW",
		lostReason: null,
		score: 67,
		breakdown: {
			stage1: [
				{
					points: 25,
					reason: "Sem website no Google Maps",
				},
				{
					points: 8,
					reason: "Avaliação média de 4.0 ou mais",
				},
				{
					points: 6,
					reason: "Mais de 50 avaliações",
				},
				{
					points: 5,
					reason: "Telefone público para contacto",
				},
				{
					points: 5,
					reason: "Categoria alinhada com o serviço da campanha",
				},
			],
			stage2: [
				{
					points: 9,
					reason: "Sem marcação online e sem website",
				},
				{
					points: 9,
					reason: "Queixa recorrente nas reviews: tempo de espera",
				},
			],
		},
		audit: [
			{
				check: "Website",
				result: "FAIL",
				detail: "Não existe. O Google Maps não tem ligação para nenhum site.",
			},
		],
		complaints: [
			{
				theme: "Tempo de espera",
				mentions: 14,
				quotes: ['"Chegámos à hora e esperámos quase uma hora na sala."'],
			},
		],
		pitch:
			"Olá, boa tarde! Vi a Clínica Veterinária Pet Vida no Google Maps, com 4.4 estrelas e 74 avaliações, e reparei que ainda não têm website. Na Raposa Software fazemos sistemas de marcação online para negócios em Talatona. Posso mostrar-lhe um exemplo em 2 minutos?",
		note: null,
	},
	{
		id: "l19",
		campaignSlug: "oficinas-zango",
		rank: 10,
		name: "Auto Kilamba Motors",
		category: "Oficina auto",
		area: "Kilamba",
		address: "Quarteirão B, Kilamba",
		rating: 4.3,
		reviewCount: 91,
		phone: "+244 927 118 452",
		website: null,
		stage: "QUALIFIED",
		stageReason: null,
		status: "NEW",
		lostReason: null,
		score: 49,
		breakdown: {
			stage1: [
				{
					points: 25,
					reason: "Sem website no Google Maps",
				},
				{
					points: 8,
					reason: "Avaliação média de 4.0 ou mais",
				},
				{
					points: 6,
					reason: "Mais de 50 avaliações",
				},
				{
					points: 5,
					reason: "Telefone público para contacto",
				},
				{
					points: 5,
					reason: "Categoria alinhada com o serviço da campanha",
				},
			],
			stage2: null,
		},
		audit: [
			{
				check: "Website",
				result: "FAIL",
				detail: "Não existe. O Google Maps não tem ligação para nenhum site.",
			},
		],
		complaints: null,
		pitch:
			"Olá, boa tarde! Vi a Auto Kilamba Motors no Google Maps, com 4.3 estrelas e 91 avaliações, e reparei que ainda não têm website. Na Raposa Software fazemos websites com pedido de orçamento por WhatsApp para negócios em Kilamba. Posso mostrar-lhe um exemplo em 2 minutos?",
		note: null,
	},
];
