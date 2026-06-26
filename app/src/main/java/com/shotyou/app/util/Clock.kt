package com.shotyou.app.util

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Abstraction over the system clock so time can be faked in tests. */
interface Clock {
    fun now(): Long
}

@Singleton
class SystemClock @Inject constructor() : Clock {
    override fun now(): Long = System.currentTimeMillis()
}

/** Generates unique ids (for jobs, groups). */
interface IdGenerator {
    fun newId(): String
}

@Singleton
class UuidGenerator @Inject constructor() : IdGenerator {
    override fun newId(): String = UUID.randomUUID().toString()
}
