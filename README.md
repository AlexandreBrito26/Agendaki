# Agendaki (nome provisório)

Mini-SaaS de agendamentos para pequenos negócios (barbearias, salões, estúdios). O dono configura serviços, profissionais e horários; o cliente agenda por uma página pública, sem precisar criar conta.

## Stack

| Camada | Tecnologia |
| --- | --- |
| Frontend | React + TypeScript (Vite), SCSS |
| Backend | Java 21 + Spring Boot 3 (Web, Data JPA, Security, Validation) |
| Banco | PostgreSQL |
| Auth | JWT (dono do negócio) |
| Deploy | Um container Docker só (API + front embutido) + PostgreSQL no Render |

## Estrutura

```
agendaki/
├── backend/        # API Spring Boot
├── frontend/       # React + TypeScript (criar com Vite)
├── docs/           # modelo de dados e decisões
└── README.md
```

## Escopo do MVP

**Painel do dono (autenticado)**
- Cadastro/login
- CRUD de serviços (nome, duração, preço)
- CRUD de profissionais e quais serviços cada um faz
- Horários de funcionamento por profissional e dia da semana
- Lista de agendamentos (filtro por dia/profissional) e cancelamento

**Página pública (`/{slug-do-negocio}`)**
- Escolhe serviço → profissional → dia → horário livre
- Informa nome e telefone e confirma
- Cancelamento/remarcação por link único (token)

**Regras de agenda**
- Horário livre = janela de trabalho do profissional − agendamentos existentes, respeitando a duração do serviço
- Sem sobreposição de agendamentos do mesmo profissional (validar no service **e** no banco)
- Não agendar no passado

## Roadmap

1. **Fase 1 – Base:** modelo de dados, auth, CRUD de serviços/profissionais
2. **Fase 2 – Agenda:** cálculo de horários livres, criação/cancelamento de agendamentos
3. **Fase 3 – Front:** painel do dono e página pública
4. **Fase 4 – Polimento:** e-mail de confirmação/lembrete, deploy, README com prints
5. **Depois:** multi-tenant completo, WhatsApp, relatórios, pagamento de sinal

## Como rodar (backend)

```bash
# banco local
docker run --name agendaki-db -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=agendaki -p 5432:5432 -d postgres:16

cd backend
mvn spring-boot:run
# (opcional) gerar o Maven Wrapper: mvn wrapper:wrapper
```

O Flyway aplica `src/main/resources/db/migration/V1__schema_inicial.sql` na primeira subida (cópia de `docs/schema.sql`).

## API (por enquanto)

| Método | Rota | Descrição |
| --- | --- | --- |
| POST | `/api/auth/register` | Cria o negócio + conta do dono. Body: `businessName`, `slug`, `email`, `password` |
| POST | `/api/auth/login` | Login. Body: `email`, `password` |

| GET/POST | `/api/services` | Lista / cria serviços. Body: `name`, `durationMin`, `priceCents` |
| GET/PUT/DELETE | `/api/services/{id}` | Detalhe / edita (`active` opcional) / desativa |
| GET/POST | `/api/professionals` | Lista / cria profissionais. Body: `name`, `serviceIds` |
| GET/PUT/DELETE | `/api/professionals/{id}` | Detalhe / edita (`serviceIds`, `active`) / desativa |
| GET/PUT | `/api/professionals/{id}/working-hours` | Lê / **substitui** a grade semanal. Body: lista de `{ weekday (0=dom…6=sáb), startTime "HH:mm", endTime "HH:mm" }` |
| GET | `/api/appointments?date=YYYY-MM-DD&professionalId=` | Agenda do dono em um dia (padrão: hoje) |
| POST | `/api/appointments/{id}/cancel` | Dono cancela um agendamento |

**Endpoints públicos (sem login)**

| Método | Rota | Descrição |
| --- | --- | --- |
| GET | `/api/public/businesses/{slug}` | Catálogo: serviços e profissionais ativos |
| GET | `/api/public/businesses/{slug}/availability?serviceId=&professionalId=&date=YYYY-MM-DD` | Horários livres (`["09:00","09:15",…]`) |
| POST | `/api/public/businesses/{slug}/appointments` | Agenda. Body: `serviceId`, `professionalId`, `date`, `time`, `customerName`, `customerPhone`, `customerEmail` (opcional). Retorna o `manageToken` |
| GET | `/api/public/appointments/{token}` | Consulta pelo link do cliente |
| POST | `/api/public/appointments/{token}/cancel` | Cliente cancela |
| POST | `/api/public/appointments/{token}/reschedule` | Cliente remarca. Body: `date`, `time` |

Regras: horários são oferecidos de 15 em 15 minutos (`app.booking.slot-step-minutes`), até 60 dias à frente, nunca no passado. Se dois clientes pegarem o mesmo horário ao mesmo tempo, o banco barra o segundo e a API responde 409.

Rotas protegidas (tudo exceto `/api/auth/**`) exigem o token. `DELETE` só desativa, porque agendamentos antigos referenciam o registro.

`register`/`login` retornam `{ token, businessId, businessName, slug }`. Nas rotas protegidas, envie `Authorization: Bearer <token>`. Rotas em `/api/public/**` são abertas (página de agendamento do cliente).

Teste rápido:

```bash
curl -X POST localhost:8080/api/auth/register -H 'Content-Type: application/json' \
  -d '{"businessName":"Barbearia do Zé","slug":"barbearia-do-ze","email":"ze@email.com","password":"senha12345"}'
```

Variáveis de ambiente úteis: `DB_URL` (ou `DB_HOST`/`DB_PORT`/`DB_NAME`), `DB_USER`, `DB_PASSWORD`, `JWT_SECRET` (mín. 32 caracteres), `CORS_ORIGINS`.

## E-mails (confirmação e lembrete)

O e-mail do cliente é opcional no formulário. Se informado, ele recebe a confirmação ao agendar e um lembrete quando faltar menos de 24h (job a cada 15 min; quem agenda com menos de 24h de antecedência só recebe a confirmação; remarcar zera o lembrete). Migração: `V2__email_cliente.sql`.

Por padrão `MAIL_ENABLED=false`: o e-mail só aparece no log, então tudo roda sem SMTP. Para enviar de verdade:

```
MAIL_ENABLED=true  MAIL_FROM=agenda@seudominio.com
SMTP_HOST=smtp.exemplo.com  SMTP_PORT=587  SMTP_USER=...  SMTP_PASSWORD=...
PUBLIC_URL=https://seu-front.vercel.app   # base dos links "remarcar/cancelar"
```

> ⚠️ Esta parte (V2, `AppointmentMailListener`, `ReminderJob`) foi escrita sem poder rodar `mvn` (sem acesso ao Maven Central no ambiente em que foi feita): só passou por checagem de sintaxe. Rode `mvn test` e faça um agendamento com e-mail antes de confiar.

## Testes

```bash
cd backend
mvn test    # unitários + BookingFlowTest (precisa de Docker: sobe um PostgreSQL via Testcontainers)
```

`BookingFlowTest` cobre o fluxo completo contra um Postgres real (a regra de não-sobreposição depende da constraint `EXCLUDE`): dono configura o negócio → cliente agenda, remarca e cancela → horário volta a ficar livre; conflito de horário (409), validações, rotas protegidas e isolamento entre negócios.

## Deploy

Passo a passo completo: [docs/DEPLOY.md](docs/DEPLOY.md).

1. **Tudo no Render:** `render.yaml` na raiz é um Blueprint (New → Blueprint). O `Dockerfile` da raiz builda o front (Vite), embute o resultado na API Spring Boot e sobe um container só, junto com o PostgreSQL. Não precisa de Vercel, `VITE_API_URL` nem CORS: front e API ficam no mesmo domínio.
2. Preencha `PUBLIC_URL` com a URL do próprio app no Render (só é usada nos links dos e-mails).
3. Crie sua conta em `/cadastro`, cadastre serviços e profissionais (com horários) e compartilhe `https://seu-app.onrender.com/seu-slug`.

O plano free do Render dorme após inatividade (a 1ª requisição demora).

## Frontend (página pública de agendamento)

React + TypeScript + Vite + SCSS + react-router. Rotas:

| Rota | O que faz |
| --- | --- |
| `/` | Campo para digitar o endereço (slug) do negócio |
| `/:slug` | Fluxo do cliente: serviço → profissional → dia/horário → nome e telefone |
| `/:slug/agendamento/:token` | Link do cliente: ver, remarcar ou cancelar (sem conta) |
| `/cadastro`, `/entrar` | Cria a conta do negócio / login do dono (JWT guardado no `localStorage`; 401 desloga) |
| `/painel` | **Agenda** do dia (filtro por profissional, cancelar, telefone abre WhatsApp e botão "Lembrar no WhatsApp" com mensagem pronta) |
| `/painel/servicos` | CRUD de serviços (nome, duração, preço; desativar/reativar) |
| `/painel/profissionais` | CRUD de profissionais, serviços que fazem e grade semanal (várias janelas por dia) |

**Visual e usabilidade:** 8 temas (Floresta, Oceano, Vinho e Âmbar de dia; Noite, Meia-noite, Grafite e Ameixa à noite) com seletor em todas as páginas, salvo no aparelho e seguindo o modo claro/escuro do sistema na primeira visita. Horários agrupados em manhã/tarde/noite, esqueletos de carregamento, "ver o próximo dia com horário", dados do cliente lembrados no aparelho e botões "Google Agenda"/.ics na confirmação.

`entrar`, `cadastro` e `painel` são rotas fixas; o cadastro barra esses slugs.

```bash
cd frontend
npm install
npm run dev      # http://localhost:5173 — /api é repassado para localhost:8080 (proxy do Vite)
npm run build
```

Em produção o front é servido pela própria API (mesmo domínio), então não há variável a configurar.

**Vercel (opcional):** o `frontend/vercel.json` repassa `/api/*` para a API no Render (troque `agendaki-3rp3.onrender.com` pela URL real do seu serviço), então também não precisa de `VITE_API_URL` nem de CORS. Na Vercel: *Root Directory* = `frontend`.

Detalhes: os dias e o "hoje" são calculados no fuso do negócio (`timezone` do catálogo); se o horário for tomado por outro cliente (409), o fluxo volta à escolha de horário com a mensagem da API.

**Próximo:** prints no README, testes de integração do fluxo de agendamento, e depois WhatsApp/relatórios.
