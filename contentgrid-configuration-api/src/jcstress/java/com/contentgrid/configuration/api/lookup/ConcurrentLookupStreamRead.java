package com.contentgrid.configuration.api.lookup;

import java.util.stream.Collectors;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.III_Result;

@JCStressTest
@Outcome(id = {"1, 1, 0", "1, 1, 1", "1, 1, 2"}, expect = Expect.ACCEPTABLE, desc = "We have normality.")
@Outcome(id = {"-1, .*, .*", "0, .*, .*"}, expect = Expect.FORBIDDEN, desc = "Exception on .add()")
@Outcome(id = {".*, .*, -1"}, expect = Expect.FORBIDDEN, desc = "Exception on stream out")
@Outcome(expect = Expect.FORBIDDEN, desc = "Something went wrong")
@State
public class ConcurrentLookupStreamRead {

    private final ConcurrentLookup<String, Object> map = new ConcurrentLookup<>(Object::toString/*, new NoopReadWriteLock()*/);

    @Actor
    public void actor1(III_Result r) {
        try {
            map.add("foo");
            r.r1 = 1;
        } catch (Exception e) {
            r.r1 = -1;
        }
    }

    @Actor
    public void actor2(III_Result r) {
        try {
            map.add("bar");
            r.r2 = 1;
        } catch (Exception e) {
            r.r2 = -1;
        }
    }

    @Actor
    public void actor3(III_Result r) {

        try {
            var data = map.stream()
                    .map(Object::hashCode)
                    .collect(Collectors.toUnmodifiableSet());
            r.r3 = data.size();
        } catch (Exception e) {
            e.printStackTrace();
            r.r3 = -1;
        }
    }

}