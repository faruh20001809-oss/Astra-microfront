/** Ключи сессии клиента витрины (JWT + профиль). */
export const CLIENT_TOKEN_KEY = 'astra_client_access_token'
export const CLIENT_PROFILE_KEY = 'astra_client_profile'
export const GUEST_EMAIL_KEY = 'astra_guest_email'

function storage() {
  return typeof localStorage !== 'undefined' ? localStorage : null
}

export function getClientToken() {
  const ls = storage()
  return ls ? ls.getItem(CLIENT_TOKEN_KEY) || '' : ''
}

export function loadClientProfile() {
  const ls = storage()
  if (!ls) return {}
  try {
    return JSON.parse(ls.getItem(CLIENT_PROFILE_KEY) || '{}') || {}
  } catch {
    return {}
  }
}

export function isClientLoggedIn() {
  return Boolean(getClientToken())
}

/** Сохранить профиль и JWT после login/register. */
export function saveClientSession(profile, accessToken) {
  const p = profile && typeof profile === 'object' ? profile : {}
  const ls = storage()
  if (ls) {
    ls.setItem(CLIENT_PROFILE_KEY, JSON.stringify(p))
    if (accessToken) {
      ls.setItem(CLIENT_TOKEN_KEY, String(accessToken))
    }
    if (p.email) {
      ls.setItem(GUEST_EMAIL_KEY, String(p.email).trim())
    }
  }
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent('astra:client-profile-updated', { detail: { profile: p } }))
    if (p.email) {
      window.dispatchEvent(new CustomEvent('astra:guest-email-updated', { detail: { email: p.email } }))
    }
  }
}

export function clearClientSession() {
  const ls = storage()
  if (ls) {
    ls.removeItem(CLIENT_TOKEN_KEY)
    ls.removeItem(CLIENT_PROFILE_KEY)
  }
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent('astra:client-profile-updated', { detail: { profile: {} } }))
  }
}

export function getAuthHeaders() {
  const token = getClientToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
}
