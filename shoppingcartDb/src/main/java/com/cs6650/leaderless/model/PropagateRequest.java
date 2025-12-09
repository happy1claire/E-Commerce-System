package com.cs6650.leaderless.model;

import ch.qos.logback.core.hook.ShutdownHook;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PropagateRequest {
    private String key;
    private Shoppingcart shoppingcart;
    private long version;
}
