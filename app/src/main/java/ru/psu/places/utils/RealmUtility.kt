package ru.psu.places.utils

import io.realm.RealmConfiguration

class RealmUtility {
    private val SCHEMA_V_PREV = 1
    private val SCHEMA_V_NOW = 2


    fun getSchemaVNow(): Int {
        return SCHEMA_V_NOW
    }


    fun getDefaultConfig(): RealmConfiguration? {
        return RealmConfiguration.Builder()
            .schemaVersion(SCHEMA_V_NOW.toLong())
            .deleteRealmIfMigrationNeeded()
            .build()
    }
}