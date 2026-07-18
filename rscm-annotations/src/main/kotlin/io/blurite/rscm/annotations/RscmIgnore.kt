package io.blurite.rscm.annotations

/** Suppresses RSCM interpretation and validation for a string property or value parameter. */
@Target(
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.LOCAL_VARIABLE,
)
@Retention(AnnotationRetention.BINARY)
annotation class RscmIgnore
