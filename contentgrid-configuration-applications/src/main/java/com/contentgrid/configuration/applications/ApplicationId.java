package com.contentgrid.configuration.applications;

import java.util.UUID;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@EqualsAndHashCode
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ApplicationId {

    @NonNull
    @Getter
    private final String value;

    @Override
    public String toString() {
        return this.getValue();
    }

    public static ApplicationId from(@NonNull String value) {
        return new ApplicationId(value);
    }

    public static ApplicationId random() {
        return new ApplicationId(UUID.randomUUID().toString());
    }
}
