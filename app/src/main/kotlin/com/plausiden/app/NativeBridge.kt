package com.plausiden.app

/**
 * JNI bridge to the Rust plausiden-engine.
 *
 * Loads the `plausiden_engine_jni` shared library and declares external functions
 * that map to Rust JNI exports.
 */
object NativeBridge {

    init {
        System.loadLibrary("plausiden_engine_jni")
    }

    /**
     * Generate artifacts for the given category.
     *
     * @param category One of: "browser", "filesystem", "contacts", "media", "clipboard"
     * @param count Number of artifacts to generate in this batch
     * @param riskLevel One of: "low", "medium", "high", "maximum"
     * @return JSON string describing the generated artifacts
     */
    external fun generateArtifacts(category: String, count: Int, riskLevel: String): String

    /**
     * Get the current status of the Rust engine.
     *
     * @return JSON string with engine status information
     */
    external fun getEngineStatus(): String

    /**
     * Configure the engine with a user profile.
     *
     * @param profileJson JSON string with profile configuration
     * @return true if configuration was accepted
     */
    external fun configureProfile(profileJson: String): Boolean

    /**
     * Get the list of available artifact generators.
     *
     * @return JSON string with generator names and descriptions
     */
    external fun getGeneratorList(): String
}
