package com.contentgrid.configuration.api.lookup;

import java.util.stream.Collectors;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.IIII_Result;

@JCStressTest
@Outcome(id = {"1, 1, [012], [012]"}, expect = Expect.ACCEPTABLE, desc = "We have normality.")
@Outcome(id = {"-1, .*, .*, .*", ".*, -1, .*, .*"}, expect = Expect.FORBIDDEN, desc = "Exception on .add()")
@Outcome(id = {".*, .*, -1, .*"}, expect = Expect.FORBIDDEN, desc = "Exception on stream out")
@Outcome(id = {".*, .*, .*, -1"}, expect = Expect.FORBIDDEN, desc = "Exception on keys read")
@Outcome(expect = Expect.FORBIDDEN, desc = "Something went wrong")
@State
public class ConcurrentLookupStreamRead {

    private final ConcurrentLookup<String, String> map = new ConcurrentLookup<>(String::toUpperCase /*, new NoopReadWriteLock()*/);

    @Actor
    public void actor1(IIII_Result r) {
        try {
            map.add("foo");
            r.r1 = 1;
        } catch (Exception e) {
            e.printStackTrace();
            r.r1 = -1;
        }
    }

    @Actor
    public void actor2(IIII_Result r) {
        try {
            map.add("bar");
            r.r2 = 1;
        } catch (Exception e) {
            e.printStackTrace();
            r.r2 = -1;
        }
    }

    @Actor
    public void actor3(IIII_Result r) {

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

    @Actor
    public void actor4(IIII_Result r) {
        try {
            var data = map.keys();
            r.r4 = data.size();
        } catch(Exception e) {
            e.printStackTrace();
            r.r4 = -1;
        }
    }

}