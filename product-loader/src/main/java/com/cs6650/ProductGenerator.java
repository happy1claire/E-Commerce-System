package com.cs6650;


import com.cs6650.Model.Product;

import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class ProductGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final AtomicInteger PRODUCT_ID_COUNTER = new AtomicInteger(1);
    private static final String SKU_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";


    public static Product generateProduct() {
        int productId = PRODUCT_ID_COUNTER.getAndIncrement();
        String sku = generateSku();
        String manufacturer = "MFG-" + RANDOM.nextInt(1000);
        int categoryId = RANDOM.nextInt(100) + 1;
        int weight = RANDOM.nextInt(5000);
        int someOtherId = RANDOM.nextInt(10000) + 1;

        return new Product(
                productId,
                sku,
                manufacturer,
                categoryId,
                weight,
                someOtherId
        );
    }

    private static String generateSku() {
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            int index = RANDOM.nextInt(SKU_CHARS.length());
            sb.append(SKU_CHARS.charAt(index));
        }
        return sb.toString();
    }

}
