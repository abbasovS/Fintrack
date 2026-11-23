const TOKEN_KEY = 'fintrack.access-token';

function decodePayload(token: string): { exp?: number } {
  const segment = token.split('.')[1];
  if (!segment) throw new Error('Token formatı yanlışdır');
  const base64 = segment.replace(/-/g, '+').replace(/_/g, '/');
  return JSON.parse(atob(base64.padEnd(Math.ceil(base64.length / 4) * 4, '='))) as { exp?: number; uid?: number };
}

export function getToken(): string | null {
  const token = sessionStorage.getItem(TOKEN_KEY);
  if (!token) return null;
  try {
    const payload = decodePayload(token);
    if (payload.exp && payload.exp * 1000 <= Date.now()) {
      sessionStorage.removeItem(TOKEN_KEY);
      return null;
    }
    return token;
  } catch {
    sessionStorage.removeItem(TOKEN_KEY);
    return null;
  }
}

export function saveToken(token: string): void {
  sessionStorage.setItem(TOKEN_KEY, token);
}

export function clearToken(): void {
  sessionStorage.removeItem(TOKEN_KEY);
}

