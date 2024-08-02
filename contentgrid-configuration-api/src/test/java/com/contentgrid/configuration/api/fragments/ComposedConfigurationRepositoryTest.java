package com.contentgrid.configuration.api.fragments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.contentgrid.configuration.api.AggregateIdConfiguration;
import com.contentgrid.configuration.api.observable.Observable.UpdateEvent;
import com.contentgrid.configuration.api.observable.Observable.UpdateType;
import com.contentgrid.configuration.api.observable.Publisher;
import com.contentgrid.configuration.api.test.ObservableUtils;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import lombok.Value;
import org.awaitility.core.ThrowingRunnable;
import org.junit.jupiter.api.Test;

class ComposedConfigurationRepositoryTest {

    @Value
    private static class TestConfiguration {

        String name;
        Set<String> aliases;

        public TestConfiguration merge(TestConfiguration configuration) {
            var aliasesCopy = new HashSet<>(aliases);
            aliasesCopy.addAll(configuration.aliases);

            var nameCopy = name == null ? configuration.name : name;

            return new TestConfiguration(nameCopy, aliasesCopy);
        }
    }

    private void awaitUntilAsserted(ThrowingRunnable runnable) {
        await()
                .timeout(Duration.of(1, ChronoUnit.SECONDS))
                .untilAsserted(runnable);
    }

    @Test
    void registerConfigurationFragments() {
        var repo = new ComposedConfigurationRepository<String, String, TestConfiguration>(TestConfiguration::merge);

        repo.register(new ConfigurationFragment<>("test", "abc", new TestConfiguration("xyz", Set.of())));
        repo.register(new ConfigurationFragment<>("test2", "abc", new TestConfiguration(null, Set.of("ZZZ"))));
        repo.register(new ConfigurationFragment<>("test3", "abc", new TestConfiguration(null, Set.of("AAA", "BBB"))));

        assertThat(repo.findConfiguration("abc").getConfiguration()).hasValue(
                new TestConfiguration("xyz", Set.of("ZZZ", "AAA", "BBB")));

        var events = ObservableUtils.eventsToList(repo);

        awaitUntilAsserted(() -> {
            assertThat(events).containsExactly(new UpdateEvent<>(UpdateType.ADD, repo.findConfiguration("abc")));
        });

        repo.register(new ConfigurationFragment<>("test4", "def", new TestConfiguration("def", Set.of("MMM"))));

        var abcFirst = repo.findConfiguration("abc");
        var defFirst = repo.findConfiguration("def");

        assertThat(abcFirst.getConfiguration()).hasValue(new TestConfiguration("xyz", Set.of("ZZZ", "AAA", "BBB")));
        assertThat(defFirst.getConfiguration()).hasValue(new TestConfiguration("def", Set.of("MMM")));

        awaitUntilAsserted(() -> {
            assertThat(events).containsExactly(
                    new UpdateEvent<>(UpdateType.ADD, repo.findConfiguration("abc")),
                    new UpdateEvent<>(UpdateType.ADD, repo.findConfiguration("def"))
            );
        });

        repo.revoke("test3");
        assertThat(repo.findConfiguration("abc").getConfiguration()).hasValue(
                new TestConfiguration("xyz", Set.of("ZZZ")));

        awaitUntilAsserted(() -> {
            assertThat(events).containsExactly(
                    new UpdateEvent<>(UpdateType.ADD, abcFirst),
                    new UpdateEvent<>(UpdateType.ADD, defFirst),
                    new UpdateEvent<>(UpdateType.UPDATE, repo.findConfiguration("abc"))
            );
        });

        repo.revoke("test4");

        assertThat(repo.findConfiguration("def").getConfiguration()).isEmpty();

        awaitUntilAsserted(() -> {
            assertThat(events).containsExactly(
                    new UpdateEvent<>(UpdateType.ADD, abcFirst),
                    new UpdateEvent<>(UpdateType.ADD, defFirst),
                    new UpdateEvent<>(UpdateType.UPDATE, repo.findConfiguration("abc")),
                    new UpdateEvent<>(UpdateType.REMOVE, defFirst)
            );
        });
    }

    @Test
    void revokeNonExistingFragment() {
        var repo = new ComposedConfigurationRepository<String, String, TestConfiguration>(TestConfiguration::merge);

        var events = ObservableUtils.eventsToList(repo);

        repo.register(new ConfigurationFragment<>("test", "abc", new TestConfiguration("xyz", Set.of())));
        repo.revoke("xyz");

        awaitUntilAsserted(() -> {
            assertThat(events).containsExactly(
                    new UpdateEvent<>(UpdateType.ADD, repo.findConfiguration("abc"))
            );
        });
    }

    @Test
    void registerFragmentToNewAggregate() {
        var repo = new ComposedConfigurationRepository<String, String, TestConfiguration>(TestConfiguration::merge);

        var events = ObservableUtils.eventsToList(repo);

        repo.register(new ConfigurationFragment<>("test", "abc", new TestConfiguration("xyz", Set.of())));

        var firstAbc = repo.findConfiguration("abc");

        repo.register(new ConfigurationFragment<>("test", "def", new TestConfiguration("xyz123", Set.of())));

        assertThat(repo.findConfiguration("abc").getConfiguration()).isEmpty();
        assertThat(repo.findConfiguration("def").getConfiguration()).hasValue(
                new TestConfiguration("xyz123", Set.of()));

        awaitUntilAsserted(() -> {
            assertThat(events).containsExactly(
                    new UpdateEvent<>(UpdateType.ADD, firstAbc),
                    new UpdateEvent<>(UpdateType.REMOVE, firstAbc),
                    new UpdateEvent<>(UpdateType.ADD, repo.findConfiguration("def"))
            );
        });

    }

    @Test
    void reRegisterFragment() {
        var repo = new ComposedConfigurationRepository<String, String, TestConfiguration>(TestConfiguration::merge);

        var events = ObservableUtils.eventsToConcurrentLookup(repo, AggregateIdConfiguration::getAggregateId);

        repo.register(new ConfigurationFragment<>("test", "abc", new TestConfiguration("xyz", Set.of("ZZZ"))));

        var firstAbc = repo.findConfiguration("abc");

        repo.register(new ConfigurationFragment<>("test", "abc", new TestConfiguration("xyz123", Set.of("ABC"))));

        assertThat(repo.findConfiguration("abc").getConfiguration())
                .hasValue(new TestConfiguration("xyz123", Set.of("ABC")));

        awaitUntilAsserted(() -> {
            assertThat(events.get("abc")).isEqualTo(repo.findConfiguration("abc"));
        });

    }

    @Test
    void subscription() {
        var publisher = new Publisher<ConfigurationFragment<String, String, TestConfiguration>>();
        var repo = new ComposedConfigurationRepository<>(TestConfiguration::merge, publisher);

        assertThat(repo.aggregationIds()).isEmpty();

        publisher.emit(UpdateType.ADD, new ConfigurationFragment<>("test", "abc", new TestConfiguration("xyz", Set.of("ZZZ"))));

        assertThat(repo.aggregationIds()).containsExactly("abc");
        assertThat(repo.findConfiguration("abc").getConfiguration()).hasValue(new TestConfiguration("xyz", Set.of("ZZZ")));

        publisher.emit(UpdateType.UPDATE, new ConfigurationFragment<>("test", "abc", new TestConfiguration("xyz", Set.of("UUU"))));
        assertThat(repo.findConfiguration("abc").getConfiguration()).hasValue(new TestConfiguration("xyz", Set.of("UUU")));

        publisher.emit(UpdateType.REMOVE, new ConfigurationFragment<>("test", "abc", new TestConfiguration("xyz", Set.of("UUU"))));

        assertThat(repo.aggregationIds()).isEmpty();
    }

}