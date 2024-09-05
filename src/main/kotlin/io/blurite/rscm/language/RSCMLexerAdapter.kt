package io.blurite.rscm.language

import com.intellij.lexer.FlexAdapter

/**
 * @author Chris
 * @since 12/3/2020
 */
class RSCMLexerAdapter : FlexAdapter(RSCMLexer(null))
