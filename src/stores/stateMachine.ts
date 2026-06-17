import { defineStore } from 'pinia'
import type { StateNode, Transition, StateMachine, PageResult } from '@/types'
import {
  getStateMachineList,
  getStateMachine,
  createStateMachine,
  updateStateMachine,
  publishStateMachine,
  offlineStateMachine,
  deleteStateMachine,
} from '@/api/stateMachine'

export const useStateMachineStore = defineStore('stateMachine', {
  state: () => ({
    currentMachine: null as StateMachine | null,
    selectedNode: null as StateNode | null,
    selectedTransition: null as Transition | null,
    nodes: [] as StateNode[],
    transitions: [] as Transition[],
    isDirty: false,
    stateMachines: [] as StateMachine[],
    total: 0,
    loading: false,
  }),
  getters: {
    getNodeById: (state) => (id: string) => {
      return state.nodes.find(node => node.id === id) || null
    },
    getTransitionById: (state) => (id: string) => {
      return state.transitions.find(transition => transition.id === id) || null
    },
  },
  actions: {
    async fetchStateMachines() {
      this.loading = true
      try {
        const res = await getStateMachineList({ page: 1, size: 10 })
        if (res.code === 200) {
          this.stateMachines = res.data.list
          this.total = res.data.total
        }
        return res
      } finally {
        this.loading = false
      }
    },
    async fetchStateMachine(id: number | string) {
      this.loading = true
      try {
        const res = await getStateMachine(Number(id))
        if (res.code === 200 && res.data) {
          this.setCurrentMachine(res.data)
        }
        return res
      } finally {
        this.loading = false
      }
    },
    async create(name: string, description: string) {
      const res = await createStateMachine({ name, description })
      if (res.code === 200 && res.data) {
        this.setCurrentMachine(res.data)
      }
      return res
    },
    async save(id: number) {
      const res = await updateStateMachine(id, {
        nodes: this.nodes,
        transitions: this.transitions,
      })
      if (res.code === 200 && res.data) {
        this.setCurrentMachine(res.data)
      }
      return res
    },
    async publish(id: number) {
      const res = await publishStateMachine(id)
      if (res.code === 200 && res.data) {
        this.setCurrentMachine(res.data)
      }
      return res
    },
    async offline(id: number) {
      const res = await offlineStateMachine(id)
      return res
    },
    async remove(id: number) {
      const res = await deleteStateMachine(id)
      return res
    },
    setCurrentMachine(machine: StateMachine) {
      this.currentMachine = machine
      this.nodes = machine.nodes || []
      this.transitions = machine.transitions || []
      this.isDirty = false
    },
    addNode(node: StateNode) {
      this.nodes.push(node)
      this.isDirty = true
    },
    updateNode(id: string, data: Partial<StateNode>) {
      const index = this.nodes.findIndex(node => node.id === id)
      if (index !== -1) {
        this.nodes[index] = { ...this.nodes[index], ...data }
        this.isDirty = true
      }
    },
    removeNode(id: string) {
      this.nodes = this.nodes.filter(node => node.id !== id)
      this.transitions = this.transitions.filter(
        t => t.sourceStateId !== id && t.targetStateId !== id
      )
      if (this.selectedNode?.id === id) {
        this.selectedNode = null
      }
      this.isDirty = true
    },
    addTransition(transition: Transition) {
      this.transitions.push(transition)
      this.isDirty = true
    },
    updateTransition(id: string, data: Partial<Transition>) {
      const index = this.transitions.findIndex(t => t.id === id)
      if (index !== -1) {
        this.transitions[index] = { ...this.transitions[index], ...data }
        this.isDirty = true
      }
    },
    removeTransition(id: string) {
      this.transitions = this.transitions.filter(t => t.id !== id)
      if (this.selectedTransition?.id === id) {
        this.selectedTransition = null
      }
      this.isDirty = true
    },
    selectNode(node: StateNode | null) {
      this.selectedNode = node
      this.selectedTransition = null
    },
    selectTransition(transition: Transition | null) {
      this.selectedTransition = transition
      this.selectedNode = null
    },
    setDirty(dirty: boolean) {
      this.isDirty = dirty
    },
    reset() {
      this.currentMachine = null
      this.selectedNode = null
      this.selectedTransition = null
      this.nodes = []
      this.transitions = []
      this.isDirty = false
    },
  },
})
