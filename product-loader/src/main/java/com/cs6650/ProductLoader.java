package com.cs6650;

import com.cs6650.ProductGenerator;
import com.cs6650.Model.Product;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ProductLoader {

    /**
     * The endpoint URL used to create a new random product on the Product Service.
     */
    private static final String PRODUCT_SERVICE_RANDOM_URL =
            "http://localhost:8080/product";

    /**
     * Total number of products to preload.
     */
    private static final int NUM_PRODUCTS = 1000;

    /**
     * Entry point of the preload client.
     * This method:
     * Creates an {@link HttpClient}
     * Executes {@code NUM_PRODUCTS} POST requests
     * Logs progress every 100 requests
     * Prints errors if a request fails
     * Outputs a completion message
     *
     * @param args unused command-line arguments
     * @throws Exception if the HTTP client encounters a fatal error
     */
    public static void main(String[] args) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        for (int i = 1; i <= NUM_PRODUCTS; i++) {
            Product product = ProductGenerator.generateProduct();
            String jsonBody = toJson(product);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(PRODUCT_SERVICE_RANDOM_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            try {
                HttpResponse<String> response =
                        client.send(request, HttpResponse.BodyHandlers.ofString());

                if(response.statusCode()!= 201 ){
                    System.out.printf("Failed at %d: status=%d, body=%s%n", i, response.statusCode(), response.body());
                }
                if(i % 100 == 0){
                    System.out.println(product.getId());
                    System.out.println("Processed " + i + " of " + NUM_PRODUCTS + " products");
                }

            }catch (Exception e) {
                System.out.printf("Exception at %d: %s%n", i, e.getMessage());
            }
        }

    }
    //helper
    private static String toJson(Product p) {
        return "{"
                + "\"id\":" + p.getId() + ","
                + "\"sku\":\"" + p.getSku() + "\","
                + "\"manufacturer\":\"" + p.getManufacturer() + "\","
                + "\"categoryId\":" + p.getCategoryId() + ","
                + "\"weight\":" + p.getWeight() + ","
                + "\"someOtherId\":" + p.getSomeOtherId()
                + "}";
    }
}
