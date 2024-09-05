package io.blurite.rscm.language

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import io.blurite.rscm.language.psi.RSCMTypes

/**
 * @author Chris
 * @since 12/3/2020
 */
class RSCMSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer() = RSCMLexerAdapter()

    override fun getTokenHighlights(tokenType: IElementType) =
        when (tokenType) {
            RSCMTypes.SEPARATOR -> SEPARATOR_KEYS
            RSCMTypes.KEY -> KEY_KEYS
            RSCMTypes.VALUE -> VALUE_KEYS
            RSCMTypes.COMMENT -> COMMENT_KEYS
            TokenType.BAD_CHARACTER -> BAD_CHAR_KEYS
            else -> EMPTY_KEYS
        }

    companion object {
        val SEPARATOR: TextAttributesKey =
            createTextAttributesKey("RSCM_SEPARATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN)
        private val KEY: TextAttributesKey =
            createTextAttributesKey("RSCM_KEY", DefaultLanguageHighlighterColors.KEYWORD)
        val VALUE: TextAttributesKey = createTextAttributesKey("RSCM_VALUE", DefaultLanguageHighlighterColors.STRING)
        private val COMMENT: TextAttributesKey =
            createTextAttributesKey("RSCM_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
        private val BAD_CHARACTER: TextAttributesKey =
            createTextAttributesKey("RSCM_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER)

        private val BAD_CHAR_KEYS = arrayOf(BAD_CHARACTER)
        private val SEPARATOR_KEYS = arrayOf(SEPARATOR)
        private val KEY_KEYS = arrayOf(KEY)
        private val VALUE_KEYS = arrayOf(VALUE)
        private val COMMENT_KEYS = arrayOf(COMMENT)
        private val EMPTY_KEYS = emptyArray<TextAttributesKey>()
    }
}
