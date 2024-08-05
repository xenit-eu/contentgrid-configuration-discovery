package com.contentgrid.configuration.spring.autoconfigure.kubernetes;

import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class KubernetesResourceFilter {

    private final String namespace;
    private final LabelSelector labelSelector;

    public <T, L, R extends Resource<T>> FilterWatchListDeletable<T, L, R> filter(MixedOperation<T, L, R> operation) {
        var nextOp = namespace != null ? operation.inNamespace(namespace) : operation;
        return labelSelector != null ? nextOp.withLabelSelector(labelSelector) : nextOp;
    }
}
