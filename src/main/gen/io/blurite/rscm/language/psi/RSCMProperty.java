// This is a generated file. Not intended for manual editing.
package io.blurite.rscm.language.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface RSCMProperty extends RSCMNamedElement {

    @Nullable
    String getKey();

    @Nullable
    String getValue();

    @Nullable
    String getName();

    @NotNull
    PsiElement setName(@NotNull String newName);

    @Nullable
    PsiElement getNameIdentifier();

}
