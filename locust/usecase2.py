"""
Use Case 2: Customer Checks Out Shopping Cart

Test Scenario:
- Customer logs in and gets/creates a shopping cart
- Customer adds 1-5 items to the shopping cart
- Customer enters credit card information and completes checkout

Flow:
1. GET /shopping-cart/by-customer/{customerId}
2. POST /shopping-cart/{cartId}/items (1-5 times)
3. POST /shopping-cart/{cartId}/checkout

"""
from locust import HttpUser, task, between, SequentialTaskSet
import random
from helper import generate_credit_card_number, random_product_id, random_customer_id


class Checkout(SequentialTaskSet):

    def on_start(self):
        """Initialize customer session"""
        self.customer_id = random_customer_id()
        self.items_added_successfully = 0

    @task
    def complete_shopping_flow(self):
        """Complete shopping flow: get cart -> add items -> checkout"""

        # Step 1: Get or create shopping cart
        with self.client.get(
                f"/shopping-cart/by-customer/{self.customer_id}",
                name="/shopping-cart/by-customer/{customerId}",
                catch_response=True,
        ) as resp:
            if resp.status_code != 200:
                resp.failure(f"Failed to get/create cartId: {resp.status_code}")
                self.interrupt()
                return

            try:
                data = resp.json()
                cart_id = data.get("cartId")
                if not cart_id:
                    resp.failure("Missing cartId in response JSON")
                    self.interrupt()
                    return
            except Exception as e:
                resp.failure(f"Failed to parse cartId: {str(e)}")
                self.interrupt()
                return

        # Step 2: Add items
        num_items = random.randint(1, 5)

        for _ in range(num_items):
            item_id = random_product_id()
            quantity = random.randint(1, 5)

            with self.client.post(
                    f"/shopping-cart/{cart_id}/items",
                    params={"itemId": item_id, "quantity": quantity},
                    name="/shopping-cart/{cartId}/items",
                    catch_response=True,
            ) as resp:
                if resp.status_code == 201:
                    self.items_added_successfully += 1
                    resp.success()
                elif resp.status_code == 400:
                    resp.failure("Warehouse insufficient inventory")
                else:
                    resp.failure(f"Unexpected status: {resp.status_code}")

        # Step 3: Checkout
        if self.items_added_successfully > 0:
            credit_card_number = generate_credit_card_number()

            with self.client.post(
                    f"/shopping-cart/{cart_id}/checkout",
                    params={"creditCardNumber": credit_card_number},
                    name="/shopping-cart/{cartId}/checkout",
                    catch_response=True,
            ) as resp:
                if resp.status_code in (200, 202):
                    resp.success()
                elif resp.status_code == 402:
                    resp.failure("Credit card declined")
                else:
                    resp.failure(f"Checkout failed: {resp.status_code}")

        self.items_added_successfully = 0
        self.interrupt()


class UseCase2User(HttpUser):
    """Use Case 2: User who completes checkout"""
    tasks = [Checkout]
    wait_time = between(1, 3)
    host = "http://ecommerce-alb-1016239137.us-east-1.elb.amazonaws.com"