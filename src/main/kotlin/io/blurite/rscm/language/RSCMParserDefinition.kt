package io.blurite.rscm.language

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.ParserDefinition.SpaceRequirements
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import io.blurite.rscm.language.parser.RSCMParser
import io.blurite.rscm.language.psi.RSCMFile
import io.blurite.rscm.language.psi.RSCMTypes

/**
 * @author Chris
 * @since 12/3/2020
 */
class RSCMParserDefinition : ParserDefinition {
    companion object {
        private val WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE)
        private val COMMENTS = TokenSet.create(RSCMTypes.COMMENT)
        private val FILE = IFileElementType(RSCMLanguage)
    }

    override fun createLexer(project: Project) = RSCMLexerAdapter()

    override fun getWhitespaceTokens() = WHITE_SPACES

    override fun getCommentTokens() = COMMENTS

    override fun getStringLiteralElements() = TokenSet.EMPTY

    override fun createParser(project: Project) = RSCMParser()

    override fun getFileNodeType() = FILE

    override fun createFile(viewProvider: FileViewProvider) = RSCMFile(viewProvider)

    override fun spaceExistenceTypeBetweenTokens(
        left: ASTNode?,
        right: ASTNode?,
    ) = SpaceRequirements.MAY

    override fun createElement(node: ASTNode) = RSCMTypes.Factory.createElement(node)
}
