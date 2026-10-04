package com.prove2break.apprestriction

/** Outcome of checking whether the currently-foregrounded app should be intercepted. */
sealed class RestrictionDecision {
    data object Allow : RestrictionDecision()
    data class Block(val packageName: String) : RestrictionDecision()
}
