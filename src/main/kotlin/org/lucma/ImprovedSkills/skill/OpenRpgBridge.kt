package org.lucma.ImprovedSkills.skill

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.plugin.Plugin
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Reflection-based bridge to the openRPG API.
 *
 * Registers skills parsed from YAML into openRPG's talent tree system
 * without requiring the openRPG JAR at compile time.
 */
class OpenRpgBridge(private val plugin: Plugin) {

    private val logger: Logger = plugin.logger
    private var apiInitialized = false

    // Cached reflection handles for performance
    private var apiClass: Class<*>? = null
    private var apiInstance: Any? = null
    private var createConditionMethod: java.lang.reflect.Method? = null
    private var createEffectMethod: java.lang.reflect.Method? = null
    private var registerSkillMethod: java.lang.reflect.Method? = null

    /**
     * Initialize the bridge by locating the openRPG API via Bukkit's ServicesManager.
     * @return true if openRPG was found and API is ready
     */
    fun initialize(): Boolean {
        if (apiInitialized) return true

        return try {
            // Locate OpenRPGAPI class via reflection
            val openRpgApiClass = Class.forName("org.lucma.openRPG.api.OpenRPGAPI")

            // Load from Bukkit's ServicesManager
            val servicesManager = Bukkit.getServicesManager()
            val loadMethod = servicesManager.javaClass.getMethod("load", Class::class.java)
            val api = loadMethod.invoke(servicesManager, openRpgApiClass)

            if (api == null) {
                logger.warning("[ImprovedSkills] openRPG API not registered in ServicesManager")
                return false
            }

            // Cache reflection handles
            apiClass = openRpgApiClass
            apiInstance = api

            createConditionMethod = openRpgApiClass.getMethod("createCondition", String::class.java, Map::class.java)
            createEffectMethod = openRpgApiClass.getMethod("createEffect", String::class.java, Map::class.java)

            // Resolve registerSkill overload: registerSkill(String, String, String, String, Condition, Effect, Material, List)
            for (method in openRpgApiClass.methods) {
                if (method.name == "registerSkill" && method.parameterCount == 8) {
                    registerSkillMethod = method
                    break
                }
            }

            if (registerSkillMethod == null) {
                logger.warning("[ImprovedSkills] Could not find registerSkill method on openRPG API")
                return false
            }

            apiInitialized = true
            logger.info("[ImprovedSkills] openRPG API bridge initialized successfully")
            true
        } catch (e: Exception) {
            logger.log(Level.WARNING, "[ImprovedSkills] Failed to initialize openRPG API bridge", e)
            false
        }
    }

    /**
     * Register all parsed skill definitions into openRPG.
     * Skills with errors are logged and skipped individually.
     */
    fun registerAllSkills(skills: List<SkillDefinition>) {
        if (!apiInitialized) {
            logger.warning("[ImprovedSkills] openRPG API not available — skills will NOT be registered")
            logger.warning("[ImprovedSkills] Place skills.yml manually at plugins/openRPG/skills.yml as fallback")
            return
        }

        var success = 0
        var errors = 0

        for (skill in skills) {
            try {
                registerSkill(skill)
                success++
            } catch (e: Exception) {
                logger.log(Level.WARNING, "[ImprovedSkills] Failed to register skill '${skill.id}': ${e.message}")
                errors++
            }
        }

        logger.info("[ImprovedSkills] Skills registered: $success success, $errors errors")
    }

    /**
     * Register a single skill via openRPG API.
     */
    private fun registerSkill(skill: SkillDefinition) {
        val api = apiInstance ?: throw IllegalStateException("openRPG API not initialized")

        // 1. Create the Condition object
        val condition = createConditionMethod!!.invoke(api, skill.condition.type, skill.condition.config)
            ?: throw IllegalStateException("createCondition returned null for type '${skill.condition.type}'")

        // 2. Create the Effect object
        val effect = createEffectMethod!!.invoke(api, skill.effect.type, skill.effect.config)
            ?: throw IllegalStateException("createEffect returned null for type '${skill.effect.type}'")

        // 3. Resolve Bukkit Material (default: ENCHANTED_BOOK)
        val material = try {
            skill.materialName?.let { Material.valueOf(it.uppercase()) } ?: Material.ENCHANTED_BOOK
        } catch (e: IllegalArgumentException) {
            logger.fine("[ImprovedSkills] Unknown material '${skill.materialName}' for skill '${skill.id}', using ENCHANTED_BOOK")
            Material.ENCHANTED_BOOK
        }

        // 4. Build prerequisites list
        val prerequisites: List<String> = skill.prerequisites

        // 5. Register the complete skill
        val description = skill.description ?: ""

        registerSkillMethod!!.invoke(
            api,
            skill.id,
            skill.name,
            description,
            skill.className,
            condition,
            effect,
            material,
            prerequisites
        )
    }
}
