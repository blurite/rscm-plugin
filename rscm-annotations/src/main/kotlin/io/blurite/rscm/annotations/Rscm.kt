package io.blurite.rscm.annotations

/**
 * Constrains a string declaration or type use to references from [value].
 *
 * The RSCM compiler and IntelliJ plugins validate compile-time string literals. Dynamic values,
 * including interpolated strings, are intentionally left to application code.
 */
@Target(
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.TYPE,
)
@Retention(AnnotationRetention.BINARY)
annotation class Rscm(
    /** The required RSCM type. Named `value` so Java can use `@Rscm("item")`. */
    val value: String = "",
)
