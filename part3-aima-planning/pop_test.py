"""
Partial Order Planner (POP) - AIMA Chapter 11
"""
import copy


class Step:
    _counter = 0

    def __init__(self, name, precond=None, add_effects=None, del_effects=None):
        self.name = name
        self.precond = list(precond or [])
        self.add_effects = list(add_effects or [])
        self.del_effects = list(del_effects or [])
        self._id = Step._counter
        Step._counter += 1

    def achieves(self, literal):
        return literal in self.add_effects

    def threatens(self, cond):
        return cond in self.del_effects

    def __repr__(self):
        return self.name


class CausalLink:
    def __init__(self, producer_id, cond, consumer_id):
        self.producer_id = producer_id
        self.cond = cond
        self.consumer_id = consumer_id

    def __repr__(self):
        return f"#{self.producer_id} --[{self.cond}]--> #{self.consumer_id}"


def _by_id(steps, sid):
    for s in steps:
        if s._id == sid:
            return s
    return None


def _has_cycle(step_ids_by_id, orderings):
    """orderings is a list of (id_a, id_b) meaning a before b."""
    graph = {}
    for (a, b) in orderings:
        graph.setdefault(a, set()).add(b)
    visited, in_stack = set(), set()
    def dfs(n):
        visited.add(n)
        in_stack.add(n)
        for nb in graph.get(n, []):
            if nb not in visited:
                if dfs(nb): return True
            elif nb in in_stack:
                return True
        in_stack.discard(n)
        return False
    for node in list(graph):
        if node not in visited:
            if dfs(node): return True
    return False


# Plan is a dict:
#   steps       : list[Step]
#   orderings   : list[(id_a, id_b)]
#   causal_links: list[CausalLink]
# Agenda is a list of (cond:str, consumer_id:int)


def pop(initial_state, goal_state, action_schemas, verbose=False):
    # Do NOT reset Step._counter: schema _ids must stay distinct from instance _ids.
    start  = Step("Start",  precond=[],              add_effects=list(initial_state))
    finish = Step("Finish", precond=list(goal_state), add_effects=[])

    plan = {
        'steps':         [start, finish],
        'orderings':     [(start._id, finish._id)],
        'causal_links':  [],
        'used_schemas':  set(),   # schema _ids already instantiated in this plan
    }
    agenda = [(cond, finish._id) for cond in goal_state]
    return _pop(plan, agenda, action_schemas, start._id, finish._id, verbose)


def _pop(plan, agenda, action_schemas, start_id, finish_id, verbose):
    if not agenda:
        return plan

    cond, consumer_id = agenda[0]
    rest_agenda = agenda[1:]

    if verbose:
        consumer = _by_id(plan['steps'], consumer_id)
        print(f"  >> [{cond}] needed by {consumer}")

    # Providers already in the plan
    existing_providers = [s._id for s in plan['steps']
                          if s.achieves(cond) and s._id != consumer_id]
    # New action schemas not yet instantiated in this plan
    new_schemas = [s for s in action_schemas
                   if s.achieves(cond) and s._id not in plan['used_schemas']]

    candidates = [('exist', sid) for sid in existing_providers] + \
                 [('new',   s)   for s   in new_schemas]

    for kind, ref in candidates:
        new_plan = {
            'steps':        [copy.copy(s) for s in plan['steps']],
            'orderings':    list(plan['orderings']),
            'causal_links': list(plan['causal_links']),
            'used_schemas': set(plan['used_schemas']),
        }

        if kind == 'exist':
            provider_id  = ref
            extra_agenda = []
        else:
            schema = ref
            ns = Step(schema.name,
                      precond=list(schema.precond),
                      add_effects=list(schema.add_effects),
                      del_effects=list(schema.del_effects))
            new_plan['steps'].append(ns)
            new_plan['orderings'].append((start_id, ns._id))
            new_plan['orderings'].append((ns._id, finish_id))
            new_plan['used_schemas'].add(schema._id)
            provider_id  = ns._id
            extra_agenda = [(p, ns._id) for p in ns.precond]

        # Ordering: provider before consumer
        if (provider_id, consumer_id) not in new_plan['orderings']:
            new_plan['orderings'].append((provider_id, consumer_id))

        # Causal link
        link = CausalLink(provider_id, cond, consumer_id)
        new_plan['causal_links'].append(link)

        # Threat resolution
        ok = True
        for step in new_plan['steps']:
            if step._id in (link.producer_id, link.consumer_id):
                continue
            if step.threatens(link.cond):
                if verbose:
                    print(f"    ! Threat: {step} threatens [{cond}]")
                # Promotion: step < provider
                prom = new_plan['orderings'] + [(step._id, link.producer_id)]
                # Demotion: consumer < step
                dem  = new_plan['orderings'] + [(link.consumer_id, step._id)]

                if not _has_cycle(None, prom):
                    new_plan['orderings'] = prom
                    if verbose: print(f"    Promoted: {step} < #{link.producer_id}")
                elif not _has_cycle(None, dem):
                    new_plan['orderings'] = dem
                    if verbose: print(f"    Demoted: #{link.consumer_id} < {step}")
                else:
                    ok = False
                    break

        if not ok or _has_cycle(None, new_plan['orderings']):
            continue

        new_agenda = rest_agenda + extra_agenda
        result = _pop(new_plan, new_agenda, action_schemas, start_id, finish_id, verbose)
        if result is not None:
            return result

    return None


def _levels(plan):
    """Compute level (earliest parallel slot) for each step."""
    steps = plan['steps']
    level = {s._id: 0 for s in steps}
    changed = True
    while changed:
        changed = False
        for (a_id, b_id) in plan['orderings']:
            new_lv = level[a_id] + 1
            if level[b_id] < new_lv:
                level[b_id] = new_lv
                changed = True
    by_level = {}
    for s in steps:
        by_level.setdefault(level[s._id], []).append(s)
    return by_level


def print_plan(plan):
    print("\n--- Steps ---")
    for s in plan['steps']:
        print(f"  {s}  pre={s.precond}  add={s.add_effects}  del={s.del_effects}")

    print("\n--- Orderings ---")
    id2name = {s._id: s.name for s in plan['steps']}
    for (a, b) in plan['orderings']:
        print(f"  {id2name[a]} < {id2name[b]}")

    print("\n--- Causal Links ---")
    for lk in plan['causal_links']:
        print(f"  {id2name[lk.producer_id]} --[{lk.cond}]--> {id2name[lk.consumer_id]}")

    print("\n--- Parallel Levels ---")
    for lvl, steps in sorted(_levels(plan).items()):
        print(f"  Level {lvl}: {[s.name for s in steps]}")


# ── Example 1 : Socks and Shoes ──────────────────────────────────
def example_socks_shoes():
    print("=" * 55)
    print("EXAMPLE 1 -- Socks and Shoes")
    print("=" * 55)

    initial_state = []
    goal_state    = ["RightShoeOn", "LeftShoeOn"]

    actions = [
        Step("RightSock", precond=[],              add_effects=["RightSockOn"]),
        Step("LeftSock",  precond=[],              add_effects=["LeftSockOn"]),
        Step("RightShoe", precond=["RightSockOn"], add_effects=["RightShoeOn"]),
        Step("LeftShoe",  precond=["LeftSockOn"],  add_effects=["LeftShoeOn"]),
    ]

    plan = pop(initial_state, goal_state, actions, verbose=True)

    if plan is None:
        print("No plan found.")
    else:
        print_plan(plan)


# ── Example 2 : Spare Tire (delete effects -> threats) ───────────
def example_spare_tire():
    print("\n" + "=" * 55)
    print("EXAMPLE 2 -- Spare Tire")
    print("=" * 55)

    initial_state = ["At(Flat,Axle)", "At(Spare,Trunk)"]
    goal_state    = ["At(Spare,Axle)", "At(Flat,Ground)"]

    actions = [
        Step("Remove(Flat,Axle)",
             precond=["At(Flat,Axle)"],
             add_effects=["At(Flat,Ground)"],
             del_effects=["At(Flat,Axle)"]),
        Step("Remove(Spare,Trunk)",
             precond=["At(Spare,Trunk)"],
             add_effects=["At(Spare,Ground)"],
             del_effects=["At(Spare,Trunk)"]),
        Step("PutOn(Spare,Axle)",
             precond=["At(Spare,Ground)"],
             add_effects=["At(Spare,Axle)"],
             del_effects=["At(Spare,Ground)"]),
    ]

    plan = pop(initial_state, goal_state, actions, verbose=True)

    if plan is None:
        print("No plan found.")
    else:
        print_plan(plan)


if __name__ == "__main__":
    example_socks_shoes()
    example_spare_tire()
