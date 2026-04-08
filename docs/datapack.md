# Hazardous Datapack Guide

Hazardous is a highly configurable hazard framework for modpacks. You can use it to define custom environmental dangers such as radiation, solar burn, heat, contaminated items, or area-based hazards, and then decide how players detect, resist, or recover from them.

For modpack developers, the main value is flexibility:
- hazards are defined with datapacks instead of hardcoded logic
- hazard types, sources, and effects can be mixed and matched
- server config decides which built-in or custom hazards are actually enabled
- player-facing tools like the gas mask, Geiger counter, dosimeter, pills, and anti-rad pills can be retargeted through config

Optional integrations:
- Curios: gas masks work in the `head` Curios slot, and the Geiger counter and dosimeter can also be equipped as Curios
- Lost Cities: hazard sources can target cities, city styles, buildings, and multibuildings

Included gameplay items:
- `hazardous:gasmask`
- `hazardous:filter`
- `hazardous:geiger_counter`
- `hazardous:dosimeter`
- `hazardous:pills`
- `hazardous:resistance_pills`

## 0) Config Setup First (Required)

`hazardous-server.toml` defaults to:

```toml
enabledHazardTypes = []
enabledHazardSources = []
```

That means no hazards run until you opt in.

Built-in hazard type ids you can enable:
- `hazardous:solar_burn`
- `hazardous:radioactive_type`
- `hazardous:lostcity_radiation`
- `hazardous:lava_heat`

Built-in hazard source ids you can enable:
- `hazardous:overworld_solar` -> uses `hazardous:solar_burn`
- `hazardous:radioactive_zombie` -> uses `hazardous:radioactive_type`
- `hazardous:lostcity_buildings` -> uses `hazardous:lostcity_radiation`
- `hazardous:lava_bucket` -> uses `hazardous:lava_heat`
- `hazardous:dropped_lava_bucket` -> uses `hazardous:lava_heat`
- `hazardous:near_lava` -> uses `hazardous:lava_heat`

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
2. A `hazardsource` also says how the hazard transmits (`sky`, `point`, `contact`) and how it falls off with distance.
3. A `hazardtype` says blocking, exposure, and effects.
4. `effectentries` say what happens when exposure or dose reaches certain values.
5. Items and config (gas mask, pills, Geiger counter, dosimeter) modify or visualize runtime behavior.

## 1) HazardType JSON

Top-level fields:
- `blocking`: `none`, `simple`, or `absorption`
- `exposure`: timing plus accumulation behavior
- `resistanceAttribute`: optional attribute id used as natural resistance for this hazard type
- `effects`: list of effect entry ids (optional, defaults to `[]`)

### 1.1 Blocking

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
- `absorptionRegistryHint` is currently required by the JSON format, but current Hazardous runtime logic does not use it for calculations. Treat it as metadata for the absorption setup rather than a gameplay control.

### 1.2 Exposure

- `applyIntervalTicks` (int): evaluation interval (`20` = once per second)
- `accumulate` (boolean): whether to keep per-player dose
- `exponential` (boolean): if capped, accumulation slows near max
- `maximum` (double): `<= 0` means uncapped
- `decayPerTick` (double): decay applied at each evaluation

### 1.3 Example HazardType (point radiation shared settings)

`data/example/hazardous/hazardtypes/radiation_point.json`

```json
{
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
  "resistanceAttribute": "hazardous:radioactive_type_resistance",
  "effects": [
    "example:radiation_damage"
  ]
}
```

### 1.4 Resistance Attributes

- `resistanceAttribute` is optional.
- The attribute value is treated as a `0.0..1.0` resistance multiplier.
- A player with `0.25` resistance for a hazard type only receives `75%` of that hazard's incoming exposure.
- Hazard resistance is applied after source calculation and gas mask protection, before personal dose is accumulated.
- Built-in hazard types ship with matching built-in attributes:
  - `hazardous:solar_burn_resistance`
  - `hazardous:radioactive_type_resistance`
  - `hazardous:lostcity_radiation_resistance`
  - `hazardous:lava_heat_resistance`
- These built-in attributes exist on players and can be modified by commands, equipment, effects, or other mods.
- Custom hazard types should set `resistanceAttribute` to a registered attribute id if they want resistance support.
- If a hazard type points at a missing attribute id, datapack validation fails on reload.

## 2) HazardSource JSON

Top-level fields:
- `hazardType`: resource location
- `falloff`: `none`, `inverse_square`, `linear`, or `exponential`
- `transmission`: `sky`, `point`, or `contact`
- `association`: where this hazard exists

### 2.1 Falloff

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
- runtime note: the cutoff still comes from `transmission.maxDistance` when that transmission uses distance

Runtime note:
- `falloff` is used by `point` transmission.
- `falloff` is also used by `city + sky` to shape exposure around matching building or multibuilding centers.
- Other `sky` sources and all `contact` sources can still declare a falloff, but it is ignored in runtime calculations.

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
- valid with associations: `locations`, `entity_type`, `block`, `item`, `city`

`contact`
- `type: "contact"`
- `baseIntensity` (double)
- valid with associations: `entity_type`, `block`, `item`

Association variants:

`level`
- `type: "level"`
- `level`: dimension id

`entity_type`
- `type: "entity_type"`
- `entityTypes`: list of entity ids
- `stacks` (optional): item stack predicates, only used for matching `minecraft:item` entities
- runtime note: with `point` transmission, search radius comes from `transmission.maxDistance`

`locations`
- `type: "locations"`
- `level`
- `positions`: list of `{ "x": ..., "y": ..., "z": ... }`

`biome`
- `type: "biome"`
- `biome`: biome matcher object
  - `if_all` (optional): list of biome selectors, all must match
  - `if_any` (optional): list of biome selectors, at least one must match
  - `excluding` (optional): list of biome selectors, none may match
  - biome selectors support biome ids and biome tags (`#namespace:tag`)

`city`
- `type: "city"`
- `style` (optional string): only matches Lost Cities chunks whose `ILostCityInfo#getCityStyle()` equals this value
- `buildings` (optional list of strings): only matches Lost Cities chunks whose `ILostChunkInfo#getBuildingId().toString()` is in this list
- `multibuildings` (optional list of strings): only matches Lost Cities chunks whose `ILostChunkInfo#getMultiBuildingInfo().buildingType().toString()` is in this list
- if multiple optional filters are given, all of them must match
- only works when Lost Cities is installed
- runtime note: `city + sky + falloff = none` starts as soon as the player enters a matching city/building chunk
- runtime note: `city + sky + falloff != none` searches nearby matching buildings or multibuildings in the current city and uses each center as a source position
- runtime note: `city + sky + falloff != none` can also detect nearby matching buildings just outside the city boundary
- runtime note: `city + point` treats matching buildings or multibuildings as point sources centered on their footprint
- runtime note: `city + point` searches around the player using `transmission.maxDistance`, so nearby matching buildings can contribute even when the player is outside the city boundary
- runtime note: `city + point` requires `buildings` or `multibuildings`; plain city-wide point sources are not supported
- runtime note: center-based `city + sky` behavior needs `buildings` or `multibuildings`; if both are empty, Hazardous logs a warning and falls back to city-wide behavior

`block`
- `type: "block"`
- exactly one of `block` or `tag`
- runtime note: with `point` transmission, search radius comes from `transmission.maxDistance`

`item`
- `type: "item"`
- `stacks`: list of item stack predicates
- runtime note: with `point` transmission, search radius comes from `transmission.maxDistance`
- each stack predicate matches any carried stack in main inventory, hotbar, offhand, or armor
- if any predicate matches, that player becomes the hazard source
- stack predicate fields:
  - exactly one of `item` or `tag`
  - `count` (optional int, default `1`): minimum stack size
  - `nbt` (optional string): SNBT that must match the stack NBT

Validation note:
- Reload validates association and transmission compatibility.
- If they do not match, the datapack is rejected.

### 2.2 Source examples

Entire overworld gets solar hazard:

```json
{
  "hazardType": "example:solar_burn",
  "falloff": {
    "type": "none"
  },
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

Configured entity types act as radioactive point sources:

```json
{
  "hazardType": "example:radioactive_type",
  "falloff": {
    "type": "exponential",
    "k": 0.18
  },
  "transmission": {
    "type": "point",
    "baseIntensity": 1.0,
    "maxDistance": 12,
    "requiresLineOfSight": true,
    "airAttenuationPerBlock": 0.05
  },
  "association": {
    "type": "entity_type",
    "entityTypes": [
      "minecraft:zombie"
    ]
  }
}
```

Dropped lava bucket emits heat:

```json
{
  "hazardType": "example:lava_heat",
  "falloff": {
    "type": "linear"
  },
  "transmission": {
    "type": "point",
    "baseIntensity": 0.8,
    "maxDistance": 4,
    "requiresLineOfSight": false,
    "airAttenuationPerBlock": 0.0
  },
  "association": {
    "type": "entity_type",
    "entityTypes": [
      "minecraft:item"
    ],
    "stacks": [
      {
        "item": "minecraft:lava_bucket"
      }
    ]
  }
}
```

Near-lava heat hazard:

```json
{
  "hazardType": "example:lava_heat",
  "falloff": {
    "type": "linear"
  },
  "transmission": {
    "type": "point",
    "baseIntensity": 0.8,
    "maxDistance": 4,
    "requiresLineOfSight": false,
    "airAttenuationPerBlock": 0.0
  },
  "association": {
    "type": "block",
    "block": "minecraft:lava"
  }
}
```

Lava bucket carrier emits heat:

```json
{
  "hazardType": "example:lava_heat",
  "falloff": {
    "type": "linear"
  },
  "transmission": {
    "type": "point",
    "baseIntensity": 0.8,
    "maxDistance": 4,
    "requiresLineOfSight": false,
    "airAttenuationPerBlock": 0.0
  },
  "association": {
    "type": "item",
    "stacks": [
      {
        "item": "minecraft:lava_bucket"
      }
    ]
  }
}
```

This makes any player carrying a lava bucket act as a nearby heat source, including for themselves.

Biome-based sky hazard with biome matcher:

```json
{
  "hazardType": "example:solar_burn",
  "falloff": {
    "type": "none"
  },
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
    "type": "biome",
    "biome": {
      "if_any": [
        "#minecraft:is_overworld",
        "minecraft:desert"
      ],
      "excluding": [
        "#minecraft:is_forest"
      ]
    }
  }
}
```

Lost Cities sky hazard limited to one city style:

```json
{
  "hazardType": "example:lostcity_radiation",
  "falloff": {
    "type": "none"
  },
  "transmission": {
    "type": "sky",
    "baseIntensity": 0.03,
    "requiresDirectSky": false,
    "rainMultiplier": 1.0,
    "thunderMultiplier": 1.0,
    "nightMultiplier": 1.0,
    "indoorLeak": 1.0
  },
  "association": {
    "type": "city",
    "style": "ancient"
  }
}
```

Lost Cities sky hazard limited to one city style, a set of building types, and optionally a set of multi-building ids:

```json
{
  "hazardType": "example:lostcity_radiation",
  "falloff": {
    "type": "none"
  },
  "transmission": {
    "type": "sky",
    "baseIntensity": 0.03,
    "requiresDirectSky": false,
    "rainMultiplier": 1.0,
    "thunderMultiplier": 1.0,
    "nightMultiplier": 1.0,
    "indoorLeak": 1.0
  },
  "association": {
    "type": "city",
    "style": "ancient",
    "buildings": [
      "lostcities:library",
      "lostcities:apartment"
    ],
    "multibuildings": [
      "lostcities:station"
    ]
  }
}
```

Lost Cities point hazard emitted by matching multi-buildings:

```json
{
  "hazardType": "example:lostcity_radiation",
  "falloff": {
    "type": "exponential",
    "k": 0.18
  },
  "transmission": {
    "type": "point",
    "baseIntensity": 500.0,
    "maxDistance": 64,
    "requiresLineOfSight": false,
    "airAttenuationPerBlock": 0.0
  },
  "association": {
    "type": "city",
    "multibuildings": [
      "deceasedcraft:multi_arcfurnace"
    ]
  }
}
```

## 3) EffectEntry JSON

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
  - `operation` (string enum: `add`, `multiply_base`, `multiply_total`)
  - `durationTicks` (int)
- Optional fields:
  - `scaleAmount` (Scaling, default `{ "type": "constant", "value": 1.0 }`)
- Runtime behavior:
  - skipped when factor `<= 0`
  - skipped if the target attribute id is missing or not present on the player
  - final modifier amount = `amount * scaleAmount.eval(value) * factor`
  - skipped if the final modifier amount is `0`
  - `durationTicks` is clamped to a minimum of `1`
  - applies a transient timed attribute modifier using the configured `uuid`, `name`, `operation`, and final modifier amount
  - reapplying the same `uuid` on the same attribute replaces the existing timed modifier entry and refreshes its duration
  - active timed attribute modifiers persist in player capability data and are restored after reloads and death-copy capability transfer
  - timed attribute modifiers are separate from resistance pill tracking and do not appear in the resistance status HUD

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
    - `darken`: fills the screen with a black overlay
    - `lighten`: fills the screen with a light gray overlay
    - `blur`: draws `assets/hazardous/textures/gui/blur.png` over the screen, with overlay alpha scaled by intensity
    - `blurradial`: draws `assets/hazardous/textures/gui/blur_radial.png` over the screen, with overlay alpha scaled by intensity
    - `shake`, `shaking`: adds camera yaw/pitch jitter
    - `warp`, `warping`: adds camera roll/yaw/pitch distortion
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

This section documents item behavior and practical use. Crafting recipes are intentionally not listed here because many modpacks replace them, and in-game recipe viewers are the most reliable source for the current pack.

### 4.1 Gas Mask (`hazardous:gasmask`)

Behavior:
- Must be worn in the armor helmet slot or in the Curios `head` slot.
- Only protects one configured hazard type id (`gasmaskProtectedType`).
- Protection amount is `gasmaskProtectionLevel` (0.0 to 1.0).
- Each protection application consumes 1 durability.
- At 0 durability it stays equipped but no longer protects.

Datapack armor tag:
- Hazardous also checks the item tag `hazardous:protective_armor`.
- If a damageable armor item with that tag is equipped in its normal armor slot (`head`, `chest`, `legs`, or `feet`), it can provide the same protection as the gas mask.
- Tagged armor uses the same `gasmaskProtectedType` and `gasmaskProtectionLevel` config values as the gas mask.
- Each protection application consumes 1 normal armor durability and can break the armor item.
- If both a usable gas mask and tagged armor are equipped, the gas mask is used first.

Example datapack tag file:

`data/hazardous/tags/items/protective_armor.json`

```json
{
  "replace": false,
  "values": [
    "minecraft:iron_helmet",
    "minecraft:diamond_chestplate"
  ]
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
- Uses `pillsAttribute` to choose which resistance attribute bucket to heal.
- Removes `pillsDoseHeal` from each stored player dose entry whose hazard type resolves to that configured attribute.
- If there is no dose to remove, nothing is consumed.
- If the configured attribute id is empty, invalid, or unused by loaded hazard types, the item has no effect.

### 4.4 Anti-rad Pills (`hazardous:resistance_pills`)

Behavior:
- Right-click consumes the item and applies the temporary resistance effect.
- Uses `resistancePillsAttribute` to choose which player attribute to raise.
- Each use adds a temporary resistance bonus of `resistancePillsAmount` for `resistancePillsDurationTicks`.
- Multiple uses stack by amount, and each consumed pill expires independently.
- `resistancePillsMaxStacks` limits how many simultaneous stacks can be active for that configured attribute. `0` means unlimited.
- If the configured attribute id is empty, invalid, or not attached to the player, using the item has no resistance effect.

Typical usage:
1. Point `resistancePillsAttribute` at a hazard resistance attribute such as `hazardous:radioactive_type_resistance`
2. Use the item one or more times
3. Use `/haz resistances` to inspect the updated player values while the timer is active

### 4.5 Geiger Counter (`hazardous:geiger_counter`)

Behavior:
- Shows HUD dial when:
1. Held in selected hotbar slot, or
2. Equipped in Curios (if Curios is installed)
- Reads a configured hazard type id from client radiation data: `geigerDisplayHazardType`.
- When the server applies gas mask protection to that hazard type, the displayed client value reflects the reduced exposure after protection.
- Dial full scale is `geigerMaxRadiation`.
- Pointer color bands are controlled by `geigerMediumThresshold` and `geigerHighTresshold`.
- Position controlled by `geigerHudAnchor`, `geigerHudScale`, `geigerHudOffsetX`, `geigerHudOffsetY`.
- Plays looped Geiger audio while the HUD is visible:
1. No sound below `geigerSoundMediumMinRadiation`
2. `hazardous:geiger.mediumdose` loop at/above `geigerSoundMediumMinRadiation`
3. `hazardous:geiger.highdose` loop at/above `geigerSoundHighMinRadiation`
- Loop loudness is scaled by `geigerSoundVolume`.
- The needle adds animated jitter while radiation is present, controlled by `geigerNeedleJitterAngle` and `geigerNeedleJitterSpeed`.
- Sound loops stop immediately when the Geiger HUD is no longer visible.

Important:
- The Geiger HUD does not depend on `client_fx`; it uses hazard values synced from server to client.
- Geiger audio uses the same displayed hazard value as the HUD dial.

### 4.6 Dosimeter (`hazardous:dosimeter`)

Behavior:
- Shows HUD when:
1. Held in selected hotbar slot, or
2. Equipped in Curios (if Curios is installed)
- Uses synchronized player dose data (accumulated dose per enabled hazard type).
- Displayed value selection:
1. If `dosimeterDisplayResource` is set, shows that one hazard type dose.
2. If `dosimeterDisplayResource` is empty, shows the sum of all positive stored dose entries.
- Bar fill is `dose / dosimeterMaxDose`, clamped to `0..1`.
- Bar color thresholds:
1. Green below `dosimeterMediumDose`
2. Yellow/orange at/above `dosimeterMediumDose`
3. Red at/above `dosimeterHighDose` (internally clamped to be at least `dosimeterMediumDose`)
- Radiation icon shake starts at `dosimeterMediumDose`, using `dosimeterIconShakeMediumDistance`, and ramps up to `dosimeterIconShakeMaxDistance` by `dosimeterMaxDose`. Animation speed uses `dosimeterIconShakeSpeed`.
- Plays a looped `hazardous:dosimeter.beep` sound while the HUD is visible and the displayed dose is at or above `dosimeterMediumDose`.
- Position controlled by `dosimeterHudAnchor`, `dosimeterHudScale`, `dosimeterHudOffsetX`, `dosimeterHudOffsetY`.

### 4.7 Tooltip Feedback

Behavior:
- Items that match enabled `item` hazard sources can show a carried-emissions tooltip.
- Tagged protective armor items can also show a tooltip describing what hazard type they protect against and how much protection they provide.

## 5) Config Options (Server + Client)

This section documents config options besides `enabledHazardTypes` and `enabledHazardSources`.

Server config (`hazardous-server.toml`):
- `gasmaskProtectedType` (string resource location, default `hazardous:radioactive_type`)
- `gasmaskProtectionLevel` (double `0.0..1.0`, default `0.75`; used by both `hazardous:gasmask` and `hazardous:protective_armor` items)
- `gasmaskFilterRestore` (int `1..1000000`, default `250`)
- `pillsAttribute` (string attribute resource location, default `hazardous:radioactive_type_resistance`; empty disables pill healing)
- `pillsDoseHeal` (double `0.0..1000000.0`, default `20.0`; amount removed from each matching hazard dose entry)
- `resistancePillsAttribute` (string attribute resource location, default `hazardous:radioactive_type_resistance`; empty disables the bonus)
- `resistancePillsAmount` (double `0.0..1.0`, default `0.1`; temporary amount granted per use)
- `resistancePillsDurationTicks` (int `0..1000000`, default `12000`; duration of each temporary pill bonus in ticks)
- `resistancePillsMaxStacks` (int `0..1000000`, default `0`; maximum simultaneous active anti-rad pill stacks, `0` = unlimited)

Client config (`hazardous-client.toml`):
- `geigerDisplayHazardType` (string hazard type resource location, default `hazardous:radioactive_type`, empty disables dial target)
- `geigerMaxRadiation` (double `0.0001..1000000.0`, default `100.0`)
- `geigerMediumThresshold` (double `0.0..1000000.0`, default `33.3`; threshold where the pointer enters the yellow segment)
- `geigerHighTresshold` (double `0.0..1000000.0`, default `66.6`; threshold where the pointer enters the red segment)
- `geigerSoundMediumMinRadiation` (double `0.0..1000000.0`, default `1.0`; minimum radiation for the medium loop)
- `geigerSoundHighMinRadiation` (double `0.0..1000000.0`, default `25.0`; minimum radiation for the high loop, clamped to be at least the medium threshold)
- `geigerSoundVolume` (double `0.0..1.0`, default `0.8`; volume multiplier for geiger loops)
- `geigerNeedleJitterAngle` (double `0.0..10.0`, default `1.8`; maximum jitter angle in degrees for the geiger needle)
- `geigerNeedleJitterSpeed` (double `0.1..10.0`, default `1.1`; speed multiplier for geiger needle jitter animation)
- `geigerHudAnchor` (string: `top_left`, `top_center`, `top_right`, `center_left`, `center_right`, `bottom_left`, `bottom_center`, `bottom_right`; default `top_right`)
- `geigerHudScale` (double `0.1..10.0`, default `1.0`)
- `geigerHudOffsetX` (int `-5000..5000`, default `8`)
- `geigerHudOffsetY` (int `-5000..5000`, default `8`)
- `dosimeterDisplayResource` (string resource location from player dose data, default `hazardous:radioactive_type`; empty = sum of all dose entries)
- `dosimeterMaxDose` (double `0.0001..1000000.0`, default `20.0`; value treated as full bar)
- `dosimeterMediumDose` (double `0.0..1000000.0`, default `3.0`; yellow/orange threshold)
- `dosimeterHighDose` (double `0.0..1000000.0`, default `6.0`; red threshold, clamped to at least medium threshold)
- `dosimeterIconShakeMediumDistance` (double `0.0..10.0`, default `0.35`; icon shake offset once medium dose is reached)
- `dosimeterIconShakeMaxDistance` (double `0.0..10.0`, default `1.1`; icon shake offset once max dose is reached)
- `dosimeterIconShakeSpeed` (double `0.1..10.0`, default `0.9`; speed multiplier for icon shake animation)
- `dosimeterHudAnchor` (string: `top_left`, `top_center`, `top_right`, `center_left`, `center_right`, `bottom_left`, `bottom_center`, `bottom_right`; default `top_right`)
- `dosimeterHudScale` (double `0.1..10.0`, default `1.0`)
- `dosimeterHudOffsetX` (int `-5000..5000`, default `8`)
- `dosimeterHudOffsetY` (int `-5000..5000`, default `84`)
- `resistancePillsHudEnabled` (boolean, default `true`; show an anti-rad pill effect indicator while active)
- `resistancePillsHudAnchor` (string: `top_left`, `top_center`, `top_right`, `center_left`, `center_right`, `bottom_left`, `bottom_center`, `bottom_right`; default `top_left`)
- `resistancePillsHudScale` (double `0.1..10.0`, default `1.0`)
- `resistancePillsHudOffsetX` (int `-5000..5000`, default `8`)
- `resistancePillsHudOffsetY` (int `-5000..5000`, default `8`)
- `curiosHeadOverrideHelmetRender` (boolean, default `true`; when Curios is installed, a visible head curio with its own renderer can hide the normal helmet armor render)

Example:

```toml
# hazardous-server.toml
gasmaskProtectedType = "hazardous:radioactive_type"
gasmaskProtectionLevel = 0.75
gasmaskFilterRestore = 250
pillsAttribute = "hazardous:radioactive_type_resistance"
pillsDoseHeal = 20.0
resistancePillsAttribute = "hazardous:radioactive_type_resistance"
resistancePillsAmount = 0.1
resistancePillsDurationTicks = 12000
resistancePillsMaxStacks = 0

# hazardous-client.toml
geigerDisplayHazardType = "hazardous:radioactive_type"
geigerMaxRadiation = 100.0
geigerMediumThresshold = 33.3
geigerHighTresshold = 66.6
geigerSoundMediumMinRadiation = 1.0
geigerSoundHighMinRadiation = 25.0
geigerSoundVolume = 0.8
geigerNeedleJitterAngle = 1.8
geigerNeedleJitterSpeed = 1.1
geigerHudAnchor = "top_right"
geigerHudScale = 1.0
geigerHudOffsetX = 8
geigerHudOffsetY = 8
dosimeterDisplayResource = "hazardous:radioactive_type"
dosimeterMaxDose = 20.0
dosimeterMediumDose = 3.0
dosimeterHighDose = 6.0
dosimeterIconShakeMediumDistance = 0.35
dosimeterIconShakeMaxDistance = 1.1
dosimeterIconShakeSpeed = 0.9
dosimeterHudAnchor = "top_right"
dosimeterHudScale = 1.0
dosimeterHudOffsetX = 8
dosimeterHudOffsetY = 84
resistancePillsHudEnabled = true
resistancePillsHudAnchor = "top_left"
resistancePillsHudScale = 1.0
resistancePillsHudOffsetX = 8
resistancePillsHudOffsetY = 8
curiosHeadOverrideHelmetRender = true
```

## 6) Debugging and Testing

Useful commands:
- `/hazardous radiationhere`
- `/hazardous dose`
- `/hazardous resistances`
- `/hazardous resetdose`
- `/hazardous resetresistances`

Alias:
- `/haz radiationhere`
- `/haz dose`
- `/haz resistances`
- `/haz resetdose`
- `/haz resetresistances`

Command notes:
- `/hazardous resistances` lists the executing player's current resistance attributes for all loaded hazard types.
- Output includes the hazard type id, the resolved attribute id, and the player's current and base values.
- `/hazardous resetresistances [player]` sets loaded hazard resistance attribute base values back to `0.0`, removes active anti-rad pill bonuses from those attributes, and clears stored anti-rad pill timers without touching dose.

Suggested workflow:
1. Use `/haz radiationhere` where your source should apply.
2. Use `/haz dose` after waiting a few seconds to confirm accumulation.
3. Use `/haz resistances` to inspect current resistance attributes.
4. Eat anti-rad pills and run `/haz resistances` again to confirm the temporary attribute increase.
5. Use pills and run `/haz dose` again to confirm dose reduction.
6. Wear gas mask and compare `/haz dose` growth with and without mask.

## 7) Built-In Data (Reference)

Built-in ids:
- Hazard types: `hazardous:solar_burn`, `hazardous:radioactive_type`, `hazardous:lostcity_radiation`, `hazardous:lava_heat`
- Hazard sources:
  - `hazardous:overworld_solar` -> `hazardous:solar_burn`
  - `hazardous:radioactive_zombie` -> `hazardous:radioactive_type`
  - `hazardous:lostcity_buildings` -> `hazardous:lostcity_radiation`
  - `hazardous:lava_bucket` -> `hazardous:lava_heat`
  - `hazardous:dropped_lava_bucket` -> `hazardous:lava_heat`
  - `hazardous:near_lava` -> `hazardous:lava_heat`
- Effect entries:
  - `hazardous:solar_weakness`
  - `hazardous:solar_ignite`
  - `hazardous:solar_darken`
  - `hazardous:radiation_damage`
  - `hazardous:radiation_geiger`
  - `hazardous:radiation_shake`
  - `hazardous:radiation_warp`
  - `hazardous:lava_blur`
  - `hazardous:lava_fire_damage`

Use these as working templates when creating your own pack.
