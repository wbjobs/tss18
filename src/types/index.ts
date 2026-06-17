export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  list: T[]
  total: number
}

export interface StateNode {
  id: string
  name: string
  type: string
  x: number
  y: number
  color: string
  permissions: string[]
}

export interface Transition {
  id: string
  name: string
  sourceStateId: string
  targetStateId: string
  condition: string
  triggerSource: string
  callbackSource: string
}

export interface StateMachine {
  id?: string
  name?: string
  description?: string
  nodes: StateNode[]
  transitions: Transition[]
  createdAt?: string
  updatedAt?: string
}

export interface Ticket {
  id: string
  title: string
  description?: string
  stateMachineId: string
  currentStateId: string
  businessKey?: string
  context?: Record<string, any>
  createdAt?: string
  updatedAt?: string
}

export interface TraceRecord {
  id: string
  ticketId: string
  fromStateId: string
  toStateId: string
  transitionId: string
  operator?: string
  context?: Record<string, any>
  createdAt?: string
}

export interface WebhookConfig {
  id: string
  name: string
  url: string
  method: string
  headers?: Record<string, string>
  stateMachineId: string
  events: string[]
  enabled: boolean
  createdAt?: string
  updatedAt?: string
}

export type { SimulationRequest, SimulationPath, SimulationResult, GanttItem } from './simulation'
