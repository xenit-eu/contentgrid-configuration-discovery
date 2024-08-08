package com.contentgrid.configuration.api.fragments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.contentgrid.configuration.api.ComposedConfiguration;
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
    void registerConfigurationFragments() throws Exception {
        var repo = new ComposedConfigurationRepository<String, String, TestConfiguration>(TestConfiguration::merge);

        repo.register(new ConfigurationFragment<>("test", "abc", new TestConfiguration("xyz", Set.of())));
        repo.register(new ConfigurationFragment<>("test2", "abc", new TestConfiguration(null, Set.of("ZZZ"))));
        repo.register(new ConfigurationFragment<>("test3", "abc", new TestConfiguration(null, Set.of("AAA", "BBB"))));

        assertThat(repo.findConfiguration("abc").getConfiguration()).hasValue(
                new TestConfiguration("xyz", Set.of("ZZZ", "AAA", "BBB")));

        var events = ObservableUtils.eventsToConcurrentLookup(repo, ComposedConfiguration::getCompositionKey);

        awaitUntilAsserted(() -> {
            assertThat(events.keys()).containsExactly("abc");
            assertThat(events.get("abc")).isEqualTo(repo.findConfiguration("abc"));
        });

        repo.register(new ConfigurationFragment<>("test4", "def", new TestConfiguration("def", Set.of("MMM"))));

        var abcFirst = repo.findConfiguration("abc");
        var defFirst = repo.findConfiguration("def");

        assertThat(abcFirst.getConfiguration()).hasValue(new TestConfiguration("xyz", Set.of("ZZZ", "AAA", "BBB")));
        assertThat(defFirst.getConfiguration()).hasValue(new TestConfiguration("def", Set.of("MMM")));

        awaitUntilAsserted(() -> {
            assertThat(events.keys()).containsExactlyInAnyOrder("abc", "def");
        });

        repo.revoke("test3");
        assertThat(repo.findConfiguration("abc").getConfiguration()).hasValue(
                new TestConfiguration("xyz", Set.of("ZZZ")));

        awaitUntilAsserted(() -> {
            assertThat(events.keys()).containsExactlyInAnyOrder("abc", "def");
            assertThat(events.get("abc"))
                    .isEqualTo(repo.findConfiguration("abc"))
                    .isNotEqualTo(abcFirst);
            assertThat(events.get("def")).isEqualTo(defFirst);
        });

        repo.revoke("test4");

        assertThat(repo.findConfiguration("def").getConfiguration()).isEmpty();

        awaitUntilAsserted(() -> {
            awaitUntilAsserted(() -> {
                assertThat(events.keys()).containsExactlyInAnyOrder("abc");
                assertThat(events.get("abc")).isEqualTo(repo.findConfiguration("abc"));
            });
        });

        repo.close();
        events.close();
    }

    @Test
    void revokeNonExistingFragment() throws Exception {
        var repo = new ComposedConfigurationRepository<String, String, TestConfiguration>(TestConfiguration::merge);

        var events = ObservableUtils.eventsToConcurrentLookup(repo, ComposedConfiguration::getCompositionKey);

        repo.register(new ConfigurationFragment<>("test", "abc", new TestConfiguration("xyz", Set.of())));
        repo.revoke("xyz");

        awaitUntilAsserted(() -> {
            assertThat(events.keys()).containsExactly("abc");
            assertThat(events.get("abc")).isEqualTo(repo.findConfiguration("abc"));
        });

        events.close();
        repo.close();
    }

    @Test
    void registerFragmentToNewAggregate() throws Exception {
        var repo = new ComposedConfigurationRepository<String, String, TestConfiguration>(TestConfiguration::merge);

        var events = ObservableUtils.eventsToConcurrentLookup(repo, ComposedConfiguration::getCompositionKey);

        repo.register(new ConfigurationFragment<>("test", "abc", new TestConfiguration("xyz", Set.of())));

        repo.register(new ConfigurationFragment<>("test", "def", new TestConfiguration("xyz123", Set.of())));

        assertThat(repo.findConfiguration("abc").getConfiguration()).isEmpty();
        assertThat(repo.findConfiguration("def").getConfiguration()).hasValue(
                new TestConfiguration("xyz123", Set.of()));

        awaitUntilAsserted(() -> {
            assertThat(events.get("abc")).isNull();
            assertThat(events.get("def")).isEqualTo(repo.findConfiguration("def"));
        });

        events.close();
        repo.close();
    }

    @Test
    void reRegisterFragment() throws Exception {
        var repo = new ComposedConfigurationRepository<String, String, TestConfiguration>(TestConfiguration::merge);

        var events = ObservableUtils.eventsToConcurrentLookup(repo, ComposedConfiguration::getCompositionKey);

        repo.register(new ConfigurationFragment<>("test", "abc", new TestConfiguration("xyz", Set.of("ZZZ"))));


        repo.register(new ConfigurationFragment<>("test", "abc", new TestConfiguration("xyz123", Set.of("ABC"))));

        assertThat(repo.findConfiguration("abc").getConfiguration())
                .hasValue(new TestConfiguration("xyz123", Set.of("ABC")));

        awaitUntilAsserted(() -> {
            assertThat(events.get("abc")).isEqualTo(repo.findConfiguration("abc"));
        });

        events.close();
        repo.close();
    }

    @Test
    void subscription() throws Exception {
        var publisher = new Publisher<ConfigurationFragment<String, String, TestConfiguration>>();
        var repo = new ComposedConfigurationRepository<>(TestConfiguration::merge, publisher);

        assertThat(repo.compositionKeys()).isEmpty();

        publisher.emit(UpdateType.ADD, new ConfigurationFragment<>("test", "abc", new TestConfiguration("xyz", Set.of("ZZZ"))));

        assertThat(repo.compositionKeys()).containsExactly("abc");
        assertThat(repo.findConfiguration("abc").getConfiguration()).hasValue(new TestConfiguration("xyz", Set.of("ZZZ")));

        publisher.emit(UpdateType.UPDATE, new ConfigurationFragment<>("test", "abc", new TestConfiguration("xyz", Set.of("UUU"))));
        assertThat(repo.findConfiguration("abc").getConfiguration()).hasValue(new TestConfiguration("xyz", Set.of("UUU")));

        publisher.emit(UpdateType.REMOVE, new ConfigurationFragment<>("test", "abc", new TestConfiguration("xyz", Set.of("UUU"))));

        assertThat(repo.compositionKeys()).isEmpty();

        publisher.close();
        repo.close();
    }

}