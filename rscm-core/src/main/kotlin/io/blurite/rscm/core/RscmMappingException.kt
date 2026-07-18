package io.blurite.rscm.core

class RscmMappingException(
    message: String,
    cause: Throwable? = null,
) : IllegalArgumentException(message, cause)
