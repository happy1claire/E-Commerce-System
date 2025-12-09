package com.cs6650.leaderless.controller;

import com.cs6650.leaderless.model.NodeAddress;
import com.cs6650.leaderless.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/config")
public class ConfigController {

    private final ConfigService configService;

    @Autowired
    public ConfigController(ConfigService configService) {
        this.configService = configService;
    }

    @GetMapping("/self")
    public ResponseEntity<NodeAddress> getSelfAddress() {
        String selfAddress = configService.getSelfAddress();
        return ResponseEntity.ok(new NodeAddress(selfAddress));
    }

    @PostMapping("/self")
    public ResponseEntity<?> setSelfAddress(@RequestBody NodeAddress nodeAddress) {
        if (nodeAddress == null || nodeAddress.getUrl() == null || nodeAddress.getUrl().isEmpty()) {
            return ResponseEntity.badRequest().body("URL must not be empty");
        }
        configService.setSelfAddress(nodeAddress.getUrl());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/peers")
    public ResponseEntity<List<String>> getPeers() {
        List<String> peers = configService.getPeers();
        return ResponseEntity.ok(peers);
    }

    @PostMapping("/peers")
    public ResponseEntity<?> setPeers(@RequestBody List<String> peers) {
        if (peers == null) {
            return ResponseEntity.badRequest().body("Peer list must not be null");
        }
        configService.setPeers(peers);
        return ResponseEntity.ok().build();
    }
}
