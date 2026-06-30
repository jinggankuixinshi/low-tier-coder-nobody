export interface WsMessage {
  type: string
  data: any
}

export type WsEvent =
  | 'connected'
  | 'disconnected'
  | 'message'
  | 'PARSE_COMPLETE'
  | 'PARSE_FAILED'
  | 'VERIFICATION_COMPLETE'
  | 'EVALUATION_COMPLETE'
  | 'REPORT_GENERATED'
  | 'KICKED'
  | 'NEW_TASK'

export type WsCallback = (data: any) => void
export type Unsubscribe = () => void
