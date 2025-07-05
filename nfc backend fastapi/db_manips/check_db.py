import sys
import os
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from db_files.database import SessionLocal
from db_files.models import Card

def print_all_cards():
    session = SessionLocal()
    try:
        cards = session.query(Card).all()
        if not cards:
            print("No cards found in database.")
        for card in cards:
            print(f"UID: {card.uid} | Balance: {card.balance} | ID: {card.id} | Created at: {card.created_at}")
    finally:
        session.close()

if __name__ == "__main__":
    print_all_cards()
