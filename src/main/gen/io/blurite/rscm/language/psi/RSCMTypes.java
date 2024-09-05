// This is a generated file. Not intended for manual editing.
package io.blurite.rscm.language.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import io.blurite.rscm.language.psi.impl.RSCMPropertyImpl;

public interface RSCMTypes {

    IElementType PROPERTY = new RSCMElementType("PROPERTY");

    IElementType COMMENT = new RSCMTokenType("COMMENT");
    IElementType CRLF = new RSCMTokenType("CRLF");
    IElementType KEY = new RSCMTokenType("KEY");
    IElementType SEPARATOR = new RSCMTokenType("SEPARATOR");
    IElementType VALUE = new RSCMTokenType("VALUE");

    class Factory {
        public static PsiElement createElement(ASTNode node) {
            IElementType type = node.getElementType();
            if (type == PROPERTY) {
                return new RSCMPropertyImpl(node);
            }
            throw new AssertionError("Unknown element type: " + type);
        }
    }
}
