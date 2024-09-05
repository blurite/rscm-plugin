// This is a generated file. Not intended for manual editing.
package io.blurite.rscm.language.parser;

import com.intellij.lang.ASTNode;
import com.intellij.lang.LightPsiParser;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;

import static com.intellij.lang.parser.GeneratedParserUtilBase.*;
import static io.blurite.rscm.language.psi.RSCMTypes.*;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class RSCMParser implements PsiParser, LightPsiParser {

    static boolean parse_root_(IElementType t, PsiBuilder b, int l) {
        return rscmFile(b, l + 1);
    }

    /* ********************************************************** */
    // property|COMMENT|CRLF
    static boolean item_(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "item_")) return false;
        boolean r;
        r = property(b, l + 1);
        if (!r) r = consumeToken(b, COMMENT);
        if (!r) r = consumeToken(b, CRLF);
        return r;
    }

    /* ********************************************************** */
    // (KEY? SEPARATOR VALUE?) | KEY
    public static boolean property(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "property")) return false;
        if (!nextTokenIs(b, "<property>", KEY, SEPARATOR)) return false;
        boolean r;
        Marker m = enter_section_(b, l, _NONE_, PROPERTY, "<property>");
        r = property_0(b, l + 1);
        if (!r) r = consumeToken(b, KEY);
        exit_section_(b, l, m, r, false, null);
        return r;
    }

    // KEY? SEPARATOR VALUE?
    private static boolean property_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "property_0")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = property_0_0(b, l + 1);
        r = r && consumeToken(b, SEPARATOR);
        r = r && property_0_2(b, l + 1);
        exit_section_(b, m, null, r);
        return r;
    }

    // KEY?
    private static boolean property_0_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "property_0_0")) return false;
        consumeToken(b, KEY);
        return true;
    }

    // VALUE?
    private static boolean property_0_2(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "property_0_2")) return false;
        consumeToken(b, VALUE);
        return true;
    }

    /* ********************************************************** */
    // item_*
    static boolean rscmFile(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "rscmFile")) return false;
        while (true) {
            int c = current_position_(b);
            if (!item_(b, l + 1)) break;
            if (!empty_element_parsed_guard_(b, "rscmFile", c)) break;
        }
        return true;
    }

    public ASTNode parse(IElementType t, PsiBuilder b) {
        parseLight(t, b);
        return b.getTreeBuilt();
    }

    public void parseLight(IElementType t, PsiBuilder b) {
        boolean r;
        b = adapt_builder_(t, b, this, null);
        Marker m = enter_section_(b, 0, _COLLAPSE_, null);
        r = parse_root_(t, b);
        exit_section_(b, 0, m, t, r, true, TRUE_CONDITION);
    }

    protected boolean parse_root_(IElementType t, PsiBuilder b) {
        return parse_root_(t, b, 0);
    }

}
