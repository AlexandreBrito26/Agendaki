import { useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api, ApiError, type Appointment, type Catalog } from '../api'
import DateTimePicker from '../components/DateTimePicker'
import { canAddToCalendar, downloadIcs, googleCalendarUrl } from '../lib/calendar'
import { loadCustomer, saveCustomer } from '../lib/customer'
import { formatDuration, formatLongDate, formatPrice, maskPhone } from '../lib/format'

const STEPS = ['Serviço', 'Profissional', 'Horário', 'Seus dados'] as const

export default function BookingPage() {
  const { slug = '' } = useParams()
  const [catalog, setCatalog] = useState<Catalog | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)

  const [step, setStep] = useState(0)
  const [serviceId, setServiceId] = useState<number | null>(null)
  const [professionalId, setProfessionalId] = useState<number | null>(null)
  const [date, setDate] = useState<string | null>(null)
  const [time, setTime] = useState<string | null>(null)
  const [saved] = useState(loadCustomer) // dados de um agendamento anterior neste aparelho
  const [name, setName] = useState(saved.name)
  const [phone, setPhone] = useState(saved.phone)
  const [email, setEmail] = useState(saved.email)

  const [submitting, setSubmitting] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [done, setDone] = useState<Appointment | null>(null)
  const [copied, setCopied] = useState(false)

  useEffect(() => {
    api.catalog(slug)
      .then(c => { setCatalog(c); document.title = `${c.businessName} · Agendaki` })
      .catch(e => setLoadError(e.message))
  }, [slug])

  const service = catalog?.services.find(s => s.id === serviceId) ?? null
  const professionals = useMemo(
    () => catalog?.professionals.filter(p => serviceId !== null && p.serviceIds.includes(serviceId)) ?? [],
    [catalog, serviceId],
  )
  const professional = professionals.find(p => p.id === professionalId) ?? null

  if (loadError) {
    return (
      <main className="shell shell--narrow">
        <h1 className="brand">Agendaki</h1>
        <div className="card stack">
          <h2>Negócio não encontrado</h2>
          <p className="muted">{loadError}</p>
          <Link className="btn" to="/">Voltar</Link>
        </div>
      </main>
    )
  }
  if (!catalog) {
    return (
      <main className="shell" aria-busy="true" aria-label="Carregando">
        <div className="hero"><span className="skel skel--line" style={{ width: 90 }} /><span className="skel skel--title" /></div>
        <div className="card stack">
          {[0, 1, 2].map(i => <span key={i} className="skel skel--option" />)}
        </div>
      </main>
    )
  }

  const manageUrl = (token: string) => `${window.location.origin}/${slug}/agendamento/${token}`

  const choose = (setter: () => void, next: number) => { setter(); setStep(next) }

  const selectService = (id: number) => {
    if (id !== serviceId) { setProfessionalId(null); setDate(null); setTime(null) }
    choose(() => setServiceId(id), 1)
  }
  const selectProfessional = (id: number) => {
    if (id !== professionalId) { setDate(null); setTime(null) }
    choose(() => setProfessionalId(id), 2)
  }

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!serviceId || !professionalId || !date || !time) return
    setSubmitting(true); setFormError(null); setFieldErrors({})
    try {
      const appt = await api.book(slug, {
        serviceId, professionalId, date, time,
        customerName: name.trim(), customerPhone: phone,
        customerEmail: email.trim() || undefined,
      })
      saveCustomer({ name: name.trim(), phone, email: email.trim() })
      setDone(appt)
    } catch (err) {
      const apiErr = err as ApiError
      setFormError(apiErr.message)
      setFieldErrors(apiErr.fields ?? {})
      if (apiErr.status === 409) { setTime(null); setStep(2) } // horário tomado: volta para escolher outro
    } finally {
      setSubmitting(false)
    }
  }

  if (done) {
    const link = done.manageToken ? manageUrl(done.manageToken) : null
    return (
      <main className="shell shell--narrow">
        <Header name={catalog.businessName} />
        <section className="card stack success">
          <div className="success__icon" aria-hidden>✓</div>
          <h2>Agendamento confirmado!</h2>
          <dl className="summary">
            <div><dt>Serviço</dt><dd>{done.serviceName}</dd></div>
            <div><dt>Profissional</dt><dd>{done.professionalName}</dd></div>
            <div><dt>Quando</dt><dd className="cap">{formatLongDate(done.date)}, {done.startTime}</dd></div>
            <div><dt>Em nome de</dt><dd>{done.customerName}</dd></div>
          </dl>
          {link && (
            <div className="alert">
              <strong>Guarde este link.</strong> Por ele você cancela ou remarca, sem precisar de conta.
              <div className="linkbox">
                <code>{link}</code>
                <button
                  className="btn btn--small"
                  onClick={() => navigator.clipboard?.writeText(link).then(() => setCopied(true))}
                >
                  {copied ? 'Copiado ✓' : 'Copiar'}
                </button>
              </div>
            </div>
          )}
          {canAddToCalendar(done) && (
            <div className="actions">
              <a className="btn" href={googleCalendarUrl(done, catalog.businessName)} target="_blank" rel="noreferrer">📅 Google Agenda</a>
              <button type="button" className="btn" onClick={() => downloadIcs(done, catalog.businessName)}>⬇ Baixar .ics</button>
            </div>
          )}
          {done.manageToken && (
            <Link className="btn btn--primary" to={`/${slug}/agendamento/${done.manageToken}`}>
              Ver / alterar agendamento
            </Link>
          )}
        </section>
      </main>
    )
  }

  const back = step > 0 ? () => setStep(step - 1) : null

  return (
    <main className="shell">
      <Header name={catalog.businessName} />

      <ol className="steps" aria-label="Etapas">
        {STEPS.map((label, i) => (
          <li key={label} className={i === step ? 'on' : i < step ? 'past' : ''}>
            <button type="button" disabled={i >= step} onClick={() => setStep(i)}>
              <span className="steps__n">{i < step ? '✓' : i + 1}</span>
              <span className="steps__l">{label}</span>
            </button>
          </li>
        ))}
      </ol>

      <div className="layout">
        <section className="card stack pane" key={step}>
          {back && <button type="button" className="linkbtn" onClick={back}>← Voltar</button>}

          {step === 0 && (
            <>
              <h2>Escolha o serviço</h2>
              {catalog.services.length === 0 && <p className="muted">Este negócio ainda não cadastrou serviços.</p>}
              <ul className="options">
                {catalog.services.map(s => (
                  <li key={s.id}>
                    <button className={`option${s.id === serviceId ? ' option--on' : ''}`} onClick={() => selectService(s.id)}>
                      <span className="option__main">
                        <strong>{s.name}</strong>
                        <small>{formatDuration(s.durationMin)}</small>
                      </span>
                      <span className="option__price">{formatPrice(s.priceCents)}</span>
                    </button>
                  </li>
                ))}
              </ul>
            </>
          )}

          {step === 1 && (
            <>
              <h2>Com quem?</h2>
              {professionals.length === 0 && <p className="muted">Nenhum profissional faz este serviço no momento.</p>}
              <ul className="options">
                {professionals.map(p => (
                  <li key={p.id}>
                    <button className={`option${p.id === professionalId ? ' option--on' : ''}`} onClick={() => selectProfessional(p.id)}>
                      <span className="avatar" aria-hidden>{p.name.charAt(0).toUpperCase()}</span>
                      <span className="option__main"><strong>{p.name}</strong></span>
                    </button>
                  </li>
                ))}
              </ul>
            </>
          )}

          {step === 2 && serviceId && professionalId && (
            <>
              <h2>Dia e horário</h2>
              {formError && <p className="alert alert--error">{formError}</p>}
              <DateTimePicker
                slug={slug}
                timezone={catalog.timezone}
                serviceId={serviceId}
                professionalId={professionalId}
                date={date}
                time={time}
                onChange={(d, t) => { setDate(d); setTime(t); setFormError(null) }}
              />
              <button className="btn btn--primary" disabled={!date || !time} onClick={() => setStep(3)}>
                Continuar{date && time ? ` · ${formatLongDate(date).split(',')[0]} às ${time}` : ''}
              </button>
            </>
          )}

          {step === 3 && (
            <form className="stack" onSubmit={submit} noValidate>
              <h2>Seus dados</h2>
              {saved.name && <p className="hint">Preenchemos com os dados do seu último agendamento. Confira antes de confirmar.</p>}
              <label className="field">
                <span>Nome</span>
                <input value={name} onChange={e => setName(e.target.value)} autoComplete="name" maxLength={100} required />
                {fieldErrors.customerName && <em className="field__err">{fieldErrors.customerName}</em>}
              </label>
              <label className="field">
                <span>Telefone (WhatsApp)</span>
                <input
                  value={phone}
                  onChange={e => setPhone(maskPhone(e.target.value))}
                  inputMode="tel" autoComplete="tel" placeholder="(11) 91234-5678" required
                />
                {fieldErrors.customerPhone && <em className="field__err">{fieldErrors.customerPhone}</em>}
              </label>
              <label className="field">
                <span>E-mail <small className="muted">(opcional)</small></span>
                <input type="email" value={email} onChange={e => setEmail(e.target.value)} autoComplete="email" placeholder="para receber a confirmação e o lembrete" />
                {fieldErrors.customerEmail && <em className="field__err">{fieldErrors.customerEmail}</em>}
              </label>
              {formError && <p className="alert alert--error">{formError}</p>}
              <button
                className="btn btn--primary"
                disabled={submitting || name.trim().length < 2 || phone.replace(/\D/g, '').length < 10}
              >
                {submitting ? 'Confirmando…' : 'Confirmar agendamento'}
              </button>
            </form>
          )}
        </section>

        <aside className="card summary-card" aria-label="Resumo">
          <h3>Resumo</h3>
          <dl className="summary">
            <div><dt>Serviço</dt><dd>{service ? `${service.name} · ${formatPrice(service.priceCents)}` : '—'}</dd></div>
            <div><dt>Duração</dt><dd>{service ? formatDuration(service.durationMin) : '—'}</dd></div>
            <div><dt>Profissional</dt><dd>{professional?.name ?? '—'}</dd></div>
            <div><dt>Quando</dt><dd className="cap">{date && time ? `${formatLongDate(date)}, ${time}` : '—'}</dd></div>
          </dl>
        </aside>
      </div>
    </main>
  )
}

function Header({ name }: { name: string }) {
  return (
    <header className="hero">
      <span className="brand">Agendaki</span>
      <h1>{name}</h1>
      <p className="muted">Agende seu horário online</p>
    </header>
  )
}
