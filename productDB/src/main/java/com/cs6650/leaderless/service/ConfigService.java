package com.cs6650.leaderless.service;

import java.util.List;

public interface ConfigService {
    String getSelfAddress();

    void setSelfAddress(String url);

    void setPeers(List<String> peers);

    List<String> getPeers();
}
