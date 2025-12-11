"""
Use Case 1: Customer Adds Items to Shopping Cart
Test Scenario:
- Customer logs in and gets/creates a shopping cart
- Customer adds 2-5 items to the shopping cart
- Customer does NOT checkout (simulates cart abandonment)

    Flow:
    1. GET /shopping-cart/by-customer/{customerId}
    2. POST /shopping-cart/{cartId}/items (2-5 times)
    3. End session (no checkout)
"""
from locust import HttpUser, task, between, SequentialTaskSet
import random
from helper import random_product_id, random_customer_id


class AddToCart(SequentialTaskSet):
    """
    Use Case 1: Customer adds items to shopping cart


    """

    def on_start(self):
        """Initialize customer session and get/create shopping cart"""
        self.customer_id = random_customer_id()
        self.items_added = 0

        with self.client.get(
                f"/shopping-cart/by-customer/{self.customer_id}",
                name="/shopping-cart/by-customer/{customerId}",
                catch_response=True,
        ) as resp:
            if resp.status_code == 200:
                try:
                    data = resp.json()
                    self.cart_id = data.get("cartId")
                    if not self.cart_id:
                        resp.failure("Missing cartId in response JSON")
                except Exception as e:
                    self.cart_id = None
                    resp.failure(f"Failed to parse cartId: {str(e)}")
            else:
                self.cart_id = None
                resp.failure(f"Failed to get/create cartId: {resp.status_code}")

    @task
    def add_multiple_items(self):
        """Add 2-5 items to shopping cart"""
        if not getattr(self, "cart_id", None):
            self.interrupt()
            return

        num_items = random.randint(2, 5)

        for i in range(num_items):
            item_id = random_product_id()
            quantity = random.randint(1, 5)

            with self.client.post(
                    f"/shopping-cart/{self.cart_id}/items",
                    params={"itemId": item_id, "quantity": quantity},
                    name="/shopping-cart/{cartId}/items",
                    catch_response=True,
            ) as resp:
                if resp.status_code == 201:
                    self.items_added += 1
                    resp.success()
                elif resp.status_code == 400:
                    resp.failure("Warehouse insufficient inventory")
                else:
                    resp.failure(f"Unexpected status: {resp.status_code}")

        # End session (simulates cart abandonment)
        self.interrupt()


class UseCase1User(HttpUser):
    """Use Case 1: User who only adds items, does not checkout"""
    tasks = [AddToCart]
    wait_time = between(1, 3)
    host = "http://ecommerce-alb-1016239137.us-east-1.elb.amazonaws.com"