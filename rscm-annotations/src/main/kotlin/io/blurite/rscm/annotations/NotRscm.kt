package io.blurite.rscm.annotations

/** Requires statically known strings to not be RSCM references. */
@Target(
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.TYPE,
)
@Retention(AnnotationRetention.BINARY)
annotation class NotRscm
