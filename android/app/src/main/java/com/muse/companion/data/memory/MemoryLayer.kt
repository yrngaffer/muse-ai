package com.muse.companion.data.memory

/** The three distinct memory layers (see docs/02 §4). */
enum class MemoryLayer { PERMANENT, EPISODIC, ADAPTIVE }

/** Distinguishes user-confirmed facts from assumptions and model suggestions. */
enum class VerificationStatus { USER_CONFIRMED, ASSUMED, MODEL_SUGGESTED }

/** Drives whether content is encrypted at rest and handled with extra care. */
enum class Sensitivity { NORMAL, SENSITIVE }
