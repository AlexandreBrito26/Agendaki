import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function Home() {
  const [slug, setSlug] = useState('')
  const navigate = useNavigate()
  return (
    <main className="shell shell--narrow">
      <h1 className="brand brand--big">Agendaki</h1>
      <p className="lead">Agende seu horário em poucos toques, sem criar conta.</p>
      <form
        className="card stack"
        onSubmit={e => { e.preventDefault(); if (slug.trim()) navigate(`/${slug.trim().toLowerCase()}`) }}
      >
        <label className="field">
          <span>Endereço do negócio</span>
          <input
            value={slug}
            onChange={e => setSlug(e.target.value)}
            placeholder="ex.: barbearia-do-ze"
            autoCapitalize="none"
          />
        </label>
        <button className="btn btn--primary" disabled={!slug.trim()}>Ver horários</button>
      </form>
      <p className="muted center">Tem um negócio? <Link to="/entrar">Entrar no painel</Link> · <Link to="/cadastro">Criar conta</Link></p>
    </main>
  )
}
