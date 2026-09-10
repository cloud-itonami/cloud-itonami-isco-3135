# Operator Guide

## Overview

This guide is for plant operators deploying the MetalOpsActor system for metal production plant operations coordination.

## What the actor can do

The MetalOpsActor coordinates routine operational logging and maintenance scheduling under independent human oversight:

- **Log furnace readings** — Temperature, pressure, output rate, alloy composition observations are logged as timestamped records in the audit ledger.
- **Schedule maintenance** — Refractory replacement, electrode maintenance, equipment inspections are proposed and subject to your approval before scheduling.
- **Flag anomalous readings** — Out-of-range observations trigger escalation to you for immediate review and sign-off.
- **Flag temperature deviations** — Significant furnace temperature deviations from setpoint trigger escalation to you.
- **Coordinate shift handovers** — Shift changeover notes, notable conditions, and handoff checklists are logged and approved.

## What the actor CANNOT do

**CRITICAL:** The MetalOpsActor is an **operations coordination system only**. It does **NOT** control furnace operations:

- ✗ Does NOT start or stop furnaces.
- ✗ Does NOT control furnace temperature setpoints.
- ✗ Does NOT adjust alloy composition or material feed rates.
- ✗ Does NOT dispatch emergency shutdown.

All furnace control, process adjustments, and emergency decisions remain **exclusively under your authority as the licensed plant operator**.

The Governor (built into the system) enforces this boundary at every step. Any proposal that attempts to cross this boundary is automatically rejected and audited.

## Deployment steps

### 1. Install

```bash
git clone https://github.com/cloud-itonami/cloud-itonami-isco-3135.git
cd cloud-itonami-isco-3135
clojure -M:test
```

Ensure all tests pass.

### 2. Register your plant/furnace

Before the actor can process any request, your plant and furnaces must be registered and marked as verified in the store.

```clojure
(require '[metal-ops.actor :as actor]
         '[metal-ops.store :as store])

;; Create a store and register your plant/furnace
(def my-plant {:plant-id :my-steel-mill
               :name "My Steel Mill Furnace A"
               :status :operational
               :verified? true})

(def s (store/mem-store {:my-steel-mill my-plant}))

;; Build the actor graph
(def g (actor/build-graph {:store s}))
```

### 3. Process requests

Submit operational requests to the actor:

```clojure
;; Log a furnace reading
(actor/run-request! g "thread-1"
  {:plant-id :my-steel-mill
   :op :log-furnace-reading
   :payload {:temperature 1350 :pressure 2.1 :output-rate 45}})

;; The result will indicate: :ok (committed), :escalate (awaiting your approval),
;; or :hold (rejected due to safety/validation).
```

### 4. Approve escalated proposals

Any anomalous reading or low-confidence proposal escalates to you:

```clojure
;; After human review, approve and resume:
(actor/approve! g "thread-1")
```

## Audit trail

All decisions (commits, holds, escalations) are logged to an append-only audit ledger:

```clojure
;; Inspect the audit ledger
(store/ledger s)
;; Output: [{:disposition :commit :record {...}} ...]
```

This ledger is tamper-evident and suitable for regulatory compliance audits.

## Customization

### Swap the Advisor

Replace the `mock-advisor` with your own LLM or domain-specific model:

```clojure
(require '[metal-ops.advisor :as advisor])

;; Implement the Advisor protocol
(deftype MyAdvisor []
  advisor/Advisor
  (-advise [_ store request]
    ;; Your logic here
    {:op :log-furnace-reading :payload {...} :confidence 0.95 :effect :propose}))

(def g (actor/build-graph {:store s :advisor (MyAdvisor.)}))
```

### Customize the Governor

The Governor rules are in `src/metal_ops/governor.kotoba`. You can fork and customize:
- Add new escalation operators.
- Adjust the confidence floor.
- Add plant-specific safety rules.

Always ensure:
1. Furnace/smelting control operations remain forbidden.
2. Escalation workflows preserve human approval.
3. All decisions are audit-logged.

## Support

For issues, questions, or contributions, open a GitHub issue or PR on this repository.
