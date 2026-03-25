const HEARTBEAT_INTERVAL = 12000

export function createSignalingClient({ getState, onMessage, onStatus, log }) {
  let socket = null
  let heartbeatTimer = null

  function updateStatus(status) {
    onStatus?.(status)
  }

  function stopHeartbeat() {
    if (heartbeatTimer) {
      clearInterval(heartbeatTimer)
      heartbeatTimer = null
    }
  }

  function startHeartbeat() {
    stopHeartbeat()
    heartbeatTimer = setInterval(() => {
      const state = getState()
      if (!socket || socket.readyState !== WebSocket.OPEN || !state.token) {
        return
      }
      sendPeerSignal({
        token: state.token,
        sendUserId: state.user?.userId || '',
        receiveUserId: '',
        signalType: 'heartbeat',
        signalData: '',
      })
    }, HEARTBEAT_INTERVAL)
  }

  function connect(wsBase) {
    const state = getState()
    if (!state.token) {
      throw new Error('未登录，无法连接 WebSocket')
    }
    if (socket && [WebSocket.OPEN, WebSocket.CONNECTING].includes(socket.readyState)) {
      return socket
    }

    socket = new WebSocket(`${wsBase}/ws?token=${encodeURIComponent(state.token)}`)
    updateStatus('连接中')

    socket.addEventListener('open', () => {
      log('WS 已连接')
      updateStatus('已连接')
      startHeartbeat()
    })

    socket.addEventListener('message', (event) => {
      try {
        const payload = JSON.parse(event.data)
        onMessage?.(payload)
      } catch (error) {
        log(`WS 消息解析失败: ${error.message}`)
      }
    })

    socket.addEventListener('close', () => {
      stopHeartbeat()
      updateStatus('已断开')
      log('WS 已断开')
    })

    socket.addEventListener('error', () => {
      updateStatus('错误')
      log('WS 连接发生错误')
    })

    return socket
  }

  function sendPeerSignal(message) {
    if (!socket || socket.readyState !== WebSocket.OPEN) {
      log('WS 未连接，忽略发送信令')
      return
    }
    socket.send(JSON.stringify(message))
  }

  function close() {
    stopHeartbeat()
    if (socket) {
      socket.close()
      socket = null
    }
    updateStatus('未连接')
  }

  return {
    connect,
    close,
    sendPeerSignal,
  }
}
