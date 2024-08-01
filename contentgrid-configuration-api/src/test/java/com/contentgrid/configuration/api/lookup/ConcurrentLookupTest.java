package com.contentgrid.configuration.api.lookup;

import static org.assertj.core.api.Assertions.assertThat;

import com.contentgrid.configuration.api.observable.Observable.UpdateType;
import com.contentgrid.configuration.api.observable.Publisher;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ConcurrentLookupTest {

    @Test
    void simpleOperations() {
        var map = new ConcurrentLookup<String, String>(String::toUpperCase);
        map.add("foo");
        map.add("bar");
        map.add("foo");

        assertThat(map.get("foo")).isNull();
        assertThat(map.get("FOO")).isEqualTo("foo");
        assertThat(map.size()).isEqualTo(2);

        map.remove("bar");
        assertThat(map.size()).isEqualTo(2);
        map.remove("BAR");
        assertThat(map.size()).isEqualTo(1);

        map.clear();
        assertThat(map.size()).isZero();
    }


    @Test
    void stream() {

        var map = new ConcurrentLookup<String, String>(String::toUpperCase);

        map.add("foo");
        var stream = map.stream();
        map.add("bar");

        assertThat(stream).hasSize(2);

        var stream2 = map.stream();
        map.clear();
        assertThat(stream2).isEmpty();
    }

    @Test
    void createLookup() {

        var map = new ConcurrentLookup<String, String>(String::toUpperCase);

        map.add("foo");
        map.add("bar");
        var lengthLookup = map.createLookup(String::length);
        map.add("foobar");

        assertThat(lengthLookup.apply(3)).containsExactlyInAnyOrder("foo", "bar");
        assertThat(lengthLookup.apply(4)).isEmpty();
        assertThat(lengthLookup.apply(5)).isEmpty();
        assertThat(lengthLookup.apply(6)).contains("foobar");

        map.remove("BAR");
        assertThat(lengthLookup.apply(3)).containsExactly("foo");

        map.clear();
        assertThat(lengthLookup.apply(3)).isEmpty();

    }

    @Test
    void createMultiLookup() {

        var map = new ConcurrentLookup<String, String>(String::toUpperCase);

        map.add("foo");
        map.add("bar");
        var letterLookup = map.createMultiLookup(str -> Arrays.stream(str.split("")));
        map.add("foobar");
        map.add("baz");

        assertThat(letterLookup.apply("f")).containsExactlyInAnyOrder("foo", "foobar");
        assertThat(letterLookup.apply("b")).containsExactlyInAnyOrder("bar", "foobar", "baz");
        assertThat(letterLookup.apply("o")).containsExactlyInAnyOrder("foo", "foobar");
        assertThat(letterLookup.apply("z")).containsExactly("baz");

        map.remove("FOOBAR");

        assertThat(letterLookup.apply("f")).containsExactly("foo");
        assertThat(letterLookup.apply("b")).containsExactlyInAnyOrder("bar", "baz");
        assertThat(letterLookup.apply("o")).containsExactly("foo");

        map.clear();
        assertThat(letterLookup.apply("f")).isEmpty();
        assertThat(letterLookup.apply("b")).isEmpty();
    }

    @Test
    void subscribe() {
        var map = new ConcurrentLookup<String, String>(String::toUpperCase);
        var publisher = new Publisher<String>();
        map.subscribe(publisher);

        assertThat(map.get("FOO")).isNull();

        publisher.emit(UpdateType.ADD, "foo");

        assertThat(map.get("FOO")).isEqualTo("foo");

        publisher.emit(UpdateType.UPDATE, "Foo");

        assertThat(map.get("FOO")).isEqualTo("Foo");

        publisher.emit(UpdateType.REMOVE, "Foo");

        assertThat(map.get("FOO")).isNull();
    }
}