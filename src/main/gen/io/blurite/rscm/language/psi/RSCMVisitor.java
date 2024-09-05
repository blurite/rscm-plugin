// This is a generated file. Not intended for manual editing.
package io.blurite.rscm.language.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

public class RSCMVisitor extends PsiElementVisitor {

    public void visitProperty(@NotNull RSCMProperty o) {
        visitNamedElement(o);
    }

    public void visitNamedElement(@NotNull RSCMNamedElement o) {
        visitPsiElement(o);
    }

    public void visitPsiElement(@NotNull PsiElement o) {
        visitElement(o);
    }

}
