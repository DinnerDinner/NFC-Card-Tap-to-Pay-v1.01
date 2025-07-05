# import random
# from sqlalchemy.orm import Session
# from database import SessionLocal, engine
# from models import Base, Card

# # Make sure tables exist
# Base.metadata.create_all(bind=engine)

# def create_fake_card(uid: str, session: Session):
#     # Check if UID already exists
#     card = session.query(Card).filter(Card.uid == uid).first()
#     if card:
#         print(f"Card with UID {uid} already exists.")
#         return card
    
#     # Generate random balance for dummy data
#     balance = round(random.uniform(0, 1000), 2)
#     new_card = Card(uid=uid, balance=balance)
#     session.add(new_card)
#     session.commit()
#     print(f"Inserted new card UID {uid} with balance {balance}")
#     return new_card

# def main():
#     session = SessionLocal()
#     # Example: Insert 2 fake cards
#     for i in range(2):
#         fake_uid = f"FAKEUID{i+10:03d}"
#         create_fake_card(fake_uid, session)
#     session.close()

# if __name__ == "__main__":
#     main()
