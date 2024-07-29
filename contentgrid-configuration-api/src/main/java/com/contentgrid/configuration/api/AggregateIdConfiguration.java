package com.contentgrid.configuration.api;

public interface AggregateIdConfiguration<AGG, C> extends HasConfiguration<C> {
    AGG getAggregateId();

}
