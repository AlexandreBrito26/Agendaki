/** Lembra nome/telefone/e-mail neste aparelho para o próximo agendamento (só no navegador). */
const KEY = 'agendaki-customer'
export interface Customer { name: string; phone: string; email: string }

export function loadCustomer(): Customer {
  try {
    const c = JSON.parse(localStorage.getItem(KEY) ?? '{}')
    return { name: String(c.name ?? ''), phone: String(c.phone ?? ''), email: String(c.email ?? '') }
  } catch { return { name: '', phone: '', email: '' } }
}

export function saveCustomer(c: Customer) {
  try { localStorage.setItem(KEY, JSON.stringify(c)) } catch { /* ignora */ }
}
