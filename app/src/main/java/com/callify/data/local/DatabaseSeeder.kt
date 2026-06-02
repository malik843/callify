package com.callify.data.local

import com.callify.data.model.CallerInfo

object DatabaseSeeder {

    /**
     * Seeds the contacts database with the mock dataset.
     * No-op if records already exist.
     * Must be called from a coroutine on Dispatchers.IO.
     */
    suspend fun seedIfEmpty(dao: CallerDao) {
        if (dao.count() > 0) return

        val contacts = listOf(
            CallerInfo(firstname = "Arnold",     lastname = "Alaye",           phone = "9074086115", address = "plot 16, otungba jobi fele way, ikeja."),
            CallerInfo(firstname = "Ayokunmi",   lastname = "Israel Adebanjo", phone = "8143147766", address = "plot 16, otungba jobi fele way, ikeja."),
            CallerInfo(firstname = "iyinonitan", lastname = "Olaiya",          phone = "8131609902", address = "plot 16, otungba jobi fele way, ikeja."),
            CallerInfo(firstname = "Mr jide",    lastname = "Alaye",           phone = "8033006821", address = "plot 16, otungba jobi fele way, ikeja."),
            CallerInfo(firstname = "Malik",      lastname = "Yusuff",          phone = "9056226824", address = "plot 16, otungba jobi fele way, ikeja."),
            CallerInfo(firstname = "Hakeem",     lastname = "Alaye",           phone = "9052634086", address = "plot 16, otungba jobi fele way, ikeja."),
            CallerInfo(firstname = "segun",      lastname = "Adelaja",         phone = "9047545530", address = "plot 16, otungba jobi fele way, ikeja."),
            CallerInfo(firstname = "sodiq",      lastname = "Alaye",           phone = "9074086115", address = "plot 16, otungba jobi fele way, ikeja."),
            CallerInfo(firstname = "wisdom",     lastname = "Alaye",           phone = "8176535014", address = "plot 16, otungba jobi fele way, ikeja."),
            CallerInfo(firstname = "Azeem",      lastname = "Ogunmola",        phone = "7052634086", address = "plot 16, otungba jobi fele way, ikeja.")
        )
        dao.insertAll(contacts)
    }
}
