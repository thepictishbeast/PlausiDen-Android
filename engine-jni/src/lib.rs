//! JNI bridge for the PlausiDen engine.
//!
//! Exports functions matching the Kotlin `NativeBridge` declarations.
//! Each function has the correct JNI signature and returns stub data
//! until the real engine crate is integrated.

use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jint, JNI_TRUE};
use jni::JNIEnv;
use serde::Serialize;

/// Result of artifact generation, returned as JSON to Kotlin.
#[derive(Serialize)]
struct ArtifactResult {
    category: String,
    count: i32,
    risk_level: String,
    artifacts: Vec<ArtifactEntry>,
    status: String,
}

/// A single generated artifact.
#[derive(Serialize)]
struct ArtifactEntry {
    artifact_type: String,
    description: String,
    timestamp: u64,
}

/// Engine status information.
#[derive(Serialize)]
struct EngineStatus {
    running: bool,
    version: String,
    uptime_seconds: u64,
    total_generated: u64,
    active_generators: Vec<String>,
}

/// Description of an available artifact generator.
#[derive(Serialize)]
struct GeneratorInfo {
    name: String,
    description: String,
    category: String,
    risk_levels: Vec<String>,
}

// ---------------------------------------------------------------------------
// JNI exports — fully qualified names match com.plausiden.app.NativeBridge
// ---------------------------------------------------------------------------

/// Generate artifacts for a given category.
///
/// # Safety
/// Called from JNI; `env`, `_class`, `category`, and `risk_level` must be valid.
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_plausiden_app_NativeBridge_generateArtifacts<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    category: JString<'local>,
    count: jint,
    risk_level: JString<'local>,
) -> JString<'local> {
    let category_str: String = env
        .get_string(&category)
        .map(|s| s.into())
        .unwrap_or_default();
    let risk_str: String = env
        .get_string(&risk_level)
        .map(|s| s.into())
        .unwrap_or_default();

    let now = std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .unwrap_or_default()
        .as_secs();

    // Stub: create plausible artifact entries
    let artifacts: Vec<ArtifactEntry> = (0..count)
        .map(|i| ArtifactEntry {
            artifact_type: format!("{}_artifact", category_str),
            description: format!(
                "Generated {} artifact #{} at risk level {}",
                category_str,
                i + 1,
                risk_str
            ),
            timestamp: now + i as u64,
        })
        .collect();

    let result = ArtifactResult {
        category: category_str,
        count,
        risk_level: risk_str,
        artifacts,
        status: "ok".to_string(),
    };

    let json = serde_json::to_string(&result).unwrap_or_else(|e| {
        format!(r#"{{"status":"error","message":"{}"}}"#, e)
    });

    env.new_string(&json)
        .unwrap_or_else(|_| env.new_string("").expect("failed to create empty JNI string"))
}

/// Get the current engine status.
///
/// # Safety
/// Called from JNI.
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_plausiden_app_NativeBridge_getEngineStatus<'local>(
    jni_env: JNIEnv<'local>,
    _class: JClass<'local>,
) -> JString<'local> {
    let status = EngineStatus {
        running: true,
        version: env!("CARGO_PKG_VERSION").to_string(),
        uptime_seconds: 0, // TODO: track real uptime
        total_generated: 0,
        active_generators: vec![
            "browser".to_string(),
            "filesystem".to_string(),
            "contacts".to_string(),
            "media".to_string(),
            "clipboard".to_string(),
        ],
    };

    let json = serde_json::to_string(&status).unwrap_or_else(|e| {
        format!(r#"{{"status":"error","message":"{}"}}"#, e)
    });

    jni_env.new_string(&json)
        .unwrap_or_else(|_| jni_env.new_string("").expect("failed to create empty JNI string"))
}

/// Configure the engine with a user profile.
///
/// # Safety
/// Called from JNI; `profile_json` must be a valid JNI string.
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_plausiden_app_NativeBridge_configureProfile<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    profile_json: JString<'local>,
) -> jboolean {
    let profile_str: String = env
        .get_string(&profile_json)
        .map(|s| s.into())
        .unwrap_or_default();

    // Validate that the profile JSON is parseable
    match serde_json::from_str::<serde_json::Value>(&profile_str) {
        Ok(_value) => {
            tracing::info!("Engine profile configured successfully");
            JNI_TRUE
        }
        Err(e) => {
            tracing::error!("Failed to parse profile JSON: {}", e);
            0 // JNI_FALSE
        }
    }
}

/// Get the list of available artifact generators.
///
/// # Safety
/// Called from JNI.
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_plausiden_app_NativeBridge_getGeneratorList<'local>(
    jni_env: JNIEnv<'local>,
    _class: JClass<'local>,
) -> JString<'local> {
    let generators = vec![
        GeneratorInfo {
            name: "browser_history".to_string(),
            description: "Generates plausible browser history entries".to_string(),
            category: "browser".to_string(),
            risk_levels: vec!["low".into(), "medium".into(), "high".into()],
        },
        GeneratorInfo {
            name: "filesystem_entries".to_string(),
            description: "Creates plausible files and directory structures".to_string(),
            category: "filesystem".to_string(),
            risk_levels: vec!["low".into(), "medium".into(), "high".into()],
        },
        GeneratorInfo {
            name: "contact_records".to_string(),
            description: "Generates plausible contact entries".to_string(),
            category: "contacts".to_string(),
            risk_levels: vec!["low".into(), "medium".into()],
        },
        GeneratorInfo {
            name: "media_files".to_string(),
            description: "Creates plausible media file metadata".to_string(),
            category: "media".to_string(),
            risk_levels: vec!["low".into(), "medium".into(), "high".into()],
        },
        GeneratorInfo {
            name: "clipboard_entries".to_string(),
            description: "Generates plausible clipboard history".to_string(),
            category: "clipboard".to_string(),
            risk_levels: vec!["low".into()],
        },
    ];

    let json = serde_json::to_string(&generators).unwrap_or_else(|e| {
        format!(r#"[{{"status":"error","message":"{}"}}]"#, e)
    });

    jni_env.new_string(&json)
        .unwrap_or_else(|_| jni_env.new_string("[]").expect("failed to create empty JNI string"))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn artifact_result_serializes() {
        let result = ArtifactResult {
            category: "browser".to_string(),
            count: 2,
            risk_level: "medium".to_string(),
            artifacts: vec![
                ArtifactEntry {
                    artifact_type: "browser_artifact".to_string(),
                    description: "test".to_string(),
                    timestamp: 1000,
                },
            ],
            status: "ok".to_string(),
        };
        let json = serde_json::to_string(&result).unwrap();
        assert!(json.contains("browser"));
        assert!(json.contains("medium"));
    }

    #[test]
    fn engine_status_serializes() {
        let status = EngineStatus {
            running: true,
            version: "0.1.0".to_string(),
            uptime_seconds: 42,
            total_generated: 100,
            active_generators: vec!["browser".to_string()],
        };
        let json = serde_json::to_string(&status).unwrap();
        assert!(json.contains("running"));
        assert!(json.contains("0.1.0"));
    }

    #[test]
    fn generator_info_serializes() {
        let info = GeneratorInfo {
            name: "test_generator".to_string(),
            description: "A test".to_string(),
            category: "browser".to_string(),
            risk_levels: vec!["low".to_string()],
        };
        let json = serde_json::to_string(&info).unwrap();
        assert!(json.contains("test_generator"));
    }
}
