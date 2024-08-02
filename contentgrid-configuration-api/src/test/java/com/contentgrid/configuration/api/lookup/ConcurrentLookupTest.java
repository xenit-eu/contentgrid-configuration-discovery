package com.contentgrid.configuration.api.lookup;

import static org.assertj.core.api.Assertions.assertThat;

import com.contentgrid.configuration.api.observable.Observable.UpdateEvent;
import com.contentgrid.configuration.api.observable.Observable.UpdateType;
import com.contentgrid.configuration.api.observable.Publisher;
import com.contentgrid.configuration.api.test.ObservableUtils;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
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
        assertThat(map.keys()).containsExactlyInAnyOrder("FOO", "BAR");
        assertThat(map.size()).isEqualTo(2);

        map.remove("bar");
        assertThat(map.keys()).containsExactlyInAnyOrder("FOO", "BAR");
        assertThat(map.size()).isEqualTo(2);
        map.remove("BAR");
        assertThat(map.keys()).containsExactlyInAnyOrder("FOO");
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

        assertThat(lengthLookup.get(3)).containsExactlyInAnyOrder("foo", "bar");
        assertThat(lengthLookup.get(4)).isEmpty();
        assertThat(lengthLookup.get(5)).isEmpty();
        assertThat(lengthLookup.get(6)).contains("foobar");

        assertThat(lengthLookup.keys()).containsExactlyInAnyOrder(3, 6);

        map.remove("BAR");
        assertThat(lengthLookup.get(3)).containsExactly("foo");

        map.add("Foo");

        assertThat(lengthLookup.get(3)).containsExactly("Foo");

        map.clear();
        assertThat(lengthLookup.get(3)).isEmpty();

    }

    @Test
    void createMultiLookup() {

        var map = new ConcurrentLookup<String, String>(String::toUpperCase);

        map.add("foo");
        map.add("bar");
        var letterLookup = map.createMultiLookup(str -> Arrays.stream(str.split("")));
        map.add("foobar");
        map.add("baz");

        assertThat(letterLookup.get("f")).containsExactlyInAnyOrder("foo", "foobar");
        assertThat(letterLookup.get("b")).containsExactlyInAnyOrder("bar", "foobar", "baz");
        assertThat(letterLookup.get("o")).containsExactlyInAnyOrder("foo", "foobar");
        assertThat(letterLookup.get("z")).containsExactly("baz");
        assertThat(letterLookup.keys()).containsExactlyInAnyOrder("f", "o", "b", "a", "r", "z");

        map.remove("FOOBAR");

        assertThat(letterLookup.get("f")).containsExactly("foo");
        assertThat(letterLookup.get("b")).containsExactlyInAnyOrder("bar", "baz");
        assertThat(letterLookup.get("o")).containsExactly("foo");

        map.clear();
        assertThat(letterLookup.get("f")).isEmpty();
        assertThat(letterLookup.get("b")).isEmpty();
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

    @Test
    void observeMap() {
        var map = new ConcurrentLookup<String, String>(String::toUpperCase);
        var events = ObservableUtils.eventsToList(map);

        map.add("foo");

        assertThat(events).containsExactly(new UpdateEvent<>(
                UpdateType.ADD, Map.entry("FOO", "foo")
        ));

        map.add("bar");

        assertThat(events).containsExactly(
                new UpdateEvent<>(
                        UpdateType.ADD, Map.entry("FOO", "foo")
                ),
                new UpdateEvent<>(
                        UpdateType.ADD, Map.entry("BAR", "bar")
                )
        );

        map.add("Foo");

        assertThat(events).containsExactly(
                new UpdateEvent<>(
                        UpdateType.ADD, Map.entry("FOO", "foo")
                ),
                new UpdateEvent<>(
                        UpdateType.ADD, Map.entry("BAR", "bar")
                ),
                new UpdateEvent<>(
                        UpdateType.UPDATE, Map.entry("FOO", "Foo")
                )
        );

        var newEvents = ObservableUtils.eventsToList(map);
        assertThat(newEvents).containsExactlyInAnyOrder(
                new UpdateEvent<>(
                        UpdateType.ADD, Map.entry("FOO", "Foo")
                ),
                new UpdateEvent<>(
                        UpdateType.ADD, Map.entry("BAR", "bar")
                )
        );

        map.remove("BAR");

        assertThat(events).containsExactly(
                new UpdateEvent<>(
                        UpdateType.ADD, Map.entry("FOO", "foo")
                ),
                new UpdateEvent<>(
                        UpdateType.ADD, Map.entry("BAR", "bar")
                ),
                new UpdateEvent<>(
                        UpdateType.UPDATE, Map.entry("FOO", "Foo")
                ),
                new UpdateEvent<>(
                        UpdateType.REMOVE, Map.entry("BAR", "bar")
                )
        );
    }

    @Test
    void observeLookup() {
        var map = new ConcurrentLookup<String, String>(String::toUpperCase);

        map.add("foo");
        map.add("bar");
        var lengthLookup = map.createLookup(String::length);

        var events = ObservableUtils.eventsToList(lengthLookup);

        assertThat(events).containsExactly(new UpdateEvent<>(
                UpdateType.ADD,
                Map.entry(3, Set.of("foo", "bar"))
        ));

        map.add("foobar");

        assertThat(events).containsExactly(
                new UpdateEvent<>(
                        UpdateType.ADD,
                        Map.entry(3, Set.of("foo", "bar"))
                ),
                new UpdateEvent<>(
                        UpdateType.ADD,
                        Map.entry(6, Set.of("foobar"))
                )
        );

        map.remove("FOO");
        map.remove("bar"); // This does nothing, because 'bar' is not a key

        assertThat(events).containsExactly(
                new UpdateEvent<>(
                        UpdateType.ADD,
                        Map.entry(3, Set.of("foo", "bar"))
                ),
                new UpdateEvent<>(
                        UpdateType.ADD,
                        Map.entry(6, Set.of("foobar"))
                ),
                new UpdateEvent<>(
                        UpdateType.UPDATE,
                        Map.entry(3, Set.of("bar"))
                )
        );

        map.remove("BAR");

        assertThat(events).containsExactly(
                new UpdateEvent<>(
                        UpdateType.ADD,
                        Map.entry(3, Set.of("foo", "bar"))
                ),
                new UpdateEvent<>(
                        UpdateType.ADD,
                        Map.entry(6, Set.of("foobar"))
                ),
                new UpdateEvent<>(
                        UpdateType.UPDATE,
                        Map.entry(3, Set.of("bar"))
                ),
                new UpdateEvent<>(
                        UpdateType.REMOVE,
                        Map.entry(3, Set.of("bar"))
                )
        );
    }
}