Change the `PRODUCT_SERVICE_RANDOM_URL` variable in `ProductLoader.java` to the product database alb.

Then compile and run the application using:

```bash
mvn clean compile
java -cp target/classes com.cs6650.ProductLoader
```

Notice that we are using Java version 17, so make sure the environment is set up accordingly.
