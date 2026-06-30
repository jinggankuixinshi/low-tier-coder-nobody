import type { WsCallback, WsEvent, WsMessage } from '@/types/websocket'

const wsProtocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
const BASE_URL = `${wsProtocol}//${location.host}/ws/notification`

class WebSocketClient {
  private ws: WebSocket | null = null
  private listeners: Map<string, Set<WsCallback>> = new Map()
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null
  private reconnectAttempts = 0
  private maxReconnectAttempts = 10
  private reconnectDelay = 3000

  connect(token: string): void {
    if (!token) return

    if (this.ws) {
      this.ws.close()
    }

    const url = `${BASE_URL}?token=${token}`
    this.ws = new WebSocket(url)

    this.ws.onopen = () => {
      this.reconnectAttempts = 0
      this._emit('connected', {})
    }

    this.ws.onmessage = (event: MessageEvent) => {
      try {
        const message: WsMessage = JSON.parse(event.data)
        this._emit(message.type as WsEvent, message.data)
        this._emit('message', message)
      } catch {
        // ignore parse errors
      }
    }

    this.ws.onclose = () => {
      this._emit('disconnected', {})
      this._tryReconnect(token)
    }

    this.ws.onerror = () => {
      // Error is followed by onclose, reconnect there
    }
  }

  disconnect(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
  }

  private _tryReconnect(token: string): void {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) return
    this.reconnectTimer = setTimeout(() => {
      this.reconnectAttempts++
      this.connect(token)
    }, this.reconnectDelay)
  }

  on(event: string, callback: WsCallback): () => void {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, new Set())
    }
    this.listeners.get(event)!.add(callback)
    return () => {
      this.listeners.get(event)?.delete(callback)
    }
  }

  off(event: string, callback: WsCallback): void {
    this.listeners.get(event)?.delete(callback)
  }

  private _emit(event: string, data: unknown): void {
    const callbacks = this.listeners.get(event)
    if (callbacks) {
      callbacks.forEach((cb) => cb(data))
    }
  }
}

export default new WebSocketClient()
