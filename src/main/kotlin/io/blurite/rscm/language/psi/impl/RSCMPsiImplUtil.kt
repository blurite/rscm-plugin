package io.blurite.rscm.language.psi.impl

import com.intellij.psi.PsiElement
import io.blurite.rscm.language.psi.RSCMElementFactory
import io.blurite.rscm.language.psi.RSCMProperty
import io.blurite.rscm.language.psi.RSCMTypes

/**
 * @author Chris
 * @since 12/3/2020
 */
object RSCMPsiImplUtil {
    @JvmStatic
    fun getKey(element: RSCMProperty?): String? {
        val keyNode = element?.node?.findChildByType(RSCMTypes.KEY)
        return keyNode?.text?.replace("\\\\ ", " ")
    }

    @JvmStatic
    fun getValue(element: RSCMProperty?): String? {
        val valueNode = element?.node?.findChildByType(RSCMTypes.VALUE)
        return valueNode?.text
    }

    @JvmStatic
    fun getName(element: RSCMProperty?): String? {
        return getKey(element)
    }

    @JvmStatic
    fun setName(
        element: RSCMProperty,
        newName: String,
    ): PsiElement {
        val keyNode = element.node.findChildByType(RSCMTypes.KEY)
        if (keyNode != null) {
            val property = RSCMElementFactory.createProperty(element.project, newName)
            val newKeyNode = property?.firstChild?.node
            if (newKeyNode != null) {
                element.node.replaceChild(keyNode, newKeyNode)
            }
        }
        return element
    }

    @JvmStatic
    fun getNameIdentifier(element: RSCMProperty): PsiElement? {
        val keyNode = element.node.findChildByType(RSCMTypes.KEY)
        return keyNode?.psi
    }
}
