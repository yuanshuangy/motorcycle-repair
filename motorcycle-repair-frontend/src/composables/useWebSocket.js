import { ref } from 'vue'
import { ElNotification } from 'element-plus'

const connected = ref(false)
let ws = null
let reconnectTimer = null

export function useWebSocket() {
  const connect = (shopId) => {
    if (ws && ws.readyState === WebSocket.OPEN) return
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = window.location.host
    const wsUrl = `${protocol}//${host}/ws/shop/${shopId}`

    try {
      ws = new WebSocket(wsUrl)

      ws.onopen = () => {
        connected.value = true
        if (reconnectTimer) { clearTimeout(reconnectTimer); reconnectTimer = null }
      }

      ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data)
          if (data.type === 'NEW_APPOINTMENT') {
            ElNotification({
              title: '🔔 新预约通知',
              message: `${data.userName}预约了${data.serviceName}，预约时间：${data.appointmentTime}`,
              type: 'success',
              duration: 10000,
            })
            try { new Audio('/notification.mp3').play() } catch {}
          }
        } catch {}
      }

      ws.onclose = () => {
        connected.value = false
        reconnectTimer = setTimeout(() => connect(shopId), 5000)
      }

      ws.onerror = () => {
        connected.value = false
      }
    } catch {
      connected.value = false
    }
  }

  const disconnect = () => {
    if (reconnectTimer) { clearTimeout(reconnectTimer); reconnectTimer = null }
    if (ws) { ws.close(); ws = null }
    connected.value = false
  }

  return { connected, connect, disconnect }
}
