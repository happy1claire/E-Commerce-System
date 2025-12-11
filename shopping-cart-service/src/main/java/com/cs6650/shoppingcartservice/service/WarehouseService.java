package com.cs6650.shoppingcartservice.service;

import com.cs6650.shoppingcartservice.model.ShipRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * WarehouseService is responsible for calling external WarehouseService.
 * It sends HTTP requests to /warehouse/reserve and /warehouse/ship.
 *
 * This class reuses the shared RestTemplate bean defined in RestTemplateConfig.
 */
@Service
public class WarehouseService {

    private final RestTemplate restTemplate;
    private final String warehouseBaseUrl;

    /**
     * warehouse.service.url should be defined in application.properties, e.g.:
     *
     * warehouse.service.url=http://localhost:8082
     */
    public WarehouseService(RestTemplate restTemplate,
                           @Value("${warehouse.service.url}") String warehouseBaseUrl) {
        this.restTemplate = restTemplate;
        this.warehouseBaseUrl = warehouseBaseUrl;
    }


    /**
     * Calls WarehouseService /warehouse/ship.
     * Always expected to succeed in this assignment.
     */
    public boolean ship(long productId, int quantity) {
        String url = warehouseBaseUrl + "/warehouse/ship";

        ShipRequest request = new ShipRequest(productId, quantity);

        try {
            ResponseEntity<Map> response =
                    restTemplate.postForEntity(url, request, Map.class);

            return response.getStatusCode().is2xxSuccessful();

        } catch (Exception e) {
            return false;
        }
    }
}
