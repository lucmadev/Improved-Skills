package org.lucma.ImprovedSkills

import org.bukkit.plugin.java.JavaPlugin
import org.lucma.ImprovedSkills.skill.OpenRpgBridge
import org.lucma.ImprovedSkills.skill.SkillParser

/**
 * Improved Skills — Extension plugin for openRPG.
 *
 * Provides 100+ skills across three classes (Warrior, Mage, Assassin)
 * registered programmatically into openRPG's talent tree system.
 *
 * All skill definitions live in skills.yml (src/main/resources/).
 * Add, remove or modify skills without recompiling the plugin.
 */
class ImprovedSkills : JavaPlugin() {

    private lateinit var skillParser: SkillParser
    private lateinit var openRpgBridge: OpenRpgBridge

    override fun onEnable() {
        val pluginVersion = description.version
        logger.info("+-----------------------------------+")
        logger.info("|   Improved Skills v$pluginVersion   |")
        logger.info("+-----------------------------------+")

        // Step 1: Parse skills from bundled YAML
        skillParser = SkillParser(this)
        val skills = skillParser.parse()

        if (skills.isEmpty()) {
            logger.severe("[ImprovedSkills] No skills were parsed! Check skills.yml syntax.")
            logger.severe("[ImprovedSkills] Plugin will still enable but no skills will be registered.")
            return
        }

        // Step 2: Initialize openRPG bridge
        openRpgBridge = OpenRpgBridge(this)
        val bridgeReady = openRpgBridge.initialize()

        if (!bridgeReady) {
            logger.warning("[ImprovedSkills] openRPG API bridge failed to initialize.")
            logger.warning("[ImprovedSkills] Make sure openRPG plugin is installed and loaded before Improved Skills.")
            logger.warning("[ImprovedSkills] Fallback: copy skills.yml to plugins/openRPG/skills.yml manually.")
        }

        // Step 3: Register all skills into openRPG
        openRpgBridge.registerAllSkills(skills)

        logger.info("[ImprovedSkills] Enabled successfully")
    }

    override fun onDisable() {
        logger.info("[ImprovedSkills] Disabled")
    }
}
