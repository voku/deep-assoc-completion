package org.klesun.deep_keys.resolvers;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.*;
import org.klesun.deep_keys.DeepType;
import org.klesun.deep_keys.helpers.IFuncCtx;
import org.klesun.deep_keys.helpers.MultiType;
import org.klesun.lang.Lang;
import org.klesun.lang.Tls;

public class ClosRes extends Lang
{
    final private IFuncCtx ctx;

    public ClosRes(IFuncCtx ctx)
    {
        this.ctx = ctx;
    }

    public static L<PhpReturnImpl> findFunctionReturns(PsiElement funcBody)
    {
        L<PhpReturnImpl> result = list();
        for (PsiElement child: funcBody.getChildren()) {
            // anonymous functions
            if (child instanceof Function) continue;

            Tls.cast(PhpReturnImpl.class, child)
                .thn(result::add);

            findFunctionReturns(child).forEach(result::add);
        }
        return result;
    }

    public DeepType resolve(FunctionImpl func)
    {
        // TODO: think of a way how to pass arguments here
        IFuncCtx insideCtx = ctx.subCtx(L());

        DeepType result = new DeepType(func, func.getLocalType(true));
        findFunctionReturns(func)
            .map(ret -> ret.getArgument())
            .fop(toCast(PhpExpression.class))
            .map(retVal -> insideCtx.findExprType(retVal).types)
            .fch(ts -> result.returnTypes.addAll(ts));
        return result;
    }

}
