import { useEffect, useState, type ReactNode } from 'react'
import { AuthContext } from './authContext'
import { loadSession, saveSession, setUnauthorizedHandler, type Session } from './ownerApi'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(loadSession)
  const signIn = (s: Session) => { saveSession(s); setSession(s) }
  const signOut = () => { saveSession(null); setSession(null) }
  useEffect(() => { setUnauthorizedHandler(signOut) }, [])
  return <AuthContext.Provider value={{ session, signIn, signOut }}>{children}</AuthContext.Provider>
}
