export async function postForm(baseUrl, path, data = {}, token) {
  const body = new URLSearchParams()
  Object.entries(data).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      body.append(key, value)
    }
  })

  const response = await fetch(`${baseUrl}${path}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
      ...(token ? { token } : {}),
    },
    body,
  })

  return response.json()
}

export const api = {
  register(baseUrl, payload) {
    return postForm(baseUrl, '/account/register', payload)
  },
  login(baseUrl, payload) {
    return postForm(baseUrl, '/account/login', payload)
  },
  quickMeeting(baseUrl, payload, token) {
    return postForm(baseUrl, '/meeting/quickMeeting', payload, token)
  },
  preJoinMeeting(baseUrl, payload, token) {
    return postForm(baseUrl, '/meeting/preJoinMeeting', payload, token)
  },
  joinMeeting(baseUrl, payload, token) {
    return postForm(baseUrl, '/meeting/joinMeeting', payload, token)
  },
  exitMeeting(baseUrl, token) {
    return postForm(baseUrl, '/meeting/exitMeeting', {}, token)
  },
  finishMeeting(baseUrl, token) {
    return postForm(baseUrl, '/meeting/finishMeeting', {}, token)
  },
  getCurrentMeeting(baseUrl, token) {
    return postForm(baseUrl, '/meeting/getCurrentMeeting', {}, token)
  },
}
