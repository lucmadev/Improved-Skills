package org.lucma.ImprovedSkills.skill

/**
 * Represents a single skill parsed from skills.yml.
 *
 * YAML structure:
 *   <id>:
 *     name: "<display name>"
 *     description: "<description>"
 *     class: warrior|mage|assassin
 *     material: BUKKIT_MATERIAL
 *     prerequisites: [<skill_id>]
 *     condition:
 *       type: <registry_id>
 *       config: { <param>: <value> }
 *     effect:
 *       type: <registry_id>
 *       config: { <param>: <value> }
 */
data class SkillDefinition(
    val id: String,
    val name: String,
    val description: String?,
    val className: String,
    val materialName: String?,
    val prerequisites: List<String>,
    val condition: SkillCondition,
    val effect: SkillEffect
)

data class SkillCondition(
    val type: String,
    val config: Map<String, Any>
)

data class SkillEffect(
    val type: String,
    val config: Map<String, Any>
)
