package com.contentgrid.configuration.applications;

import java.util.UUID;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@EqualsAndHashCode
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class DeploymentId {

    @NonNull
    @Getter
    private final String value;

    @Override
    public String toString() {
        return this.getValue();
    }

    public static DeploymentId from(@NonNull String value) {
        return new DeploymentId(value);
    }

    public static DeploymentId random() {
        return new DeploymentId(UUID.randomUUID().toString());
    }
}
