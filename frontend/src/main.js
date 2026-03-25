import './styles.css'
import { api } from './api'
import { createSignalingClient } from './signaling'
import { createRtcManager } from './webrtc'

const state = {
  apiBase: '/api',
  wsBase: 'ws://localhost:6061',
  token: '',
  user: null,
  localStream: null,
  currentMeetingId: '',
  currentMeetingNo: '',
  participants: [],
}

const elements = {
  apiBase: document.querySelector('#apiBase'),
  wsBase: document.querySelector('#wsBase'),
  sessionUser: document.querySelector('#sessionUser'),
  sessionToken: document.querySelector('#sessionToken'),
  meetingId: document.querySelector('#meetingId'),
  meetingNo: document.querySelector('#meetingNo'),
  wsStatus: document.querySelector('#wsStatus'),
  participantList: document.querySelector('#participantList'),
  videoGrid: document.querySelector('#videoGrid'),
  localVideo: document.querySelector('#localVideo'),
  logPanel: document.querySelector('#logPanel'),
  registerForm: document.querySelector('#registerForm'),
  loginForm: document.querySelector('#loginForm'),
  createMeetingForm: document.querySelector('#createMeetingForm'),
  joinMeetingForm: document.querySelector('#joinMeetingForm'),
  leaveBtn: document.querySelector('#leaveBtn'),
  finishBtn: document.querySelector('#finishBtn'),
  mediaBtn: document.querySelector('#mediaBtn'),
  clearLogBtn: document.querySelector('#clearLogBtn'),
}

function log(message, type = 'info') {
  const now = new Date().toLocaleTimeString()
  elements.logPanel.textContent = `[${now}] ${message}\n${elements.logPanel.textContent}`
  if (type === 'error') {
    console.error(message)
  } else {
    console.log(message)
  }
}

function getState() {
  return state
}

function updateSessionView() {
  elements.sessionUser.textContent = state.user ? `${state.user.nickName} (${state.user.userId})` : '未登录'
  elements.sessionToken.textContent = state.token || '-'
  elements.meetingId.textContent = state.currentMeetingId || '-'
  elements.meetingNo.textContent = state.currentMeetingNo || '-'
}

function updateWsStatus(text) {
  elements.wsStatus.textContent = text
}

function renderParticipants() {
  elements.participantList.innerHTML = ''
  state.participants.forEach((member) => {
    const li = document.createElement('li')
    const flags = []
    if (member.userId === state.user?.userId) flags.push('我')
    if (member.videoOpen) flags.push('视频开')
    li.textContent = `${member.nickName || member.userId} (${member.userId}) ${flags.length ? `- ${flags.join(' / ')}` : ''}`
    elements.participantList.appendChild(li)
  })
}

function ensureRemoteVideo(remoteUserId, label) {
  let tile = document.querySelector(`[data-user-id="${remoteUserId}"]`)
  if (!tile) {
    tile = document.createElement('div')
    tile.className = 'video-tile'
    tile.dataset.userId = remoteUserId

    const title = document.createElement('div')
    title.className = 'video-title'
    title.textContent = label || remoteUserId

    const video = document.createElement('video')
    video.autoplay = true
    video.playsInline = true

    tile.appendChild(title)
    tile.appendChild(video)
    elements.videoGrid.appendChild(tile)
  }
  return tile.querySelector('video')
}

function removeRemoteVideo(remoteUserId) {
  const tile = document.querySelector(`[data-user-id="${remoteUserId}"]`)
  tile?.remove()
}

async function ensureLocalStream() {
  if (state.localStream) {
    return state.localStream
  }
  state.localStream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true })
  elements.localVideo.srcObject = state.localStream
  log('本地媒体流已就绪')
  return state.localStream
}

function stopLocalStream() {
  if (!state.localStream) {
    return
  }
  state.localStream.getTracks().forEach((track) => track.stop())
  state.localStream = null
  elements.localVideo.srcObject = null
}

const signalingClient = createSignalingClient({
  getState,
  onStatus: updateWsStatus,
  log,
  onMessage: async (payload) => {
    try {
      await handleWsMessage(payload)
    } catch (error) {
      log(`处理 WS 消息失败: ${error.message}`, 'error')
    }
  },
})

const rtcManager = createRtcManager({
  getState,
  ensureLocalStream,
  sendSignal: (message) => signalingClient.sendPeerSignal(message),
  onRemoteStream: (remoteUserId, stream) => {
    const member = state.participants.find((item) => item.userId === remoteUserId)
    const video = ensureRemoteVideo(remoteUserId, member?.nickName || remoteUserId)
    video.srcObject = stream
  },
  onPeerClosed: (remoteUserId) => {
    removeRemoteVideo(remoteUserId)
  },
  log,
})

function normalizeResponse(response) {
  if (!response || response.status !== 'success') {
    throw new Error(response?.info || '请求失败')
  }
  return response.data
}

async function connectWsIfNeeded() {
  signalingClient.connect(state.wsBase)
}

async function handleRoomJoined(meetingJoinDto) {
  const list = Array.isArray(meetingJoinDto?.meetingMemberDtoList) ? meetingJoinDto.meetingMemberDtoList : []
  state.participants = list.filter((item) => item.status === 1)
  renderParticipants()

  const newMember = meetingJoinDto?.newMember
  if (!newMember || !state.user) {
    return
  }

  if (newMember.userId === state.user.userId) {
    const peers = state.participants.filter((item) => item.userId !== state.user.userId)
    for (const peer of peers) {
      await rtcManager.createOffer(peer.userId)
    }
  }
}

async function handlePeerMessage(payload) {
  const messageContent = payload.messageContent || {}
  const remoteUserId = payload.sendUserId
  if (!remoteUserId || remoteUserId === state.user?.userId) {
    return
  }

  if (messageContent.signalType === 'heartbeat') {
    return
  }

  if (messageContent.signalType === 'offer') {
    await rtcManager.handleOffer(remoteUserId, messageContent.signalData)
    return
  }

  if (messageContent.signalType === 'answer') {
    await rtcManager.handleAnswer(remoteUserId, messageContent.signalData)
    return
  }

  if (messageContent.signalType === 'candidate') {
    await rtcManager.handleCandidate(remoteUserId, messageContent.signalData)
  }
}

async function handleExitMessage(payload) {
  const exitInfo = typeof payload.messageContent === 'string'
    ? JSON.parse(payload.messageContent)
    : payload.messageContent

  const exitUserId = exitInfo?.exitUserId
  if (exitUserId) {
    rtcManager.closePeer(exitUserId)
  }
  state.participants = Array.isArray(exitInfo?.meetingMemberDtoList)
    ? exitInfo.meetingMemberDtoList.filter((item) => item.status === 1)
    : []
  renderParticipants()
}

async function handleWsMessage(payload) {
  const type = payload.messageType
  if (type === 1) {
    log('收到入会广播')
    await handleRoomJoined(payload.messageContent)
    return
  }
  if (type === 2) {
    await handlePeerMessage(payload)
    return
  }
  if (type === 3) {
    log('收到退会广播')
    await handleExitMessage(payload)
    return
  }
  if (type === 4) {
    log('会议已结束')
    cleanupMeetingState(false)
    return
  }
  log(`收到未处理的消息类型: ${type}`)
}

function cleanupMeetingState(closeSocket = true) {
  rtcManager.closeAllPeers()
  state.participants = []
  renderParticipants()
  state.currentMeetingId = ''
  state.currentMeetingNo = ''
  updateSessionView()
  if (closeSocket) {
    signalingClient.close()
  }
  Array.from(document.querySelectorAll('.video-tile[data-user-id]')).forEach((node) => node.remove())
}

async function createMeeting(formData) {
  await ensureLocalStream()
  const meetingId = normalizeResponse(await api.quickMeeting(state.apiBase, {
    meetingNoType: formData.get('meetingNoType'),
    meetingName: formData.get('meetingName'),
    joinType: formData.get('joinType'),
    joinPassword: formData.get('joinPassword'),
  }, state.token))

  state.currentMeetingId = meetingId
  state.currentMeetingNo = '创建后请从后端列表查看会议号'
  updateSessionView()
  await connectWsIfNeeded()
  normalizeResponse(await api.joinMeeting(state.apiBase, { videoOpen: true }, state.token))
  log(`创建并加入会议成功: ${meetingId}`)
}

async function joinMeeting(formData) {
  await ensureLocalStream()
  const nickName = formData.get('nickName') || state.user?.nickName || ''
  const meetingId = normalizeResponse(await api.preJoinMeeting(state.apiBase, {
    meetingNo: formData.get('meetingNo'),
    nickName,
    password: formData.get('password'),
  }, state.token))

  state.currentMeetingId = meetingId
  state.currentMeetingNo = formData.get('meetingNo')
  updateSessionView()
  await connectWsIfNeeded()
  normalizeResponse(await api.joinMeeting(state.apiBase, { videoOpen: true }, state.token))
  log(`加入会议成功: ${meetingId}`)
}

elements.apiBase.addEventListener('change', (event) => {
  state.apiBase = event.target.value.trim() || '/api'
})

elements.wsBase.addEventListener('change', (event) => {
  state.wsBase = event.target.value.trim() || 'ws://localhost:6061'
})

elements.registerForm.addEventListener('submit', async (event) => {
  event.preventDefault()
  try {
    const formData = new FormData(event.currentTarget)
    normalizeResponse(await api.register(state.apiBase, Object.fromEntries(formData.entries())))
    log('注册成功')
    event.currentTarget.reset()
  } catch (error) {
    log(`注册失败: ${error.message}`, 'error')
  }
})

elements.loginForm.addEventListener('submit', async (event) => {
  event.preventDefault()
  try {
    const formData = new FormData(event.currentTarget)
    const data = normalizeResponse(await api.login(state.apiBase, Object.fromEntries(formData.entries())))
    state.token = data.token
    state.user = data
    updateSessionView()
    log(`登录成功: ${data.nickName}`)
  } catch (error) {
    log(`登录失败: ${error.message}`, 'error')
  }
})

elements.createMeetingForm.addEventListener('submit', async (event) => {
  event.preventDefault()
  try {
    if (!state.token) {
      throw new Error('请先登录')
    }
    await createMeeting(new FormData(event.currentTarget))
  } catch (error) {
    log(`创建会议失败: ${error.message}`, 'error')
  }
})

elements.joinMeetingForm.addEventListener('submit', async (event) => {
  event.preventDefault()
  try {
    if (!state.token) {
      throw new Error('请先登录')
    }
    await joinMeeting(new FormData(event.currentTarget))
  } catch (error) {
    log(`加入会议失败: ${error.message}`, 'error')
  }
})

elements.leaveBtn.addEventListener('click', async () => {
  try {
    if (!state.token || !state.currentMeetingId) {
      throw new Error('当前不在会议中')
    }
    normalizeResponse(await api.exitMeeting(state.apiBase, state.token))
    cleanupMeetingState(false)
    log('已退出会议')
  } catch (error) {
    log(`退出会议失败: ${error.message}`, 'error')
  }
})

elements.finishBtn.addEventListener('click', async () => {
  try {
    if (!state.token || !state.currentMeetingId) {
      throw new Error('当前不在会议中')
    }
    normalizeResponse(await api.finishMeeting(state.apiBase, state.token))
    cleanupMeetingState(false)
    log('已结束会议')
  } catch (error) {
    log(`结束会议失败: ${error.message}`, 'error')
  }
})

elements.mediaBtn.addEventListener('click', async () => {
  try {
    await ensureLocalStream()
  } catch (error) {
    log(`打开媒体设备失败: ${error.message}`, 'error')
  }
})

elements.clearLogBtn.addEventListener('click', () => {
  elements.logPanel.textContent = ''
})

window.addEventListener('beforeunload', () => {
  rtcManager.closeAllPeers()
  signalingClient.close()
  stopLocalStream()
})

updateSessionView()
updateWsStatus('未连接')
renderParticipants()
log('前端已初始化，先注册或登录后开始测试')
