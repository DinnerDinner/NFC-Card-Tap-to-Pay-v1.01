import random
from sqlalchemy.orm import Session
from database import SessionLocal, engine
from models import Base, User
from datetime import date

# Ensure tables exist
Base.metadata.create_all(bind=engine)

def create_fake_user(card_uid: str, session: Session):
    # Check if card_uid already exists
    user = session.query(User).filter(User.card_uid == card_uid).first()
    if user:
        print(f"User with card_uid {card_uid} already exists.")
        return user
    
    # Generate random dummy data for required fields
    fake_first_name = "Test"
    fake_last_name = f"User{random.randint(1000,9999)}"
    fake_email = f"{fake_first_name.lower()}.{fake_last_name.lower()}@example.com"
    fake_phone = f"+1000000{random.randint(1000,9999)}"
    fake_dob = date(2000, 1, 1)
    fake_password_hash = "hashedpassword"  # Placeholder; hash properly in real code
    fake_balance_cents = random.randint(0, 100000)  # 0 to 1000.00 dollars in cents

    new_user = User(
        first_name=fake_first_name,
        last_name=fake_last_name,
        email=fake_email,
        phone_number=fake_phone,
        dob=fake_dob,
        password_hash=fake_password_hash,
        balance_cents=fake_balance_cents,
        card_uid=card_uid,
        is_cadet=True  # example flag, can customize per use case
    )
    session.add(new_user)
    session.commit()
    print(f"Inserted new user with card_uid {card_uid}, balance {fake_balance_cents/100:.2f} USD")
    return new_user

def main():
    session = SessionLocal()
    # Insert 2 fake users
    for i in range(2):
        fake_uid = f"FAKEUID{i+10:03d}"
        create_fake_user(fake_uid, session)
    session.close()

if __name__ == "__main__":
    main()
