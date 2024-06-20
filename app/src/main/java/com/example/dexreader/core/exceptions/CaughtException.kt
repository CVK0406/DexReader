package com.example.dexreader.core.exceptions

class CaughtException(cause: Throwable, override val message: String?) : RuntimeException(cause)
