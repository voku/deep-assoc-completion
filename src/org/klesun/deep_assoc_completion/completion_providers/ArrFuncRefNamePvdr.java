package org.klesun.deep_assoc_completion.completion_providers;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.php.lang.psi.elements.impl.ArrayCreationExpressionImpl;
import com.jetbrains.php.lang.psi.elements.impl.StringLiteralExpressionImpl;
import org.jetbrains.annotations.NotNull;
import org.klesun.deep_assoc_completion.contexts.ExprCtx;
import org.klesun.deep_assoc_completion.contexts.FuncCtx;
import org.klesun.deep_assoc_completion.contexts.SearchCtx;
import org.klesun.deep_assoc_completion.resolvers.ArrCtorRes;
import org.klesun.lang.It;

import static org.klesun.lang.Lang.*;

/**
 * provides completion of method names in `[Some\ClassName::class, '']
 * IDEA already provides such functionality, but only when you pass
 * such array somewhere where it is explicitly said to be `callable`
 * this provider, on the other hand, resolves method  _always_
 *
 * not sure hat it is still relevant now, that there is ArrFuncRefCbtr.java...
 */
public class ArrFuncRefNamePvdr extends CompletionProvider<CompletionParameters>
{
    private static LookupElement makeLookup(Method method)
    {
        return LookupElementBuilder.create(method.getName())
            .bold()
            .withIcon(AssocKeyPvdr.getIcon())
            .withTypeText(method.getLocalType(false).filterUnknown().toString());
    }

    /** @return type of an associative array with vars to suggest as keys */
    public static It<Method> resolve(StringLiteralExpression literal, boolean isAutoPopup)
    {
        SearchCtx search = new SearchCtx(literal.getProject())
            .setDepth(AssocKeyPvdr.getMaxDepth(isAutoPopup, literal.getProject()));
        FuncCtx funcCtx = new FuncCtx(search);
        return opt(literal.getParent())
            .map(arrVal -> arrVal.getParent())
            .fop(toCast(ArrayCreationExpressionImpl.class))
            .map(arrCtor -> L(arrCtor.getChildren()))
            .flt(params -> params.size() == 2)
            .flt(params -> literal.isEquivalentTo(params.get(1).getFirstChild()))
            .fop(params -> params.gat(0))
            .fap(clsPsi -> It.cnc(
                ArrCtorRes.resolveClass(clsPsi)
                    .fap(cls -> It(cls.getMethods()))
                    .flt(meth -> meth.isStatic()),
                new ArrCtorRes(new ExprCtx(funcCtx, clsPsi, 0))
                    .resolveInstance(clsPsi)
                    .fap(cls -> It(cls.getMethods()))
                    .flt(meth -> meth.getMethodType(false) != Method.MethodType.CONSTRUCTOR)
                    .flt(meth -> !meth.isStatic()))
            );
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet result)
    {
        It<Method> methods = opt(parameters.getPosition().getParent())
            .cst(StringLiteralExpressionImpl.class)
            .fap(literal -> resolve(literal, parameters.isAutoPopup()));

        methods.map(m -> makeLookup(m)).fch(result::addElement);
    }
}