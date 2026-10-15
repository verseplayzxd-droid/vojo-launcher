package net.kdt.pojavlaunch.auth

/** M5 must wire VerifiedLaunchSession into launch explicitly. Old account files
 * and callbacks cannot confer ownership or silently enable demo/offline launch. */
object LegacyLaunchBoundary {
    @JvmStatic fun rejectLegacyLaunch() {
        throw SecurityException("Legacy account, demo and offline launch paths are disabled. Verified Vojo launch integration arrives in Milestone 5.")
    }
}
