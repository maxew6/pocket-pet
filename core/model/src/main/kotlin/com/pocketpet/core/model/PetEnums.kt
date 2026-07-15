package com.pocketpet.core.model

/**
 * Every physical behavior the pet can be performing right now.
 *
 * This is intentionally a flat enum rather than a class hierarchy: the behavior engine treats
 * state transitions as data (see `PetBehaviorEngine`), and a flat enum keeps those transition
 * tables trivial to read, test, and extend.
 */
enum class PetState {
    Idle,
    Walking,
    Running,
    Sleeping,
    Stretching,
    Jumping,
    LookingAround,
    FollowingFinger,
    BeingDragged,
    Sitting,
    Rolling,
    Grooming,
    Yawning,
    Eating,
    Crying,
    Hiding,
    HappyDance,
    WatchingNotification,
    WatchingCharging,
    Recovering,
}

/** The pet's current emotional coloring, derived from needs, battery, and recent events. */
enum class Mood {
    Happy,
    Hungry,
    Sleepy,
    Excited,
    Lonely,
    Lazy,
    Curious,
    Concerned,
    Scared,
    Content,
}

/** Cream-family body tones a person can pick for the default cat species. */
enum class ColorTone {
    Milk,
    Ivory,
    Honey,
    Caramel,
    Blush,
    Charcoal,
}

/** Optional cosmetic accessories layered on top of the rendered pet. */
enum class Accessory {
    None,
    Hat,
    Glasses,
    Scarf,
}

/** Light/dark/system app theme selection. */
enum class AppTheme {
    Light,
    Dark,
    System,
}

/**
 * The battery-percentage bands the pet reacts to. Kept as an ordered enum (rather than raw
 * percentages scattered through the code) so "did we cross a milestone" is a single comparison
 * and each milestone fires its reaction exactly once per crossing — see
 * `BatteryReactionTracker`.
 */
enum class BatteryMilestone(val thresholdPercent: Int) {
    Full(100),
    High(80),
    Normal(50),
    Low(30),
    Critical(20),
    Emergency(10),
}
