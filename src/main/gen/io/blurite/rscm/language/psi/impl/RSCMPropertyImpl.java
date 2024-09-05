// This is a generated file. Not intended for manual editing.
package io.blurite.rscm.language.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import io.blurite.rscm.language.psi.RSCMNamedElementImpl;
import io.blurite.rscm.language.psi.RSCMProperty;
import io.blurite.rscm.language.psi.RSCMVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RSCMPropertyImpl extends RSCMNamedElementImpl implements RSCMProperty {

    public RSCMPropertyImpl(@NotNull ASTNode node) {
        super(node);
    }

    public void accept(@NotNull RSCMVisitor visitor) {
        visitor.visitProperty(this);
    }

    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof RSCMVisitor) accept((RSCMVisitor) visitor);
        else super.accept(visitor);
    }

    @Override
    @Nullable
    public String getKey() {
        return RSCMPsiImplUtil.getKey(this);
    }

    @Override
    @Nullable
    public String getValue() {
        return RSCMPsiImplUtil.getValue(this);
    }

    @Override
    @Nullable
    public String getName() {
        return RSCMPsiImplUtil.getName(this);
    }

    @Override
    @NotNull
    public PsiElement setName(@NotNull String newName) {
        return RSCMPsiImplUtil.setName(this, newName);
    }

    @Override
    @Nullable
    public PsiElement getNameIdentifier() {
        return RSCMPsiImplUtil.getNameIdentifier(this);
    }

}
