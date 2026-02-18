# Hazardous Datapack Guide

This guide covers Hazardous datapack formats, config, gameplay items, and testing.

Included gameplay items:
- `hazardous:gasmask`
- `hazardous:filter`
- `hazardous:geiger_counter`
- `hazardous:pills`

## 0) Config Setup First (Required)

`hazardous-server.toml` defaults to:

```toml
enabledHazardTypes = []
enabledHazardSources = []
```

That means no hazards run until you opt in.

Built-in hazard type ids you can enable:
- `hazardous:solar_burn`
- `hazardous:radioactive_source`
- `hazardous:lostcity_radiation`
- `hazardous:lava_heat`

Built-in hazard source ids you can enable:
- `hazardous:overworld_solar`
- `hazardous:radioactive_zombie`
- `hazardous:lostcity_buildings`
- `hazardous:near_lava`

Enable hazards by listing both:
1. The hazard type id in `enabledHazardTypes`
2. The hazard source id in `enabledHazardSources`

Example (enable solar + lava only):

```toml
enabledHazardTypes = ["hazardous:solar_burn", "hazardous:lava_heat"]
enabledHazardSources = ["hazardous:overworld_solar", "hazardous:near_lava"]
```

Rules for correct setup:
- Every id must be a valid resource location (`namespace:path`).
- A source only works if its referenced hazard type is also enabled.
- Custom datapack ids are allowed, but they must exist in loaded datapacks.

## Quick Start

Put custom hazard JSON files in your datapack under:
- `data/<namespace>/hazardous/hazardtypes/*.json`
- `data/<namespace>/hazardous/hazardsources/*.json`
- `data/<namespace>/hazardous/effectentries/*.json`

Mental model:
1. A `hazardsource` says where a hazard exists.
2. A `hazardtype` says how strong it is and how exposure accumulates.
3. `effectentries` say what happens when exposure or dose reaches certain values.
4. Items and config (gas mask, pills, geiger) modify or visualize runtime behavior.

## 1) HazardType JSON

Codec: `mcjty.hazardous.data.objects.HazardType.CODEC`

Top-level fields:
- `transmission`: `sky`, `point`, or `contact`
- `falloff`: `none`, `inverse_square`, `linear`, or `exponential`
- `blocking`: `none`, `simple`, or `absorption`
- `exposure`: timing plus accumulation behavior
- `effects`: list of effect entry ids (optional, defaults to `[]`)

### 1.1 Transmission

`sky`
- `baseIntensity` (double)
- `requiresDirectSky` (boolean)
- `rainMultiplier` (double)
- `thunderMultiplier` (double)
- `nightMultiplier` (double)
- `indoorLeak` (double)
- Works with source associations: `level`, `biome`, `city`

`point`
- `baseIntensity` (double)
- `maxDistance` (int)
- `requiresLineOfSight` (boolean)
- `airAttenuationPerBlock` (double)
- Works with source associations: `locations`, `entity_type`, `block`

`contact`
- `baseIntensity` (double)
- Works with source associations: `entity_type`, `block`

### 1.2 Falloff

`none`
- no fields

`inverse_square`
- `minDistance` (double)

`linear`
- no fields
- behavior: `base * max(0, 1 - d/maxDistance)`

`exponential`
- `k` (double)
- behavior: `base * exp(-k * d)`

### 1.3 Blocking

`none`
- no fields

`simple`
- `solidBlockMultiplier` (double)
- `fluidMultiplier` (double)
- `treatLeavesAsSolid` (boolean)

`absorption`
- `absorptionRegistryHint` (resource location)
- `defaultAbsorption` (double)

Known runtime behavior:
- `blocking` is parsed and stored but not yet applied in runtime hazard calculations.
- `point.requiresLineOfSight` is not enforced in runtime hazard calculations.

These fields are still useful for forward-compatible datapacks.

### 1.4 Exposure

- `applyIntervalTicks` (int): evaluation interval (`20` = once per second)
- `accumulate` (boolean): whether to keep per-player dose
- `exponential` (boolean): if capped, accumulation slows near max
- `maximum` (double): `<= 0` means uncapped
- `decayPerTick` (double): decay applied at each evaluation

### 1.5 Example HazardType (point radiation)

`data/example/hazardous/hazardtypes/radiation_point.json`

```json
{
  "transmission": {
    "type": "point",
    "baseIntensity": 1.0,
    "maxDistance": 12,
    "requiresLineOfSight": true,
    "airAttenuationPerBlock": 0.05
  },
  "falloff": { "type": "exponential", "k": 0.18 },
  "blocking": {
    "type": "absorption",
    "absorptionRegistryHint": "hazardous:absorption_hints",
    "defaultAbsorption": 0.2
  },
  "exposure": {
    "applyIntervalTicks": 10,
    "accumulate": true,
    "exponential": false,
    "maximum": 200.0,
    "decayPerTick": 0.002
  },
  "effects": [
    "example:radiation_damage"
  ]
}
```

## 2) HazardSource JSON

Codec: `mcjty.hazardous.data.objects.HazardSource.CODEC`

Top-level fields:
- `hazardType`: resource location
- `association`: where this hazard exists

Association variants:

`level`
- `type: "level"`
- `level`: dimension id
- valid with `sky`

`entity_type`
- `type: "entity_type"`
- `entityType`
- `maxDistance`
- valid with `point`, `contact`

`locations`
- `type: "locations"`
- `level`
- `positions`: list of `{ "x": ..., "y": ..., "z": ... }`
- valid with `point`

`biome`
- `type: "biome"`
- `biome`
- valid with `sky`

`city`
- `type: "city"`
- no extra fields
- valid with `sky`
- only works when Lost Cities is installed

`block`
- `type: "block"`
- exactly one of `block` or `tag`
- `maxDistance`
- valid with `point`, `contact`

Validation note:
- Reload validates association and transmission compatibility.
- If they do not match, the datapack is rejected.

### 2.1 Source examples

Entire overworld gets solar hazard:

```json
{
  "hazardType": "example:solar_burn",
  "association": {
    "type": "level",
    "level": "minecraft:overworld"
  }
}
```

All zombies act as radioactive point sources:

```json
{
  "hazardType": "example:radioactive_source",
  "association": {
    "type": "entity_type",
    "entityType": "minecraft:zombie",
    "maxDistance": 3.0
  }
}
```

Near-lava heat hazard:

```json
{
  "hazardType": "example:lava_heat",
  "association": {
    "type": "block",
    "block": "minecraft:lava",
    "maxDistance": 4.0
  }
}
```

## 3) EffectEntry JSON

Codec: `mcjty.hazardous.data.objects.EffectEntry.CODEC`

Top-level fields:
- `trigger`
- `action`

### 3.1 Trigger

`threshold`
- `min`
- optional `hysteresis` (parsed but not used by runtime logic)

`range`
- `min`
- `max`
- runtime behavior: trigger condition is `value >= min` (not hard-capped at `max`)
- `max` is still used to clamp computed factor to `0..1`

`probability`
- `scaling`: produces chance, clamped to `0..1`

### 3.2 Action

`potion`
- applies a mob effect

`damage`
- uses `damageType` by path (`magic`, `on_fire`, `in_fire`, `wither`, fallback generic)

`fire`
- sets fire seconds (scaled and clamped)

`attribute`
- no-op placeholder

`client_fx`
- sends a client FX packet to the affected player
- fields:
  - `fxId` (string): effect id on client
  - `intensity` (scaling, optional): defaults to constant `1.0`
  - `durationTicks` (int, optional): defaults to `40`
- server-side clamps:
  - final intensity must be `> 0` to send
  - `durationTicks` is clamped to `1..1200`
- client-side clamps:
  - intensity is clamped to `0.0..2.0`
  - duration is clamped to `1..1200`
- repeated activations with the same `fxId` refresh the effect:
  - peak intensity becomes the max of old/new
  - remaining duration becomes the max of old/new
  - intensity then linearly fades to zero over remaining duration
- recognized `fxId` values:
  - `darken`: draws a black full-screen overlay (vignette-like darkening)
  - `blur`: draws a gray full-screen haze overlay
  - `shake` or `shaking`: camera yaw/pitch jitter
  - `warp` or `warping`: camera roll/yaw/pitch wobble
- unknown `fxId` values are accepted and tracked but have no visible effect unless client code uses them
- note: `geiger` is used by default data and has no dedicated visual/audio behavior in `ClientFxManager`

`command`
- no-op placeholder (disabled for safety)

### 3.3 Scaling

Variants:
- `constant`
- `linear01`
- `clamp`
- `power`

Example probability trigger with scaling:

```json
{
  "trigger": {
    "type": "probability",
    "scaling": { "type": "linear01", "min": 0.05, "max": 1.0 }
  },
  "action": {
    "type": "client_fx",
    "fxId": "shake",
    "durationTicks": 20
  }
}
```

## 4) Items and Hazard Interaction

This section documents item behavior and practical use.

### 4.1 Gas Mask (`hazardous:gasmask`)

Behavior:
- Must be worn in helmet slot.
- Only protects one configured hazard type id (`gasmaskProtectedSource`).
- Protection amount is `gasmaskProtectionLevel` (0.0 to 1.0).
- Each protection application consumes 1 durability.
- At 0 durability it stays equipped but no longer protects.

Crafting recipe:

```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["iii", "gfg", "iii"],
  "key": {
    "i": { "item": "minecraft:iron_ingot" },
    "g": { "item": "minecraft:glass" },
    "f": { "item": "hazardous:filter" }
  },
  "result": { "item": "hazardous:gasmask" }
}
```

### 4.2 Filter (`hazardous:filter`)

Behavior:
- Refills damaged gas masks by `gasmaskFilterRestore`.
- Two supported usages:
1. Right-click while wearing a damaged gas mask.
2. Crafting grid with exactly one damaged gas mask plus one filter (custom recipe type `hazardous:gasmask_filter_refill`).

### 4.3 Pills (`hazardous:pills`)

Behavior:
- Right-click consumes pills (unless creative).
- Removes `pillsDoseHeal` from every stored player dose entry.
- If there is no dose to remove, nothing is consumed.

Recipe:

```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["ccc", "cgc", "ccc"],
  "key": {
    "c": { "item": "minecraft:charcoal" },
    "g": { "item": "minecraft:golden_apple" }
  },
  "result": { "item": "hazardous:pills" }
}
```

### 4.4 Geiger Counter (`hazardous:geiger_counter`)

Behavior:
- Shows HUD dial when:
1. Held in selected hotbar slot, or
2. Equipped in Curios (if Curios is installed)
- Reads a configured hazard id from client radiation data: `geigerDisplayResource`.
- Dial full scale is `geigerMaxRadiation`.
- Position controlled by `geigerHudAnchor`, `geigerHudOffsetX`, `geigerHudOffsetY`.

Important:
- The geiger HUD does not depend on `client_fx`; it uses hazard values synced from server to client.

Recipe:

```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["qtq", "dcd", "qiq"],
  "key": {
    "q": { "item": "minecraft:quartz" },
    "t": { "item": "minecraft:redstone_torch" },
    "d": { "item": "minecraft:diamond" },
    "c": { "item": "minecraft:comparator" },
    "i": { "item": "minecraft:iron_ingot" }
  },
  "result": { "item": "hazardous:geiger_counter" }
}
```

## 5) Config Options (Server + Client)

This section documents config options besides `enabledHazardTypes` and `enabledHazardSources`.

Server config (`hazardous-server.toml`):
- `gasmaskProtectedSource` (string resource location, default `hazardous:radioactive_source`)
- `gasmaskProtectionLevel` (double `0.0..1.0`, default `0.75`)
- `gasmaskFilterRestore` (int `1..1000000`, default `250`)
- `pillsDoseHeal` (double `0.0..1000000.0`, default `20.0`)

Client config (`hazardous-client.toml`):
- `geigerDisplayResource` (string resource location, default `hazardous:radioactive_source`, empty disables dial target)
- `geigerMaxRadiation` (double `0.0001..1000000.0`, default `100.0`)
- `geigerHudAnchor` (string: `top_left`, `top_right`, `bottom_left`, `bottom_right`; default `top_right`)
- `geigerHudOffsetX` (int `-5000..5000`, default `8`)
- `geigerHudOffsetY` (int `-5000..5000`, default `8`)

Example:

```toml
# hazardous-server.toml
gasmaskProtectedSource = "hazardous:radioactive_source"
gasmaskProtectionLevel = 0.75
gasmaskFilterRestore = 250
pillsDoseHeal = 20.0

# hazardous-client.toml
geigerDisplayResource = "hazardous:radioactive_source"
geigerMaxRadiation = 100.0
geigerHudAnchor = "top_right"
geigerHudOffsetX = 8
geigerHudOffsetY = 8
```

## 6) Debugging and Testing

Useful commands:
- `/hazardous radiationhere`
- `/hazardous dose`
- `/hazardous resetdose`

Alias:
- `/haz radiationhere`
- `/haz dose`
- `/haz resetdose`

Suggested workflow:
1. Use `/haz radiationhere` where your source should apply.
2. Use `/haz dose` after waiting a few seconds to confirm accumulation.
3. Use pills and run `/haz dose` again to confirm dose reduction.
4. Wear gas mask and compare `/haz dose` growth with and without mask.

## 7) Built-In Data (Reference)

Built-in ids:
- Hazard types: `hazardous:solar_burn`, `hazardous:radioactive_source`, `hazardous:lostcity_radiation`, `hazardous:lava_heat`
- Hazard sources: `hazardous:overworld_solar`, `hazardous:radioactive_zombie`, `hazardous:lostcity_buildings`, `hazardous:near_lava`
- Effect entries: `hazardous:solar_weakness`, `hazardous:solar_ignite`, `hazardous:radiation_damage`, `hazardous:radiation_geiger`, `hazardous:lava_fire_damage`

Use these as working templates when creating your own pack.
