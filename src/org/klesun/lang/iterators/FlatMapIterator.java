package org.klesun.lang.iterators;

import org.klesun.lang.*;

import java.util.Iterator;

import static org.klesun.lang.Lang.*;

public class FlatMapIterator<A, B> implements Iterator<B> {
    final It<Iterable<B>> iterables;
    Opt<Iterator<Iterator<B>>> iterators = non();
    private Iterator<B> current = new L<B>().iterator();
    private int i = 0;

    public FlatMapIterator(Iterator<A> sourceIt, Lang.F2<A, Integer, Iterable<B>> flatten) {
        // hundreds of thousands of types come here. this is probably wrong and should be fixed
        this.iterables = It(() -> sourceIt)
            .map((el, i) -> flatten.apply(el,i))
            // this does not seem a good solution since it slows tests
            // it would be better if there were no duplicates in the first place
            .unq(ble -> ble instanceof It ? ((It<B>) ble).getSourceHash() : ble)
            ;
    }

    private Iterator<Iterator<B>> getIterators() {
        if (!iterators.has()) {
            iterators = som(iterables.map(ble -> ble.iterator()).iterator());
        }
        return iterators.unw();
    }

    private Opt<Lang.S<B>> getNextSup() {
        Iterator<Iterator<B>> iterators = getIterators();
        if (current.hasNext()) {
            return som(() -> current.next());
        } else {
            while (iterators.hasNext()) {
                current = iterators.next();
                if (current.hasNext()) {
                    return som(() -> current.next());
                }
                ++i;
            }
            return non();
        }
    }
    public boolean hasNext() {
        return getNextSup().has();
    }
    public B next() {
        return getNextSup().unw().get();
    }
}
