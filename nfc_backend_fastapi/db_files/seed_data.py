# import random
# from sqlalchemy.orm import Session
# from database import SessionLocal, engine
# from models import Base, User
# from datetime import date

# # Ensure tables exist
# Base.metadata.create_all(bind=engine)

# def create_fake_user(card_uid: str, session: Session):
#     # Check if card_uid already exists
#     user = session.query(User).filter(User.card_uid == card_uid).first()
#     if user:
#         print(f"User with card_uid {card_uid} already exists.")
#         return user
    
#     # Generate random dummy data for required fields
#     fake_first_name = "Test"
#     fake_last_name = f"User{random.randint(1000,9999)}"
#     fake_email = f"{fake_first_name.lower()}.{fake_last_name.lower()}@example.com"
#     fake_phone = f"+1000000{random.randint(1000,9999)}"
#     fake_dob = date(2000, 1, 1)
#     fake_password_hash = "hashedpassword"  # Placeholder; hash properly in real code
#     fake_balance_cents = random.randint(0, 100000)  # 0 to 1000.00 dollars in cents

#     new_user = User(
#         first_name=fake_first_name,
#         last_name=fake_last_name,
#         email=fake_email,
#         phone_number=fake_phone,
#         dob=fake_dob,
#         password_hash=fake_password_hash,
#         balance_cents=fake_balance_cents,
#         card_uid=card_uid,
#         is_cadet=True  # example flag, can customize per use case
#     )
#     session.add(new_user)
#     session.commit()
#     print(f"Inserted new user with card_uid {card_uid}, balance {fake_balance_cents/100:.2f} USD")
#     return new_user

# def main():
#     session = SessionLocal()
#     # Insert 2 fake users
#     for i in range(2):
#         fake_uid = f"FAKEUID{i+10:03d}"
#         create_fake_user(fake_uid, session)
#     session.close()

# if __name__ == "__main__":
#     main()






"""
seed.py
Populate Users, Businesses, and Products with dummy data.
Run:  python seed.py
"""

import os
import sys
import random
from datetime import date

from sqlalchemy.orm import Session

# Local imports
sys.path.append(os.path.abspath(os.path.dirname(__file__)))
from database import SessionLocal, engine
from models import Base, User, Business, Product

# ─────────────────────────────────────────────
# Helpers
# ─────────────────────────────────────────────
ADJECTIVES = ["Iced", "Hot", "Deluxe", "Classic", "Spicy", "Sweet"]
NOUNS      = ["Coffee", "Latte", "Bagel", "Sandwich", "Tea", "Muffin"]

def random_product_name() -> str:
    return f"{random.choice(ADJECTIVES)} {random.choice(NOUNS)}"

def random_price() -> float:
    return round(random.uniform(1.50, 15.00), 2)

# ─────────────────────────────────────────────
# Seed Functions
# ─────────────────────────────────────────────
def create_user_with_business_and_products(card_uid: str, session: Session):
    """Creates a User → Business → 3 Products if card_uid not already present."""
    
    # 1️⃣  Check existing user
    if session.query(User).filter_by(card_uid=card_uid).first():
        print(f"User with UID {card_uid} already seeded.")
        return
    
    # 2️⃣  User
    user = User(
        first_name="Test",
        last_name=f"User{random.randint(1000, 9999)}",
        email=f"test{random.randint(1000, 9999)}@example.com",
        phone_number=f"+1555{random.randint(100000, 999999)}",
        dob=date(2000, 1, 1),
        password_hash="hashedpassword",
        balance_cents=random.randint(0, 100_00),
        card_uid=card_uid,
        is_cadet=True
    )
    session.add(user)
    session.flush()  # ensures user.id is populated

    # 3️⃣  Business
    biz = Business(
        owner_id=user.id,
        business_name=f"{user.first_name}'s Shop"
    )
    session.add(biz)
    session.flush()  # ensures biz.id is populated

    # 4️⃣  Products (3 random)
    for _ in range(5):
        product = Product(
            business_id=biz.id,
            title=random_product_name(),
            price=random_price(),
            sku=f"SKU{random.randint(10000, 99999)}",
            barcode_number=f"{random.randint(100000000000, 999999999999)}",
            description="Autogenerated product for testing",
            keywords="test,seed"
        )
        session.add(product)

    session.commit()
    print(
        f"✔ Seeded user ({user.email}), business ({biz.business_name}) "
        f"and 5 products — UID {card_uid}"
    )

# ─────────────────────────────────────────────
# Entry‑point
# ─────────────────────────────────────────────
if __name__ == "__main__":
    # Ensure tables exist
    Base.metadata.create_all(bind=engine)

    with SessionLocal() as session:
        for i in range(2):                         # Seed 2 merchants
            create_user_with_business_and_products(
                card_uid=f"FAKEUID{i + 10:03d}",
                session=session
            )
