# Confirmação por WhatsApp (Cloud API da Meta)

Quando o cliente conclui um agendamento, o backend envia **uma mensagem de confirmação** no WhatsApp do número informado no formulário. O envio acontece depois do commit, em segundo plano: se o WhatsApp falhar, o agendamento continua salvo e o erro vai só para o log.

```
BookingRequest → AppointmentService.book() → commit
                                              └─ AppointmentWhatsAppListener (@Async) → WhatsAppClient → graph.facebook.com
```

Código: `backend/src/main/java/com/agendaki/whatsapp/`.

Com `WHATSAPP_ENABLED=false` (padrão) nada é enviado: a mensagem só aparece no log, igual ao e-mail.

## 1. Configurar na Meta (uma vez)

1. Crie um app em <https://developers.facebook.com> (tipo **Business**) e adicione o produto **WhatsApp**.
2. Em **WhatsApp → API Setup** você recebe um **número de teste**, o **Phone number ID** e um **token temporário** (vale 24h).
3. No mesmo painel, cadastre seu próprio número em "To" (até 5 destinatários de teste; cada um confirma com um código recebido no WhatsApp).
4. Crie o template abaixo em **WhatsApp Manager → Message templates**.

### Template

| Campo | Valor |
| --- | --- |
| Nome | `agendamento_confirmado` |
| Categoria | **Utility** (utilidade) |
| Idioma | Portuguese (BR) — `pt_BR` |

Corpo (as variáveis têm que estar nesta ordem):

```
Olá, {{1}}! Seu agendamento de {{2}} com {{3}} está confirmado para {{4}}, na {{5}}.

Para remarcar ou cancelar, acesse: {{6}}

Até breve!
```

| Variável | Conteúdo | Exemplo |
| --- | --- | --- |
| `{{1}}` | nome do cliente | Maria Silva |
| `{{2}}` | serviço | Corte |
| `{{3}}` | profissional | João |
| `{{4}}` | data e hora (fuso do negócio) | segunda-feira, 26/10 às 14:30 |
| `{{5}}` | nome do negócio | Barbearia Central |
| `{{6}}` | link de remarcar/cancelar | https://…/central/agendamento/TOKEN |

Observações:
- A Meta recusa template que começa ou termina com variável, por isso o texto fixo no início e no fim.
- Se a Meta recusar o link como variável do corpo, o caminho alternativo é um botão "Visitar site" com URL dinâmica (exige mudar o payload em `WhatsAppClient`).
- Se você mudar o texto, o número ou a ordem das variáveis, ajuste também `AppointmentWhatsAppListener.bodyParams`.

## 2. Variáveis de ambiente

| Variável | Obrigatória | Descrição |
| --- | --- | --- |
| `WHATSAPP_ENABLED` | — | `true` para enviar de verdade (padrão `false`) |
| `WHATSAPP_PHONE_NUMBER_ID` | sim | ID do número remetente (painel API Setup) |
| `WHATSAPP_ACCESS_TOKEN` | sim | Token da Cloud API. **Segredo**: só em variável de ambiente |
| `WHATSAPP_TEMPLATE_NAME` | — | Padrão `agendamento_confirmado` |
| `WHATSAPP_TEMPLATE_LANGUAGE` | — | Padrão `pt_BR` |
| `WHATSAPP_API_VERSION` | — | Versão da Graph API, padrão `v23.0`. Confira a versão vigente na documentação da Meta |
| `WHATSAPP_DEFAULT_COUNTRY_CODE` | — | Padrão `55` |

No Render, preencha `WHATSAPP_PHONE_NUMBER_ID` e `WHATSAPP_ACCESS_TOKEN` no painel (o `render.yaml` já declara as chaves com `sync: false`) e mude `WHATSAPP_ENABLED` para `true`.

## 3. Testar localmente

```bash
export WHATSAPP_ENABLED=true
export WHATSAPP_PHONE_NUMBER_ID=...      # do painel API Setup
export WHATSAPP_ACCESS_TOKEN=...         # token temporário
cd backend && mvn spring-boot:run
```

Faça um agendamento pela página pública usando, no telefone, um dos números cadastrados como destinatário de teste. No log aparece:

- `[whatsapp] confirmação enviada para=****1234 id=wamid...` — a Meta aceitou.
- `Falha ao enviar WhatsApp ...: HTTP 400 — {...}` — o corpo explica o motivo. Os mais comuns: template inexistente ou ainda não aprovado, número fora da lista de teste, token expirado.

## 4. Ir para produção

- **Token permanente:** o token do painel expira em 24h. Crie um **Usuário do sistema** no Business Manager, dê acesso ao app e gere um token com a permissão `whatsapp_business_messaging`.
- **Número próprio:** cadastre o número da empresa (não pode estar ativo no app WhatsApp comum) e passe pela verificação do negócio.
- **Consentimento:** a política da Meta e a LGPD exigem que o cliente saiba que vai receber mensagem. Antes de ligar em produção, avise isso no formulário público (ex.: "Você receberá a confirmação por WhatsApp neste número").
- **Custo:** a Meta cobra por mensagem de template. Confira a tabela de preços vigente para o Brasil.

## Formato do telefone

O formulário aceita máscara (`(11) 99999-8888`). `PhoneNumbers.toE164` converte para só dígitos com código do país: 10 ou 11 dígitos recebem o `55`; números com `+` ou `00` são usados como vieram. Telefone que não dá para interpretar não é enviado (fica só um aviso no log).
