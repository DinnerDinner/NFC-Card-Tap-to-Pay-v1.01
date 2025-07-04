# check_db.py

from database import SessionLocal
from models import Card

def print_all_cards():
    session = SessionLocal()
    try:
        cards = session.query(Card).all()
        if not cards:
            print("No cards found in database.")
        for card in cards:
            print(f"UID: {card.uid} | Balance: {card.balance} | Metadata: {card.metadata}")
    finally:
        session.close()

if __name__ == "__main__":
    print_all_cards()
