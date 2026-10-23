# Checklist de deploy

O app inteiro (API + front) sobe em **um container só** no Render, com PostgreSQL. Não precisa de Vercel nem de variável no front: o front é servido pela própria API e chama `/api` no mesmo domínio (sem CORS).

## 0. Antes de tudo
- [ ] `cd backend && mvn test` passa (precisa de Docker, por causa do `BookingFlowTest`)
- [ ] `docker build -t agendaki .` na raiz funciona (confirma front + API juntos)
- [ ] Código no GitHub

## 1. Render
1. Render → **New → Blueprint** → escolha o repositório (usa o `render.yaml` da raiz).
2. Quando pedir `PUBLIC_URL`, coloque qualquer valor provisório (ex.: `https://agendaki.onrender.com`).
3. Espere o build terminar (o primeiro demora: Node + Maven) e anote a URL do app.
4. Se a URL real for diferente da provisória, ajuste `PUBLIC_URL` no Render e salve (redeploy automático).

> O plano free dorme após ~15 min sem uso: a primeira requisição demora. Avise isso no README ou use um pinger.

## 2. Teste de fumaça
- [ ] Abrir a URL do app → home carrega; recarregar em `/entrar` não dá 404
- [ ] `/cadastro` → criar conta do negócio
- [ ] Cadastrar um serviço, um profissional (com o serviço) e a grade de horários
- [ ] Abrir `/seu-slug` em aba anônima e agendar
- [ ] Ver o agendamento em `/painel`
- [ ] Abrir o link do cliente, remarcar e cancelar
- [ ] `/api/qualquer-coisa` deve dar 401/404, nunca HTML

## 3. E-mail (opcional)
Com `MAIL_ENABLED=false` (padrão) o e-mail só aparece no log. Para enviar de verdade:
`MAIL_ENABLED=true`, `MAIL_FROM`, `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD`.
Teste com um agendamento que tenha e-mail antes de divulgar (esta parte nunca rodou em ambiente real).

## 4. Prints para o README
Salve em `docs/img/`: página pública (escolha de horário), confirmação, painel da agenda e grade de horários dos profissionais.
