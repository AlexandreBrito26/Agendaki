# Checklist de deploy

Ordem importa: a API precisa da URL do front (CORS) e o front precisa da URL da API. Por isso o primeiro deploy da API sobe com valores provisórios e é ajustado no fim.

## 0. Antes de tudo
- [ ] `cd backend && mvn test` passa (precisa de Docker, por causa do `BookingFlowTest`)
- [ ] `cd frontend && npm run lint && npm run build` passa
- [ ] Código no GitHub (um repositório só, com `backend/` e `frontend/`)

## 1. API + banco no Render
1. Render → **New → Blueprint** → escolha o repositório (usa o `render.yaml` da raiz).
2. Quando pedir os valores sem padrão, preencha provisoriamente:
   - `CORS_ORIGINS` = `https://temporario.vercel.app`
   - `PUBLIC_URL` = `https://temporario.vercel.app`
3. Espere o build do Docker terminar e anote a URL da API (ex.: `https://agendaki-api.onrender.com`).
4. Teste: `curl https://SUA-API/api/public/businesses/nao-existe` deve responder **404** (a API e o banco estão de pé; o Flyway já criou as tabelas).

> O plano free do Render dorme após ~15 min sem uso: a primeira requisição demora. Para portfólio, avise isso no README ou use um pinger.

## 2. Front na Vercel
1. Vercel → **Add New → Project** → mesmo repositório.
2. **Root Directory** = `frontend` (framework Vite é detectado).
3. Variável de ambiente: `VITE_API_URL` = URL da API do passo 1 (sem barra no final).
4. Deploy e anote o domínio (ex.: `https://agendaki.vercel.app`).

## 3. Fechar o ciclo
No Render, edite as variáveis da API e faça redeploy:
- `CORS_ORIGINS` = domínio real do front (sem barra no final; vários separados por vírgula)
- `PUBLIC_URL` = domínio real do front (base dos links "remarcar/cancelar" dos e-mails)

## 4. Teste de fumaça em produção
- [ ] Abrir o front → `/cadastro` → criar conta do negócio (sem erro de CORS no console)
- [ ] Cadastrar um serviço, um profissional (com o serviço) e a grade de horários
- [ ] Abrir `/seu-slug` em aba anônima e agendar
- [ ] Ver o agendamento em `/painel`
- [ ] Abrir o link do cliente, remarcar e cancelar

## 5. E-mail (opcional)
Com `MAIL_ENABLED=false` (padrão) o e-mail só aparece no log. Para enviar de verdade:
`MAIL_ENABLED=true`, `MAIL_FROM`, `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD`.
Teste com um agendamento que tenha e-mail antes de divulgar (esta parte nunca rodou em ambiente real).

## 6. Prints para o README
Salve em `docs/img/`: página pública (escolha de horário), confirmação, painel da agenda e grade de horários dos profissionais.
