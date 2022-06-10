/*
 * Copyright 2021-2022 Golden_Water
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package indi.goldenwater.chaosmusicplayer.utils

val mcVersionRegex = Regex("""\d+\.\d+(\.\d+)?""")

data class MCVersion(
    val release: Int,
    val major: Int,
    val patch: Int = 0,
) : Comparable<MCVersion> {
    override fun toString(): String {
        return "$release.$major.$patch"
    }

    override fun equals(other: Any?): Boolean {
        if (other !is MCVersion) return super.equals(other)
        return release == other.release && major == other.major && patch == other.major
    }

    override fun compareTo(other: MCVersion): Int {
        var currentCompare = release.compareTo(other.release)
        if (currentCompare != 0) return currentCompare

        currentCompare = major.compareTo(other.major)
        if (currentCompare != 0) return currentCompare

        currentCompare = patch.compareTo(other.patch)
        if (currentCompare != 0) return currentCompare

        return 0
    }

    override fun hashCode(): Int {
        var result = release
        result = 31 * result + major
        result = 31 * result + patch
        return result
    }
}

/**
 * Bukkit version to Minecraft version
 * Note: releases only
 */
fun String.toMCVersion(): MCVersion {
    val versionStr = mcVersionRegex.find(this)?.value ?: return MCVersion(0, 0)

    val versionParts = versionStr.split(".")
        .mapNotNull { it.toIntOrNull() }
        .toMutableList()

    return MCVersion(
        versionParts.removeFirstOrNull() ?: 0,
        versionParts.removeFirstOrNull() ?: 0,
        versionParts.removeFirstOrNull() ?: 0
    )
}