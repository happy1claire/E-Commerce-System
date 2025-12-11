"""
helpers.py - Shared Helper Functions
Shared helper functions for both Use Case tests
"""

import random

def generate_credit_card_number():
    """Generate a random credit card number in the format XXXX-XXXX-XXXX-XXXX."""
    groups = []
    for _ in range(4):
        group = ''.join(str(random.randint(0, 9)) for _ in range(4))
        groups.append(group)
    return "-".join(groups)

def random_product_id():
    """generate a random product Id"""
    return random.randint(1, 1000)

def random_customer_id():
    """generate a random customer Id"""
    return random.randint(1, 1000)

