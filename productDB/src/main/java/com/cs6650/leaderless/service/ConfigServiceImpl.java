package com.cs6650.leaderless.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConfigServiceImpl implements ConfigService {

    private final PropagationService propagationService;

    @Autowired
    public ConfigServiceImpl(PropagationService propagationService) {
        this.propagationService = propagationService;
    }

    @Override
    public String getSelfAddress() {
        return propagationService.getSelfAddress();
    }

    @Override
    public void setSelfAddress(String url) {
        propagationService.setSelfAddress(url);
    }

    @Override
    public void setPeers(List<String> peers) {
        propagationService.setPeers(peers);
    }

    @Override
    public List<String> getPeers() {
        return propagationService.getPeers();
    }
}
