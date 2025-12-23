# Hazardous Datapack Guide

This document explains how to add and configure hazards via datapacks. It focuses on the JSON formats for:
- **Hazard types**: define what a hazard is and how it behaves (intensity, falloff, blocking, exposure, and effects).
- **Hazard sources**: tell the game where a hazard exists (dimension, biome, block/tag, entity type, etc.) and which hazard type to use.
- **Effect entries**: define what happens once the current exposure value crosses a threshold or range.

These are regular data-pack registries exposed by the mod and loaded from your datapack.

- Where to place these objects in the datapack:
  - Hazard types: data/<namespace>/hazardous/hazardtypes/*.json
  - Hazard sources: data/<namespace>/hazardous/hazardsources/*.json
  - Effect entries: data/<namespace>/hazardous/effectentries/*.json

Tip: HazardTypes reference EffectEntry ids (resource locations) defined in the effect entry registry. The game validates that sources reference existing hazard types and that the type’s transmission supports the association used by the source.

How it fits together (mental model):
1) **Sources** decide when a hazard applies and produce a raw intensity.
2) **Transmission + falloff** turn distance and weather/LOS rules into a final intensity.
3) **Exposure** accumulates or decays the value over time.
4) **Effect entries** read that exposure value and trigger actions.


## 1) HazardType JSON
A HazardType defines how a given hazard behaves (how it is transmitted, how it falls off with distance, how blocks may block it, how exposure accumulates/decays, and which effects can happen).

Codec: mcjty.hazardous.data.objects.HazardType.CODEC

Top-level object fields:
- transmission: one of
  - Sky
  - Point
  - Contact
- falloff: one of
  - none
  - inverse_square
  - linear
  - exponential
- blocking: one of
  - none
  - simple
  - absorption
- exposure: describes timing and accumulation behavior
- effects: array of EffectEntry ids (resource locations, optional, defaults to empty array)

### 1.1 Transmission
Select with the field "type". Available variants:

- Sky
  - type: "sky"
  - baseIntensity: double (baseline intensity before modifiers)
  - requiresDirectSky: boolean (if true, must see the sky; otherwise indoorLeak applies)
  - rainMultiplier: double (extra multiplier while raining)
  - thunderMultiplier: double (extra multiplier while thundering)
  - nightMultiplier: double (extra multiplier at night; 0 for no sun)
  - indoorLeak: double (0..1), applied if requiresDirectSky is true but the player cannot see the sky
  - Supported associations for sources: level, biome, city

- Point
  - type: "point"
  - baseIntensity: double
  - maxDistance: int (hard cutoff distance)
  - requiresLineOfSight: boolean (whether LOS matters)
  - airAttenuationPerBlock: double (extra exponential attenuation per block, often 0)
  - Supported associations for sources: locations, entity_type, block

- Contact
  - type: "contact"
  - baseIntensity: double
  - Supported associations for sources: entity_type, block

### 1.2 Falloff
- none
  - type: "none" (no change with distance)

- inverse_square
  - type: "inverse_square"
  - minDistance: double (clamp denominator to avoid infinity)

- linear
  - type: "linear"
  - No extra fields. Effective intensity becomes base * max(0, 1 - d/maxDistance)

- exponential
  - type: "exponential"
  - k: double (intensity *= exp(-k * d))

Note: maxDistance is supplied by the Point transmission, not by the falloff itself.

### 1.3 Blocking
- none
  - type: "none"

- simple
  - type: "simple"
  - solidBlockMultiplier: double (multiply intensity per solid block crossed, e.g., 0.6)
  - fluidMultiplier: double (multiply intensity per fluid block crossed, e.g., water 0.3)
  - treatLeavesAsSolid: boolean

- absorption (future/advanced)
  - type: "absorption"
  - absorptionRegistryHint: resource location (registry for absorbers/materials)
  - defaultAbsorption: double

### 1.4 Exposure
Controls how often exposure is applied, whether it accumulates, and decay.

Fields:
- applyIntervalTicks: int (e.g., 20 = once/second)
- accumulate: boolean (if true, keep a per-player dose buffer)
- exponential: boolean (if true and maximum > 0, growth is slower as you approach the cap)
- maximum: double (<= 0 means uncapped)
- decayPerTick: double (decay applied each evaluation; 0 means no decay)

### 1.5 Example HazardType (Solar burn)
File: data/example/hazardous/hazardtypes/solar.json

{
  "transmission": {
    "type": "sky",
    "baseIntensity": 0.02,
    "requiresDirectSky": true,
    "rainMultiplier": 0.6,
    "thunderMultiplier": 0.3,
    "nightMultiplier": 0.0,
    "indoorLeak": 0.1
  },
  "falloff": { "type": "none" },
  "blocking": {
    "type": "simple",
    "solidBlockMultiplier": 0.5,
    "fluidMultiplier": 0.5,
    "treatLeavesAsSolid": true
  },
  "exposure": {
    "applyIntervalTicks": 20,
    "accumulate": true,
    "exponential": true,
    "maximum": 10.0,
    "decayPerTick": 0.0
  },
  "effects": [
    "example:solar_wither"
  ]
}


## 2) HazardSource JSON
A HazardSource tells the game where a hazard appears and which HazardType to use.
Think of this as the "where" and "which" part; the HazardType is the "how".

Codec: mcjty.hazardous.data.objects.HazardSource.CODEC

Top-level object fields:
- hazardType: resource location (must exist in the hazard type registry)
- association: where the hazard is attached; one of:
  - level
  - entity_type
  - locations
  - biome
  - city (Lost Cities compat; only active if the mod is present)
  - block

Select association with the field "type" and provide fields for that variant:

- level
  - type: "level"
  - level: resource location (dimension id, e.g., "minecraft:overworld")
  - Supported transmissions: Sky

- entity_type
  - type: "entity_type"
  - entityType: resource location (e.g., "minecraft:zombie")
  - maxDistance: double (search radius around the player for matching entities; required)
  - Supported transmissions: Point, Contact

- locations
  - type: "locations"
  - level: resource location (dimension id)
  - positions: array of block positions: {"x": int, "y": int, "z": int}
  - Supported transmissions: Point

- biome
  - type: "biome"
  - biome: resource location (e.g., "minecraft:desert")
  - Supported transmissions: Sky

- city
  - type: "city"
  - No further fields
  - Supported transmissions: Sky

- block
  - type: "block"
  - block: resource location (single block id, e.g., "minecraft:coal_ore")
  - tag: resource location (block tag id, e.g., "minecraft:logs")
  - maxDistance: double (search radius around the player for matching blocks; required)
  - Use exactly one of block or tag
  - Supported transmissions: Point, Contact

Validation: On datapack reload the mod checks that the chosen HazardType.transmission supports the association you used here. If not, the datapack load will fail with an error.
Note: For point hazards there are two distance knobs: association.maxDistance limits how far the game searches for matching entities/blocks, while transmission.maxDistance limits how far the intensity can reach (falloff still applies).

### 2.1 Examples
- Entire overworld has mild solar burn:

{
  "hazardType": "example:solar",
  "association": {
    "type": "level",
    "level": "minecraft:overworld"
  }
}

- Radiation hotspot at locations:

{
  "hazardType": "example:radiation_point",
  "association": {
    "type": "locations",
    "level": "minecraft:overworld",
    "positions": [ {"x": 100, "y": 64, "z": 100}, {"x": 140, "y": 20, "z": -30} ]
  }
}

- Hazard attached to all zombies (contact type):

{
  "hazardType": "example:zombie_touch",
  "association": {
    "type": "entity_type",
    "entityType": "minecraft:zombie",
    "maxDistance": 16.0
  }
}

- Hazard from nearby uranium ore blocks (point type):

{
  "hazardType": "example:uranium_point",
  "association": {
    "type": "block",
    "block": "example:uranium_ore",
    "maxDistance": 12.0
  }
}


## 3) EffectEntry JSON
An EffectEntry defines a rule: when to trigger (Trigger) and what to do (Action). These are registered separately as data/<namespace>/hazardous/effectentries/*.json and referenced by HazardType.effects.

Codec: mcjty.hazardous.data.objects.EffectEntry.CODEC

Top-level object fields:
- trigger: Trigger JSON (see below)
- action: Action JSON (see below)

### 3.1 Trigger
Select with field "type". Variants:

- threshold
  - type: "threshold"
  - min: double (fires when value >= min)
  - hysteresis: double (optional, default 0.0)

- range
  - type: "range"
  - min: double
  - max: double (fires when min <= value <= max)

- probability
  - type: "probability"
  - scaling: Scaling (maps value -> chance in 0..1; clamped)

### 3.2 Action
Select with field "type". Variants:

- potion
  - type: "potion"
  - effect: resource location (MobEffect id)
  - durationTicks: int
  - amplifier: int (base; can be scaled)
  - ambient: boolean (optional, default false)
  - showParticles: boolean (optional, default true)
  - showIcon: boolean (optional, default true)
  - intensityToAmplifier: Scaling (optional; default constant 1.0)

- damage
  - type: "damage"
  - damageType: resource location (1.20+ DamageType; common fallbacks: "minecraft:magic", "minecraft:wither", "minecraft:on_fire")
  - amount: double
  - scaleAmount: Scaling (optional; default constant 1.0)

- fire
  - type: "fire"
  - seconds: int (base)
  - scaleSeconds: Scaling (optional; default constant 1.0)

- attribute (placeholder; currently no-op)
  - type: "attribute"
  - attribute: resource location
  - uuid: string (UUID)
  - name: string
  - amount: double
  - operation: string ("add", "multiply_base", "multiply_total")
  - durationTicks: int
  - scaleAmount: Scaling (optional; default constant 1.0)

- client_fx (client-side visuals; placeholder hooks)
  - type: "client_fx"
  - fxId: string
  - intensity: Scaling (optional; default constant 1.0)
  - durationTicks: int (optional; default 40)

- command (server-side; disabled placeholder/no-op for safety)
  - type: "command"
  - command: string

### 3.3 Scaling
Several places use a Scaling object to turn the current exposure value into either a chance or a multiplier. Select with field "type".

Variants:
- constant
  - type: "constant"
  - value: double

- linear01
  - type: "linear01"
  - min: double
  - max: double (returns normalized (value-min)/(max-min), clamped 0..1)

- clamp
  - type: "clamp"
  - inner: Scaling
  - min: double
  - max: double (clamps inner result to [min, max])

- power
  - type: "power"
  - inner: Scaling
  - exponent: double (returns clamp(inner,0..inf)^exponent)


## 4) Putting it together (radiation examples)

### 4.1 Point radiation with linear falloff
- HazardType: data/example/hazardous/hazardtypes/radiation_point.json

{
  "transmission": {
    "type": "point",
    "baseIntensity": 1.0,
    "maxDistance": 16,
    "requiresLineOfSight": true,
    "airAttenuationPerBlock": 0.0
  },
  "falloff": { "type": "linear" },
  "blocking": { "type": "simple", "solidBlockMultiplier": 0.6, "fluidMultiplier": 0.3, "treatLeavesAsSolid": true },
  "exposure": { "applyIntervalTicks": 20, "accumulate": true, "exponential": true, "maximum": 5.0, "decayPerTick": 0.0 },
  "effects": [
    "example:radiation_damage",
    "example:radiation_geiger"
  ]
}

- HazardSource: data/example/hazardous/hazardsources/radiation_spots.json

{
  "hazardType": "example:radiation_point",
  "association": { "type": "locations", "level": "minecraft:overworld", "positions": [ {"x":0, "y":64, "z":0} ] }
}

### 4.2 City skylight hazard (Lost Cities)
- HazardType: data/example/hazardous/hazardtypes/city_scorch.json

{
  "transmission": {
    "type": "sky",
    "baseIntensity": 0.05,
    "requiresDirectSky": true,
    "rainMultiplier": 0.4,
    "thunderMultiplier": 0.2,
    "nightMultiplier": 0.0,
    "indoorLeak": 0.05
  },
  "falloff": { "type": "none" },
  "blocking": { "type": "none" },
  "exposure": { "applyIntervalTicks": 20, "accumulate": true, "exponential": true, "maximum": 10.0, "decayPerTick": 0.0 },
  "effects": [
    "example:city_scorch_fire"
  ]
}

- HazardSource (active in cities only): data/example/hazardous/hazardsources/city_scorch.json

{
  "hazardType": "example:city_scorch",
  "association": { "type": "city" }
}


## 5) Troubleshooting
- Datapack reload fails: check the logs for messages like "HazardSource refers to missing HazardType" or "Incompatible hazard source". Make sure hazardType ids are correct and that the HazardType.transmission supports the association you chose.
- Nothing seems to happen: verify exposure.applyIntervalTicks, your trigger thresholds/ranges, and that your HazardSource actually matches the player’s dimension/biome/city/position/entity type at runtime.
- LOS and blocking are currently simplified. The simple blocking model multiplies intensity per solid/fluid block crossed when a later ray/occlusion model is used; Point.requiresLineOfSight should be set accordingly for future-proofing.

## 6) Version notes
- This doc matches the JSON that the current CODEC classes accept:
  - HazardType, HazardSource, EffectEntry, Trigger, Action, Scaling, Falloff, Blocking, Exposure
- Some actions (attribute, command, client_fx) are placeholders or server-safe no-ops for now. They are included for forward compatibility and may gain functionality later.
