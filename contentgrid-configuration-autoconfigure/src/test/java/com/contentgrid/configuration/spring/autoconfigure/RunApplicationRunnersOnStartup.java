package com.contentgrid.configuration.spring.autoconfigure;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import lombok.SneakyThrows;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

public class RunApplicationRunnersOnStartup implements Function<ApplicationContextRunner, ApplicationContextRunner> {

    @Override
    public ApplicationContextRunner apply(ApplicationContextRunner applicationContextRunner) {
        return applicationContextRunner.withInitializer(applicationContext -> {
            applicationContext.addApplicationListener(new ApplicationListener<ContextRefreshedEvent>() {
                private final AtomicBoolean alreadyCalled = new AtomicBoolean();
                @Override
                @SneakyThrows
                public void onApplicationEvent(ContextRefreshedEvent event) {
                    if(alreadyCalled.compareAndSet(false, true)) {
                        for (ApplicationRunner applicationRunner : event.getApplicationContext()
                                .getBeanProvider(ApplicationRunner.class)) {
                            applicationRunner.run(new DefaultApplicationArguments());
                        }
                    }
                }
            });
        });
    }
}
