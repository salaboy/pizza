package io.diagrid.dapr.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

public record Orders(@JsonProperty List<OrderPayload> orders) {}
