export interface SimulationRequest {
  stateMachineId: number
  currentStateId: string
  triggerSource: 'MANUAL' | 'CALLBACK' | 'AUTO' | ''
  callbackSource: string
  triggerData: Record<string, any>
}

export interface SimulationPath {
  transitionId: string
  transitionName: string
  fromStateId: string
  fromStateName: string
  toStateId: string
  toStateName: string
  condition: string
  conditionResult: string
  conditionMet: boolean
  triggerSource: string
  callbackSource: string
}

export interface SimulationResult {
  currentStateId: string
  currentStateName: string
  possiblePaths: SimulationPath[]
  matchedPaths: SimulationPath[]
  finalStates: string[]
  triggerSource: string
  callbackSource: string
}

export interface GanttItem {
  stateId: string
  stateName: string
  startTime: string
  endTime: string
  durationMillis: number
  durationMinutes: number
  durationDisplay: string
  isCurrent: boolean
  sequence: number
}
