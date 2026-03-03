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
2. A `hazardsource` also says how the hazard transmits (`sky`, `point`, `contact`).
3. A `hazardtype` says falloff, blocking, exposure, and effects.
4. `effectentries` say what happens when exposure or dose reaches certain values.
5. Items and config (gas mask, pills, geiger) modify or visualize runtime behavior.

## 1) HazardType JSON

Codec: `mcjty.hazardous.data.objects.HazardType.CODEC`

Top-level fields:
- `falloff`: `none`, `inverse_square`, `linear`, or `exponential`
- `blocking`: `none`, `simple`, or `absorption`
- `exposure`: timing plus accumulation behavior
- `effects`: list of effect entry ids (optional, defaults to `[]`)

### 1.1 Falloff

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

### 1.2 Blocking

`none`
- no fields

`simple`
- `solidBlockMultiplier` (double)
- `fluidMultiplier` (double)
- `treatLeavesAsSolid` (boolean)

`absorption`
- `absorptionRegistryHint` (resource location)
- `defaultAbsorption` (double)
- `blocks` (optional list): per-block overrides
  - `block` (resource location)
  - `absorption` (double `0.0..1.0`)
- `tags` (optional list): per-tag overrides
  - `tag` (resource location)
  - `absorption` (double `0.0..1.0`)

Known runtime behavior:
- `blocking.type = absorption` is applied for `transmission = sky` and `transmission = point`.
- Sky absorption traces vertically from world top to player.
- Point absorption traces from source to player body/head and applies multiplicative attenuation.
- `hazardsource.transmission.point.requiresLineOfSight` is not enforced in runtime hazard calculations.

These fields are still useful for forward-compatible datapacks.

### 1.4 Exposure

- `applyIntervalTicks` (int): evaluation interval (`20` = once per second)
- `accumulate` (boolean): whether to keep per-player dose
- `exponential` (boolean): if capped, accumulation slows near max
- `maximum` (double): `<= 0` means uncapped
- `decayPerTick` (double): decay applied at each evaluation

### 1.5 Example HazardType (point radiation shared settings)

`data/example/hazardous/hazardtypes/radiation_point.json`

```json
{
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
- `transmission`: `sky`, `point`, or `contact`
- `association`: where this hazard exists

Transmission variants:

`sky`
- `type: "sky"`
- `baseIntensity` (double)
- `requiresDirectSky` (boolean)
- `rainMultiplier` (double)
- `thunderMultiplier` (double)
- `nightMultiplier` (double)
- `indoorLeak` (double)
- valid with associations: `level`, `biome`, `city`

`point`
- `type: "point"`
- `baseIntensity` (double)
- `maxDistance` (int)
- `requiresLineOfSight` (boolean)
- `airAttenuationPerBlock` (double)
- valid with associations: `locations`, `entity_type`, `block`

`contact`
- `type: "contact"`
- `baseIntensity` (double)
- valid with associations: `entity_type`, `block`

Association variants:

`level`
- `type: "level"`
- `level`: dimension id

`entity_type`
- `type: "entity_type"`
- `entityType`
- `maxDistance`

`locations`
- `type: "locations"`
- `level`
- `positions`: list of `{ "x": ..., "y": ..., "z": ... }`

`biome`
- `type: "biome"`
- `biome`

`city`
- `type: "city"`
- no extra fields
- only works when Lost Cities is installed

`block`
- `type: "block"`
- exactly one of `block` or `tag`
- `maxDistance`

Validation note:
- Reload validates association and transmission compatibility.
- If they do not match, the datapack is rejected.

### 2.1 Source examples

Entire overworld gets solar hazard:

```json
{
  "hazardType": "example:solar_burn",
  "transmission": {
    "type": "sky",
    "baseIntensity": 0.12,
    "requiresDirectSky": true,
    "rainMultiplier": 0.25,
    "thunderMultiplier": 0.1,
    "nightMultiplier": 0.0,
    "indoorLeak": 0.05
  },
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
  "transmission": {
    "type": "point",
    "baseIntensity": 1.0,
    "maxDistance": 12,
    "requiresLineOfSight": true,
    "airAttenuationPerBlock": 0.05
  },
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
  "transmission": {
    "type": "point",
    "baseIntensity": 0.8,
    "maxDistance": 4,
    "requiresLineOfSight": false,
    "airAttenuationPerBlock": 0.0
  },
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

All trigger objects require a `type` field.

`threshold` trigger
- Required fields:
  - `type` = `"threshold"`
  - `min` (double)
- Optional fields:
  - `hysteresis` (double, default `0.0`)
- Runtime behavior:
  - fires when `value >= min`
  - factor is `1.0` when `value >= min`, otherwise `0.0`
  - `hysteresis` is parsed and stored but not used in current runtime logic

`range` trigger
- Required fields:
  - `type` = `"range"`
  - `min` (double)
  - `max` (double)
- Runtime behavior:
  - fires when `value >= min` (not hard-stopped at `max`)
  - factor is normalized with `(value - min) / (max - min)` and clamped to `0..1`
  - if `value > max`, value is treated as `max` for factor calculation
  - if `max <= min`, factor becomes `1.0` for `value >= min`, else `0.0`

`probability` trigger
- Required fields:
  - `type` = `"probability"`
  - `scaling` (Scaling object; see section 3.3)
- Runtime behavior:
  - chance is `scaling.eval(value)` clamped to `0..1`
  - NaN chance is treated as `0.0`
  - fires when `random < chance`
  - factor is also `scaling.eval(value)` clamped to `0..1`

### 3.2 Action

All action objects require a `type` field.

`potion` action
- Required fields:
  - `type` = `"potion"`
  - `effect` (resource location in `minecraft:mob_effect` registry)
  - `durationTicks` (int)
  - `amplifier` (int)
- Optional fields:
  - `ambient` (boolean, default `false`)
  - `showParticles` (boolean, default `true`)
  - `showIcon` (boolean, default `true`)
  - `intensityToAmplifier` (Scaling, default `{ "type": "constant", "value": 1.0 }`)
- Runtime behavior:
  - skipped when trigger factor `<= 0`
  - duration is clamped to at least `1`
  - final amplifier = `round(amplifier * intensityToAmplifier.eval(value) * factor)`, clamped to `>= 0`
  - if `effect` id is unknown, action does nothing

`damage` action
- Required fields:
  - `type` = `"damage"`
  - `damageType` (resource location; path is used for mapping)
  - `amount` (double)
- Optional fields:
  - `scaleAmount` (Scaling, default `{ "type": "constant", "value": 1.0 }`)
- Runtime behavior:
  - skipped when factor `<= 0`
  - final damage = `amount * scaleAmount.eval(value) * factor`, clamped to `>= 0`
  - supported mapped paths:
    - `magic` -> magic damage source
    - `on_fire` -> on-fire damage source
    - `in_fire` -> in-fire damage source
    - `wither` -> wither damage source
  - any other path falls back to generic damage

`fire` action
- Required fields:
  - `type` = `"fire"`
  - `seconds` (int)
- Optional fields:
  - `scaleSeconds` (Scaling, default `{ "type": "constant", "value": 1.0 }`)
- Runtime behavior:
  - skipped when factor `<= 0`
  - final fire time = `round(seconds * scaleSeconds.eval(value) * factor)`, clamped to `0..600`
  - applies only if final fire time `> 0`

`attribute` action
- Required fields:
  - `type` = `"attribute"`
  - `attribute` (resource location)
  - `uuid` (string UUID, e.g. `"123e4567-e89b-12d3-a456-426614174000"`)
  - `name` (string)
  - `amount` (double)
  - `operation` (string, intended values: `add`, `multiply_base`, `multiply_total`)
  - `durationTicks` (int)
- Optional fields:
  - `scaleAmount` (Scaling, default `{ "type": "constant", "value": 1.0 }`)
- Runtime behavior:
  - currently not implemented (no-op placeholder)

`client_fx` action
- Required fields:
  - `type` = `"client_fx"`
  - `fxId` (string)
- Optional fields:
  - `intensity` (Scaling, default `{ "type": "constant", "value": 1.0 }`)
  - `durationTicks` (int, default `40`)
- Runtime behavior:
  - server-side:
    - only applies to server-side players
    - skipped when factor `<= 0`
    - computed intensity is `intensity.eval(value) * factor`, clamped to `>= 0`, must be `> 0` to send
    - `durationTicks` is clamped to `1..1200`
  - client-side:
    - intensity is clamped to `0.0..2.0`
    - duration is clamped to `1..1200`
    - reapplying same `fxId` refreshes effect using max intensity and max remaining duration
  - recognized `fxId` values:
    - `darken`, `blur`, `shake`, `shaking`, `warp`, `warping`
  - unknown `fxId` values are accepted but have no visible effect unless custom client code handles them
  - `geiger` is used by built-in data but has no dedicated visual/audio behavior in current `ClientFxManager`

`command` action
- Required fields:
  - `type` = `"command"`
  - `command` (string)
- Runtime behavior:
  - currently disabled for safety (no-op placeholder)

### 3.3 Scaling

Variants:
- `constant`
  - required fields: `type = "constant"`, `value` (double)
  - behavior: returns `value`
- `linear01`
  - required fields: `type = "linear01"`, `min` (double), `max` (double)
  - behavior: `clamp((v - min) / (max - min), 0..1)`
  - special case: if `max == min`, returns `1.0` when `v >= min`, else `0.0`
- `clamp`
  - required fields: `type = "clamp"`, `inner` (Scaling), `min` (double), `max` (double)
  - behavior: `clamp(inner.eval(v), min..max)`
- `power`
  - required fields: `type = "power"`, `inner` (Scaling), `exponent` (double)
  - behavior: `pow(inner.eval(v), exponent)`

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
- Reads a configured hazard type id from client radiation data: `geigerDisplayHazardType`.
- Dial full scale is `geigerMaxRadiation`.
- Position controlled by `geigerHudAnchor`, `geigerHudOffsetX`, `geigerHudOffsetY`.
- Plays looped Geiger audio while the HUD is visible:
1. No sound below `geigerSoundMediumMinRadiation`
2. `hazardous:geiger.mediumdose` loop at/above `geigerSoundMediumMinRadiation`
3. `hazardous:geiger.highdose` loop at/above `geigerSoundHighMinRadiation`
- Sound loops stop immediately when the Geiger HUD is no longer visible (not selected / unequipped).

Important:
- The geiger HUD does not depend on `client_fx`; it uses hazard values synced from server to client.
- Geiger audio uses the same displayed hazard value as the HUD dial.

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
- `geigerDisplayHazardType` (string hazard type resource location, default `hazardous:radioactive_source`, empty disables dial target)
- `geigerMaxRadiation` (double `0.0001..1000000.0`, default `100.0`)
- `geigerSoundMediumMinRadiation` (double `0.0..1000000.0`, default `1.0`; minimum radiation for the medium loop)
- `geigerSoundHighMinRadiation` (double `0.0..1000000.0`, default `25.0`; minimum radiation for the high loop, clamped to be at least the medium threshold)
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
geigerDisplayHazardType = "hazardous:radioactive_source"
geigerMaxRadiation = 100.0
geigerSoundMediumMinRadiation = 1.0
geigerSoundHighMinRadiation = 25.0
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
