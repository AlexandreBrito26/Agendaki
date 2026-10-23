import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import type { ApiError } from '../api'
import { useAuth } from '../authContext'
import { owner } from '../ownerApi'

// Rotas fixas do app: um negócio com esses slugs ficaria inacessível.
const RESERVED = ['entrar', 'cadastro', 'painel']
const slugify = (v: string) =>
  v.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/[^a-z0-9]+/g, '-').replace(/^-+/, '')

export default function AuthPage({ mode }: { mode: 'login' | 'register' }) {
  const { session, signIn } = useAuth()
  const navigate = useNavigate()
  const isReg = mode === 'register'
  const [businessName, setBusinessName] = useState('')
  const [slug, setSlug] = useState('')
  const [slugTouched, setSlugTouched] = useState(false)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [fields, setFields] = useState<Record<string, string>>({})

  if (session) return <Navigate to="/painel" replace />

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null); setFields({})
    if (isReg && RESERVED.includes(slug)) { setFields({ slug: 'Este endereço é reservado, escolha outro.' }); return }
    setBusy(true)
    try {
      const s = isReg
        ? await owner.register({ businessName: businessName.trim(), slug, email: email.trim(), password })
        : await owner.login(email.trim(), password)
      signIn(s); navigate('/painel', { replace: true })
    } catch (err) {
      const a = err as ApiError
      setError(a.message); setFields(a.fields ?? {})
    } finally { setBusy(false) }
  }

  return (
    <main className="shell shell--narrow">
      <span className="brand">Agendaki</span>
      <h1 className="page-title">{isReg ? 'Crie a agenda do seu negócio' : 'Entrar no painel'}</h1>
      <form className="card stack" onSubmit={submit} noValidate>
        {isReg && (
          <>
            <label className="field"><span>Nome do negócio</span>
              <input value={businessName} onChange={e => { setBusinessName(e.target.value); if (!slugTouched) setSlug(slugify(e.target.value)) }} required />
              {fields.businessName && <em className="field__err">{fields.businessName}</em>}
            </label>
            <label className="field"><span>Endereço da página</span>
              <input value={slug} onChange={e => { setSlugTouched(true); setSlug(slugify(e.target.value)) }} required />
              <small className="muted">{window.location.host}/{slug || 'seu-negocio'}</small>
              {fields.slug && <em className="field__err">{fields.slug}</em>}
            </label>
          </>
        )}
        <label className="field"><span>E-mail</span>
          <input type="email" value={email} onChange={e => setEmail(e.target.value)} autoComplete="email" required />
          {fields.email && <em className="field__err">{fields.email}</em>}
        </label>
        <label className="field"><span>Senha</span>
          <input type="password" value={password} onChange={e => setPassword(e.target.value)} autoComplete={isReg ? 'new-password' : 'current-password'} required />
          {isReg && <small className="muted">Mínimo de 8 caracteres.</small>}
          {fields.password && <em className="field__err">{fields.password}</em>}
        </label>
        {error && <p className="alert alert--error">{error}</p>}
        <button className="btn btn--primary" disabled={busy || !email || !password}>{busy ? 'Aguarde…' : isReg ? 'Criar conta' : 'Entrar'}</button>
        <p className="muted center">
          {isReg ? <>Já tem conta? <Link to="/entrar">Entrar</Link></> : <>Primeira vez? <Link to="/cadastro">Criar conta</Link></>}
        </p>
      </form>
    </main>
  )
}
