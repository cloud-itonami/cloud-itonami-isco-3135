(ns metal-ops.actor
  "MetalOpsActor — the ISCO-08 3135 metal production process operations coordination
  actor as a `langgraph.graph/state-graph` (per ADR-2607011000 / CLAUDE.md Actors
  section). One graph run = one operations coordination request (intake → advise →
  govern → decide → commit/hold, with a human-approval interrupt for escalated
  proposals). No infinite internal loop; checkpointed per superstep so an interrupted
  run can resume after human sign-off.

  CRITICAL DESIGN NOTE: This actor supports plant-operator BACK-OFFICE
  COORDINATION WORKFLOW only. It does NOT directly control furnaces,
  smelting processes, temperature/composition control, or emergency shutdown —
  those remain exclusively under licensed plant operators' human authority.
  The Governor enforces this hard boundary (see `metal-ops.governor`).

  State graph:
  ```text
  :intake -> :advise -> :govern -> :decide -+-> :commit           (:ok? true)
                                             +-> :request-approval  (:escalate? true, interrupt-before)
                                             +-> :hold              (:hard? true)
  ```

  The unconditional invariant: the MetalOpsAdvisor can never directly
  commit a record or dispatch a robot action the MetalOpsGovernor
  refuses — every commit-record! call is gated behind `:decide`."
  (:require [langgraph.graph :as g]
            [langgraph.checkpoint :as cp]
            [metal-ops.advisor :as advisor]
            [metal-ops.governor :as governor]
            [metal-ops.store :as store]))

(defn build-graph
  "Build a compiled MetalOpsActor graph. `store` implements
  `metal-ops.store/Store`. `advisor` implements `metal-ops.advisor/Advisor`
  (defaults to `mock-advisor`). `checkpointer` defaults to an in-memory one."
  [{:keys [store advisor checkpointer]
    :or {advisor (advisor/mock-advisor)
         checkpointer (cp/mem-checkpointer)}}]
  (-> (g/state-graph
       {:channels
        {:request     {:default nil}
         :context     {:default nil}
         :proposal    {:default nil}
         :verdict     {:default nil}
         :disposition {:default nil}
         :record      {:default nil}
         :audit       {:reducer into :default []}}})
      (g/add-node :intake (fn [s] s))
      (g/add-node :advise
                   (fn [{:keys [request]}]
                     (let [p (advisor/-advise advisor store request)]
                       {:proposal p
                        :audit [{:node :advise :request request :proposal p}]})))
      (g/add-node :govern
                   (fn [{:keys [request context proposal]}]
                     (let [v (governor/check request context proposal store)]
                       {:verdict v
                        :audit [{:node :govern :verdict v}]})))
      (g/add-node :decide
                   (fn [{:keys [verdict]}]
                     {:disposition (cond
                                     (:hard? verdict) :hold
                                     (:escalate? verdict) :request-approval
                                     :else :commit)}))
      (g/add-node :request-approval (fn [s] s))
      (g/add-node :commit
                   (fn [{:keys [request proposal]}]
                     (let [record {:plant-id (:plant-id request)
                                    :op (:op proposal)
                                    :payload proposal}]
                       (store/commit-record! store record)
                       (store/append-ledger! store {:disposition :commit :record record})
                       {:record record
                        :audit [{:node :commit :record record}]})))
      (g/add-node :hold
                   (fn [{:keys [verdict]}]
                     (store/append-ledger! store {:disposition :hold :verdict verdict})
                     {:audit [{:node :hold :verdict verdict}]}))
      (g/set-entry-point :intake)
      (g/add-edge :intake :advise)
      (g/add-edge :advise :govern)
      (g/add-edge :govern :decide)
      (g/add-conditional-edges
       :decide
       (fn [{:keys [disposition]}]
         (case disposition
           :commit :commit
           :request-approval :request-approval
           :hold)))
      (g/add-edge :request-approval :commit)
      (g/set-finish-point :commit)
      (g/set-finish-point :hold)
      (g/compile-graph {:checkpointer checkpointer
                         :interrupt-before #{:request-approval}})))

(defn run-request!
  "Run one operation request to completion or interrupt. `thread-id`
  scopes checkpointing for resume after human approval. Returns the
  full run result: `{:state .. :events .. :status :done|:interrupted
  :frontier ..}`."
  [graph thread-id request]
  (g/invoke graph
            {:request request}
            {:thread-id thread-id}))

(defn approve!
  "Resume an interrupted graph run after human approval. Returns the
  result of the resumed run."
  [graph thread-id]
  (g/invoke graph nil {:thread-id thread-id}))
