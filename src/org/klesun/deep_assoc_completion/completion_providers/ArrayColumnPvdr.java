package org.klesun.deep_assoc_completion.completion_providers;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.php.lang.psi.elements.impl.FunctionReferenceImpl;
import com.jetbrains.php.lang.psi.elements.impl.StringLiteralExpressionImpl;
import org.jetbrains.annotations.NotNull;
import org.klesun.deep_assoc_completion.structures.DeepType;
import org.klesun.deep_assoc_completion.contexts.FuncCtx;
import org.klesun.deep_assoc_completion.helpers.Mt;
import org.klesun.deep_assoc_completion.contexts.SearchCtx;
import org.klesun.lang.It;

import static org.klesun.lang.Lang.*;

import org.klesun.lang.Opt;
import org.klesun.lang.Tls;

import java.util.HashSet;
import java.util.Set;


/**
 * go to declaration functionality for key name in `array_column($segments, 'segmentNumber')`
 */
public class ArrayColumnPvdr extends CompletionProvider<CompletionParameters>
{
    private static LookupElementBuilder makeLookupBase(String keyName, String type)
    {
        return LookupElementBuilder.create(keyName)
            .bold()
            .withIcon(AssocKeyPvdr.getIcon())
            .withTypeText(type);
    }

    private static It<LookupElementBuilder> makeOptions(It<DeepType> tit)
    {
        Set<String> suggested = new HashSet<>();
        Mutable<Boolean> hadArrt = new Mutable<>(false);
        return tit.fap(type -> type.keys
            .fap(k -> k.keyType.getTypes.get().fap(kt -> {
                String typeStr = k.getTypes().fst().map(t -> t.briefType.toString()).def("unknown");
                if (kt.stringValue != null) {
                    if (!suggested.contains(kt.stringValue)) {
                        suggested.add(kt.stringValue);
                        return It(som(makeLookupBase(kt.stringValue, typeStr)));
                    }
                } else if (!hadArrt.get() && kt.isNumber()) {
                    hadArrt.set(true);
                    return Tls.range(0, 5).map(i -> makeLookupBase(i + "", typeStr).withBoldness(false));
                }
                return It.non();
            })));
    }

    // array_intersect($cmdTypes, ['redisplayPnr', 'itinerary', 'airItinerary', 'storeKeepPnr', 'changeArea', ''])
    private static Opt<It<DeepType>> resolveArrayColumn(StringLiteralExpression literal, FuncCtx funcCtx)
    {
        return opt(literal.getParent())
            .map(argList -> argList.getParent())
            .fop(toCast(FunctionReferenceImpl.class))
            .flt(call -> "array_column".equals(call.getName()))
            .fop(call -> L(call.getParameters()).gat(0))
            .fop(toCast(PhpExpression.class))
            .map(arr -> funcCtx.findExprType(arr)
            .fap(tit -> Mt.getElSt(tit)));
    }

    private static It<DeepType> resolve(StringLiteralExpression lit, boolean isAutoPopup)
    {
        SearchCtx search = new SearchCtx(lit.getProject())
            .setDepth(AssocKeyPvdr.getMaxDepth(isAutoPopup, lit.getProject()));
        FuncCtx funcCtx = new FuncCtx(search);

        return Opt.fst(
            () -> resolveArrayColumn(lit, funcCtx)
        ).fap(a -> a);
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet result)
    {
        It<DeepType> tit = opt(parameters.getPosition().getParent())
            .fop(toCast(StringLiteralExpression.class))
            .fap(lit -> resolve(lit, parameters.isAutoPopup()));

        makeOptions(tit).fch(result::addElement);
    }

    // ================================
    //  GotoDeclarationHandler part follows
    // ================================

    public static It<PsiElement> resolveDeclPsis(@NotNull PsiElement psiElement, int mouseOffset)
    {
        return opt(psiElement.getParent())
            .fop(toCast(StringLiteralExpressionImpl.class))
            .fap(literal -> resolve(literal, false)
                .fap(arrayType -> arrayType.keys)
                .fap(k -> k.keyType.getTypes.get())
                .flt(kt -> literal.getContents().equals(kt.stringValue)))
            .map(t -> t.definition);
    }
}
