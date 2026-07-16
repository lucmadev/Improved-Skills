package org.lucma.ImprovedSkills.skill

import org.bukkit.plugin.java.JavaPlugin
import org.yaml.snakeyaml.Yaml
import java.util.logging.Level

/**
 * Parses the bundled skills.yml into a list of SkillDefinition.
 *
 * Uses SnakeYAML (bundled with Paper/Bukkit) — no extra dependency needed.
 */
class SkillParser(private val plugin: JavaPlugin) {

    private val yaml = Yaml()
    private val logger = plugin.logger

    /**
     * Reads and parses skills.yml from the plugin's resources.
     * @return List of validated SkillDefinition objects. Invalid entries are skipped with warnings.
     */
    fun parse(): List<SkillDefinition> {
        val raw = loadRawYaml() ?: return emptyList()
        val skillsNode = raw["skills"] as? Map<String, Any> ?: run {
            logger.warning("[ImprovedSkills] skills.yml missing root 'skills' key")
            return emptyList()
        }

        val result = mutableListOf<SkillDefinition>()
        var errors = 0

        for ((id, data) in skillsNode) {
            val node = data as? Map<String, Any> ?: run {
                logger.warning("[ImprovedSkills] Skill '$id' is not a valid map, skipping")
                errors++
                continue
            }

            try {
                val skill = parseSkill(id, node)
                result.add(skill)
            } catch (e: Exception) {
                logger.log(Level.WARNING, "[ImprovedSkills] Error parsing skill '$id': ${e.message}", e)
                errors++
            }
        }

        logger.info("[ImprovedSkills] Skills parsed: ${result.size} loaded, $errors errors")
        return result
    }

    /**
     * Parse a single skill entry from YAML.
     */
    private fun parseSkill(id: String, node: Map<String, Any>): SkillDefinition {
        val name = node["name"] as? String
            ?: throw IllegalArgumentException("Missing required field 'name'")

        val className = node["class"] as? String
            ?: throw IllegalArgumentException("Missing required field 'class'")

        // Parse condition block
        val conditionNode = node["condition"] as? Map<String, Any>
            ?: throw IllegalArgumentException("Missing required field 'condition'")
        val conditionType = conditionNode["type"] as? String
            ?: throw IllegalArgumentException("Missing 'condition.type'")
        val conditionConfig = (conditionNode["config"] as? Map<String, Any>)?.toMutableMap() ?: mutableMapOf()
        coerceNumericValues(conditionConfig)

        // Parse effect block
        val effectNode = node["effect"] as? Map<String, Any>
            ?: throw IllegalArgumentException("Missing required field 'effect'")
        val effectType = effectNode["type"] as? String
            ?: throw IllegalArgumentException("Missing 'effect.type'")
        val effectConfig = (effectNode["config"] as? Map<String, Any>)?.toMutableMap() ?: mutableMapOf()
        coerceNumericValues(effectConfig)

        val description = node["description"] as? String
        val materialName = node["material"] as? String

        @Suppress("UNCHECKED_CAST")
        val prerequisites = (node["prerequisites"] as? List<String>) ?: emptyList()

        return SkillDefinition(
            id = id,
            name = name,
            description = description,
            className = className,
            materialName = materialName,
            prerequisites = prerequisites,
            condition = SkillCondition(conditionType, conditionConfig),
            effect = SkillEffect(effectType, effectConfig)
        )
    }

    /**
     * Ensures numeric strings from YAML are coerced to proper Number types.
     * SnakeYAML sometimes reads numeric values inconsistently depending on format.
     */
    private fun coerceNumericValues(config: MutableMap<String, Any>) {
        for ((key, value) in config.toMap()) {
            when (value) {
                is Number -> config[key] = value.toDouble()
                is String -> {
                    val trimmed = value.trim()
                    val num = trimmed.toDoubleOrNull()
                    if (num != null) config[key] = num
                }
            }
        }
    }

    /**
     * Loads the raw YAML file from plugin resources.
     */
    private fun loadRawYaml(): Map<String, Any>? {
        return try {
            plugin.getResource("skills.yml")?.use { inputStream ->
                @Suppress("UNCHECKED_CAST")
                yaml.load(inputStream) as? Map<String, Any>
            }
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "[ImprovedSkills] Failed to load skills.yml from resources", e)
            null
        }
    }
}
