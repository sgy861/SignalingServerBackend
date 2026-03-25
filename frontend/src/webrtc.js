const DEFAULT_RTC_CONFIG = {
  iceServers: [{ urls: 'stun:stun.l.google.com:19302' }],
}

export function createRtcManager({ getState, ensureLocalStream, sendSignal, onRemoteStream, onPeerClosed, log }) {
  const peerMap = new Map()

  async function getOrCreatePeer(remoteUserId) {
    if (peerMap.has(remoteUserId)) {
      return peerMap.get(remoteUserId)
    }

    const localStream = await ensureLocalStream()
    const pc = new RTCPeerConnection(DEFAULT_RTC_CONFIG)
    const remoteStream = new MediaStream()

    localStream.getTracks().forEach((track) => {
      pc.addTrack(track, localStream)
    })

    pc.ontrack = (event) => {
      event.streams[0]?.getTracks().forEach((track) => remoteStream.addTrack(track))
      onRemoteStream(remoteUserId, remoteStream)
    }

    pc.onicecandidate = (event) => {
      if (!event.candidate) {
        return
      }
      const state = getState()
      sendSignal({
        token: state.token,
        sendUserId: state.user?.userId || '',
        receiveUserId: remoteUserId,
        signalType: 'candidate',
        signalData: JSON.stringify(event.candidate),
      })
    }

    pc.onconnectionstatechange = () => {
      log(`Peer ${remoteUserId} connectionState=${pc.connectionState}`)
      if (['failed', 'disconnected', 'closed'].includes(pc.connectionState)) {
        closePeer(remoteUserId)
      }
    }

    peerMap.set(remoteUserId, pc)
    return pc
  }

  async function createOffer(remoteUserId) {
    const pc = await getOrCreatePeer(remoteUserId)
    const state = getState()
    const offer = await pc.createOffer()
    await pc.setLocalDescription(offer)
    sendSignal({
      token: state.token,
      sendUserId: state.user?.userId || '',
      receiveUserId: remoteUserId,
      signalType: 'offer',
      signalData: JSON.stringify(pc.localDescription),
    })
    log(`向 ${remoteUserId} 发送 offer`)
  }

  async function handleOffer(remoteUserId, signalData) {
    const pc = await getOrCreatePeer(remoteUserId)
    const state = getState()
    const offer = JSON.parse(signalData)
    await pc.setRemoteDescription(new RTCSessionDescription(offer))
    const answer = await pc.createAnswer()
    await pc.setLocalDescription(answer)
    sendSignal({
      token: state.token,
      sendUserId: state.user?.userId || '',
      receiveUserId: remoteUserId,
      signalType: 'answer',
      signalData: JSON.stringify(pc.localDescription),
    })
    log(`向 ${remoteUserId} 返回 answer`)
  }

  async function handleAnswer(remoteUserId, signalData) {
    const pc = await getOrCreatePeer(remoteUserId)
    const answer = JSON.parse(signalData)
    await pc.setRemoteDescription(new RTCSessionDescription(answer))
    log(`收到 ${remoteUserId} 的 answer`)
  }

  async function handleCandidate(remoteUserId, signalData) {
    const pc = await getOrCreatePeer(remoteUserId)
    const candidate = JSON.parse(signalData)
    await pc.addIceCandidate(new RTCIceCandidate(candidate))
  }

  function closePeer(remoteUserId) {
    const pc = peerMap.get(remoteUserId)
    if (!pc) {
      return
    }
    pc.ontrack = null
    pc.onicecandidate = null
    pc.onconnectionstatechange = null
    pc.close()
    peerMap.delete(remoteUserId)
    onPeerClosed(remoteUserId)
  }

  function closeAllPeers() {
    Array.from(peerMap.keys()).forEach(closePeer)
  }

  return {
    createOffer,
    handleOffer,
    handleAnswer,
    handleCandidate,
    closePeer,
    closeAllPeers,
  }
}
